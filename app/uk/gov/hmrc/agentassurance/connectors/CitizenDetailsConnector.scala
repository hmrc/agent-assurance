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

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.ImplementedBy
import play.api.http.Status
import play.api.libs.json.JsPath
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.Logging
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.models.entitycheck.DeceasedCheckException
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

case class CitizenDeceased(deceased: Boolean)

object CitizenDeceased {
  implicit val reads: Reads[CitizenDeceased] =
    (JsPath \ "deceased")
      .readNullable[Boolean]
      .map(x => CitizenDeceased(x.getOrElse(false)))
}

@ImplementedBy(classOf[CitizenDetailsConnectorImpl])
trait CitizenDetailsConnector {

  def getCitizenDeceasedFlag(
      saUtr: SaUtr
  )(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[DeceasedCheckException]]

}

@Singleton
class CitizenDetailsConnectorImpl @Inject() (appConfig: AppConfig, http: HttpClientV2, metrics: Metrics)
    extends CitizenDetailsConnector
    with Logging {

  private val baseUrl = appConfig.citizenDetailsBaseUrl

  def getCitizenDeceasedFlag(
      saUtr: SaUtr
  )(implicit c: HeaderCarrier, ec: ExecutionContext): Future[Option[DeceasedCheckException]] = {
    val timer = metrics.defaultRegistry.timer(s"ConsumedAPI-CitizenDetails-GET")
    timer.time()
    http
      .get(url"$baseUrl/citizen-details/sautr/${saUtr.value}")
      .execute[HttpResponse]
      .map { response =>
        timer.time().stop()
        response.status match {
          case Status.OK =>
            Json.parse(response.body).as[CitizenDeceased] match {
              case x: CitizenDeceased if !x.deceased => None
              case _                                 => Some(DeceasedCheckException.EntityDeceasedCheckFailed)
            }
          case e => Some(DeceasedCheckException.CitizenConnectorRequestFailed(e))
        }
      }

  }

}
