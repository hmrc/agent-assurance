/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentassurance.connectors

import java.net.URL
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.ImplementedBy
import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import play.api.http.Status.NOT_FOUND
import play.api.http.Status.OK
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.mvc.Request
import play.api.Logging
import play.utils.UriEncoding
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentassurance.models.AmlsSubscriptionRecord
import uk.gov.hmrc.agentassurance.services.CacheProvider
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

case class ClientRelationship(agents: Seq[Agent])

case class Agent(agentId: Option[SaAgentReference], hasAgent: Boolean, agentCeasedDate: Option[String])

object ClientRelationship {
  implicit val agentReads: Reads[Agent] = Json.reads[Agent]

  implicit val readClientRelationship: Reads[ClientRelationship] =
    (JsPath \ "agents")
      .readNullable[Seq[Agent]]
      .map(optionalAgents => ClientRelationship(optionalAgents.getOrElse(Seq.empty)))
}

case class RegistrationRelationshipResponse(processingDate: String)

object RegistrationRelationshipResponse {
  implicit val reads: Reads[RegistrationRelationshipResponse] = Json.reads[RegistrationRelationshipResponse]
}

@ImplementedBy(classOf[DesConnectorImpl])
trait DesConnector {
  def getActiveCesaAgentRelationships(
      clientIdentifier: TaxIdentifier
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Seq[SaAgentReference]]]
  def getAmlsSubscriptionStatus(
      amlsRegistrationNumber: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AmlsSubscriptionRecord]

  def getAgentRecord(
      arn: Arn
  )(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[AgentDetailsDesResponse]
}

@Singleton
class DesConnectorImpl @Inject() (
    httpGet: HttpClientV2,
    metrics: Metrics,
    agentCacheProvider: CacheProvider,
    override val configuration: Config,
    override val actorSystem: ActorSystem
)(implicit appConfig: AppConfig)
    extends DesConnector
    with BaseConnector
    with Logging {

  private val baseUrl            = appConfig.desBaseUrl
  private val authorizationToken = appConfig.desAuthToken
  private val environment        = appConfig.desEnv

  private val Environment   = "Environment"
  private val CorrelationId = "CorrelationId"

  def getActiveCesaAgentRelationships(
      clientIdentifier: TaxIdentifier
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Seq[SaAgentReference]]] = {
    val encodedClientId = UriEncoding.encodePathSegment(clientIdentifier.value, "UTF-8")
    val encodedClientType: String = {
      val clientType = clientIdentifier match {
        case nino @ Nino(_) => nino.name
        case _ @Utr(_)      => "utr"
        case e              => throw new RuntimeException(s"Unacceptable taxIdentifier: $e")
      }
      UriEncoding.encodePathSegment(clientType, "UTF-8")
    }

    val url = new URL(s"$baseUrl/registration/relationship/$encodedClientType/$encodedClientId")

    getWithDesHeaders[HttpResponse]("GetStatusAgentRelationship", url).map(response =>
      response.status match {
        case OK =>
          response.json
            .asOpt[ClientRelationship]
            .map(
              _.agents
                .filter(agent => agent.hasAgent && agent.agentCeasedDate.isEmpty)
                .flatMap(_.agentId)
            )
        case NOT_FOUND =>
          logger.warn(s" NOT_FOUND GET legacy relationship response: 404 ")
          None
        case _ =>
          throw UpstreamErrorResponse(s" error GET legacy relationship response: ${response.status}", response.status)

      }
    )
  }

  // API #1028 Get Subscription Status
  def getAmlsSubscriptionStatus(
      amlsRegistrationNumber: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AmlsSubscriptionRecord] = {
    val encodedRegNumber = UriEncoding.encodePathSegment(amlsRegistrationNumber, UTF_8.name)
    val url              = new URL(s"$baseUrl/anti-money-laundering/subscription/$encodedRegNumber/status")
    getWithDesHeaders[AmlsSubscriptionRecord]("GetAmlsSubscriptionStatus", url)
  }

  // API #1170 (API#4) Get Agent Record
  override def getAgentRecord(
      arn: Arn
  )(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[AgentDetailsDesResponse] = {
    val url = new URL(s"$baseUrl/registration/personal-details/arn/${arn.value}")
    agentCacheProvider.agentDetailsCache(arn.value) {
      getWithDesHeadersWithRetry[AgentDetailsDesResponse]("GetAgentRecordCached", url)
    }
  }

  private def getWithDesHeaders[A: HttpReads](
      apiName: String,
      url: URL
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {

    val isInternalHost = appConfig.internalHostPatterns.exists(_.pattern.matcher(url.getHost).matches())

    val timer = metrics.defaultRegistry.timer(s"ConsumedAPI-DES-$apiName-GET")
    timer.time()
    httpGet
      .get(url)
      .setHeader(desHeaders(authorizationToken, environment, isInternalHost): _*)
      .execute[A]
      .map(result => {
        timer.time().stop()
        result
      })
  }

  private def getWithDesHeadersWithRetry[A: HttpReads](
      apiName: String,
      url: URL
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, x: Reads[A]): Future[A] = {

    val isInternalHost = appConfig.internalHostPatterns.exists(_.pattern.matcher(url.getHost).matches())

    retryFor[A](s"$apiName connector get $url")(retryCondition) {
      val timer = metrics.defaultRegistry.timer(s"ConsumedAPI-DES-$apiName-GET")
      timer.time()
      httpGet
        .get(url)
        .setHeader(desHeaders(authorizationToken, environment, isInternalHost): _*)
        .executeAndDeserialise[A]
        .map(result => {
          timer.time().stop()
          result
        })
    }
  }

  /*
   * If the service being called is external (e.g. DES/IF in QA or Prod):
   * headers from HeaderCarrier are removed (except user-agent header).
   * Therefore, required headers must be explicitly set.
   * See https://github.com/hmrc/http-verbs?tab=readme-ov-file#propagation-of-headers
   * */

  def desHeaders(authToken: String, env: String, isInternalHost: Boolean)(
      implicit hc: HeaderCarrier
  ): Seq[(String, String)] = {

    val additionalHeaders =
      if (isInternalHost) Seq.empty
      else
        Seq(
          HeaderNames.authorisation -> s"Bearer $authToken",
          HeaderNames.xRequestId    -> hc.requestId.map(_.value).getOrElse(UUID.randomUUID().toString)
        ) ++ hc.sessionId.fold(Seq.empty[(String, String)])(x => Seq(HeaderNames.xSessionId -> x.value))
    val commonHeaders = Seq(Environment -> env, CorrelationId -> UUID.randomUUID().toString)
    commonHeaders ++ additionalHeaders
  }

}
