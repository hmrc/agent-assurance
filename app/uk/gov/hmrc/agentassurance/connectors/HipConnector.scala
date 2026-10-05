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
import play.api.http.Status.CREATED
import play.api.http.Status.UNPROCESSABLE_ENTITY
import play.api.libs.json.*
import play.api.libs.ws.writeableOf_JsValue
import play.utils.UriEncoding
import uk.gov.hmrc.agentassurance.support.NoRequest
import uk.gov.hmrc.agentassurance.utils.RequestAwareLogging
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.connectors.helpers.CommonHeaders
import uk.gov.hmrc.agentassurance.models.*
import uk.gov.hmrc.agentassurance.models.RegistrationRequest.*
import uk.gov.hmrc.agentassurance.services.CacheProvider
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URI
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Try

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
  private val originatingSystem = "MDTP"
  private val transmittingSystem = "HIP"

  // API#1163 Registration
  def getBusinessName(utr: String)(using hc: HeaderCarrier): Future[Option[String]] =
    val url = new URI(s"$baseUrl/etmp/RESTAdapter/registration/UTR/${UriEncoding.encodePathSegment(utr, "UTF-8")}").toURL
    agentCacheProvider.agentNameCache(utr):
      httpV2
        .post(url)
        .withBody(Json.toJson(RegistrationRequest(isAnAgent = false)))
        .setHeader(hipHeaders*)
        .execute[HttpResponse]
        .map { response =>
          response.status match
            case CREATED => (response.json \ "success").asOpt[AgentNameResponse].flatMap(_.agentName)
            case UNPROCESSABLE_ENTITY if isNoMatchFound(response.body) =>
              logger.warn("[HipConnector] getBusinessName returned a 422 No Match Found")(using NoRequest)
              None
            case status =>
              logger.warn(s"[HipConnector] getBusinessName returned a $status")(using NoRequest)
              Some("Error retrieving name")
          end match
        }
  end getBusinessName

  private def isNoMatchFound(body: String): Boolean =
    Try(Json.parse(body))
      .toOption
      .flatMap(json => (json \ "errors" \ "code").asOpt[String])
      .contains("002")

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
