/*
 * Copyright 2024 HM Revenue & Customs
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

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import play.api.libs.json.*
import play.api.libs.ws.writeableOf_JsValue
import play.utils.UriEncoding
import uk.gov.hmrc.agentassurance.support.NoRequest
import uk.gov.hmrc.agentassurance.utils.RequestAwareLogging
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.connectors.helpers.CommonHeaders
import uk.gov.hmrc.agentassurance.models.*
import uk.gov.hmrc.agentassurance.models.DesRegistrationRequest.*
import uk.gov.hmrc.agentassurance.services.CacheProvider
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URI
import java.net.URL
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class HipConnector @Inject() (
                               appConfig: AppConfig,
                               httpV2: HttpClientV2,
                               agentCacheProvider: CacheProvider,
                               override val configuration: Config,
                               override val actorSystem: ActorSystem
                             )(using ec: ExecutionContext)
  extends BaseConnector
    with RequestAwareLogging {

  private val baseUrl = appConfig.hipBaseUrl
  private val authToken = appConfig.hipAuthToken
  private val originatingSystem = "MDTP-ASA"
  private val transmittingSystem = "HIP"

  // API#1163 Registration
  def getBusinessName(utr: String)(using hc: HeaderCarrier): Future[Option[String]] =
    val url = new URI(s"$baseUrl/RESTAdapter/registration/utr/${UriEncoding.encodePathSegment(utr, "UTF-8")}").toURL
    agentCacheProvider.agentNameCache(utr):
      postWithHipHeaders[DesRegistrationRequest, DesAgentNameResponse](
        url = url,
        request = DesRegistrationRequest(isAnAgent = false)
      )
        .map(_.flatMap(_.agentName))
        .recoverWith:
          case e: UpstreamErrorResponse if e.statusCode == 422 =>
            logger.warn("[HipConnector] getBusinessName returned a 422")(using NoRequest)
            Future.successful(Some("Error retrieving name"))
  end getBusinessName

  private def postWithHipHeaders[
    B,
    A: HttpReads
  ](
     url: URL,
     request: B
   )(using
     hc: HeaderCarrier,
     ec: ExecutionContext,
     y: Writes[B]
   ): Future[Option[A]] =

    val response = httpV2
      .post(url)
      .withBody(Json.toJson(request))
      .setHeader(hipHeaders*)
      .execute[Option[A]]
    response
  end postWithHipHeaders

  /*
   * If the service being called is external (e.g. DES/IF in QA or Prod):
   * headers from HeaderCarrier are removed (except user-agent header).
   * Therefore, required headers must be explicitly set.
   * See https://github.com/hmrc/http-verbs?tab=readme-ov-file#propagation-of-headers
   * */

  private def hipHeaders(using hc: HeaderCarrier): Seq[(String, String)] = {
    CommonHeaders() ++ Seq(
      "Authorization" -> s"Basic $authToken",
      "correlationid" -> UUID.randomUUID().toString,
      "X-Originating-System" -> originatingSystem,
      "X-Receipt-Date" -> java.time.Instant.now().truncatedTo(SECONDS).toString,
      "X-Transmitting-System" -> transmittingSystem
    )
  }
}
