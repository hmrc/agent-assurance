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

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.i18n.Lang.logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.models.AgencyDetails
//import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentClientAuthConnector @Inject()(http: HttpClient, metrics: Metrics)(implicit appConfig: AppConfig) extends HttpAPIMonitor {
  val baseUrl: String = appConfig.acaBaseUrl
  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def getAgencyDetails()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AgencyDetails]] =
    monitor("ConsumerAPI-Get-AgencyDetails-GET") {
      http
        .GET[HttpResponse](s"${appConfig.acaBaseUrl}/agent-client-authorisation/agent/agency-details")
        .map(response =>
          response.status match {
            case OK => Json.parse(response.body).validate[AgencyDetails] match {
              case JsSuccess(value,_) => Some(value)
              case JsError(e) => throw new InternalServerException(e.mkString)
            }
            case NO_CONTENT => None
            case s => logger.error(s"unexpected response $s when getting agency details")
              None
          }
        )
    }

}
