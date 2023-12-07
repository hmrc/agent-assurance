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

import com.codahale.metrics.MetricRegistry
import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import play.api.Logging
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.utils.UriEncoding
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.models.AmlsSubscriptionRecord
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.domain.{Nino, SaAgentReference, TaxIdentifier}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpClient, HttpReads, HttpResponse, UpstreamErrorResponse}

import java.net.URL
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class ClientRelationship(agents: Seq[Agent])

case class Agent(agentId: Option[SaAgentReference], hasAgent: Boolean, agentCeasedDate: Option[String])

object ClientRelationship {
  implicit val agentReads: Reads[Agent] = Json.reads[Agent]

  implicit val readClientRelationship: Reads[ClientRelationship] =
    (JsPath \ "agents").readNullable[Seq[Agent]]
      .map(optionalAgents => ClientRelationship(optionalAgents.getOrElse(Seq.empty)))
}

case class RegistrationRelationshipResponse(processingDate: String)

object RegistrationRelationshipResponse {
  implicit val reads: Reads[RegistrationRelationshipResponse] = Json.reads[RegistrationRelationshipResponse]
}

@ImplementedBy(classOf[DesConnectorImpl])
trait DesConnector {
  def getActiveCesaAgentRelationships(clientIdentifier: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Seq[SaAgentReference]]]
  def getAmlsSubscriptionStatus(amlsRegistrationNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AmlsSubscriptionRecord]
}

@Singleton
class DesConnectorImpl @Inject()(httpGet: HttpClient, metrics: Metrics)(implicit appConfig: AppConfig)
  extends DesConnector with HttpAPIMonitor with Logging {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  private val baseUrl = appConfig.desBaseUrl
  private val authorizationToken = appConfig.desAuthToken
  private val environment = appConfig.desEnv

  private val Environment = "Environment"
  private val CorrelationId = "CorrelationId"
  private val Authorization_ = "Authorization"

  private def explicitHeaders =
    Seq(
      Environment   -> s"$environment",
      CorrelationId -> UUID.randomUUID().toString,
      Authorization_ -> s"Bearer $authorizationToken"
    )



  def getActiveCesaAgentRelationships(clientIdentifier: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Seq[SaAgentReference]]] = {
    val encodedClientId = UriEncoding.encodePathSegment(clientIdentifier.value, "UTF-8")
    val encodedClientType: String = {
      val clientType = clientIdentifier match {
        case nino @ Nino(_) => nino.name
        case _ @ Utr(_) => "utr"
        case e => throw new RuntimeException(s"Unacceptable taxIdentifier: $e")
      }
      UriEncoding.encodePathSegment(clientType, "UTF-8")
    }

    val url = new URL(s"$baseUrl/registration/relationship/$encodedClientType/$encodedClientId")

    getWithDesHeaders[HttpResponse]("GetStatusAgentRelationship", url).map(response => response.status match{
      case OK => response.json.asOpt[ClientRelationship].map(
          _.agents
          .filter(agent => agent.hasAgent && agent.
            agentCeasedDate.isEmpty)
          .flatMap(_.agentId)
      )
      case NOT_FOUND => logger.warn(s" NOT_FOUND GET legacy relationship response: 404 ")
                None
      case _ =>
      throw UpstreamErrorResponse(s" error GET legacy relationship response: ${response.status}", response.status)

  })
  }

  //API #1028 Get Subscription Status
  def getAmlsSubscriptionStatus(amlsRegistrationNumber: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AmlsSubscriptionRecord] = {
    val url = new URL(s"$baseUrl/anti-money-laundering/subscription/$amlsRegistrationNumber/status")
    getWithDesHeaders[AmlsSubscriptionRecord]("GetAmlsSubscriptionStatus", url)
  }

  private def getWithDesHeaders[A: HttpReads](apiName: String, url: URL)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
    val desHeaderCarrier = hc.copy(
      authorization = Some(Authorization(s"Bearer $authorizationToken")),
      extraHeaders = hc.extraHeaders :+ "Environment" -> environment)
    monitor(s"ConsumedAPI-DES-$apiName-GET") {
      httpGet.GET[A](url.toString, headers = explicitHeaders)(implicitly[HttpReads[A]], desHeaderCarrier, ec)
    }
  }
}
