/*
 * Copyright 2019 HM Revenue & Customs
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.codahale.metrics.MetricRegistry
import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Named, Singleton}
import play.api.libs.json._
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentassurance.models.DateOfBirth
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@ImplementedBy(classOf[CitizenDetailsConnectorImpl])
trait CitizenDetailsConnector{
  def getDateOfBirth(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[DateOfBirth]]
}

@Singleton
class CitizenDetailsConnectorImpl @Inject()(@Named("citizen-details-baseUrl") baseUrl: URL,
                                        httpGet: HttpGet,
                                        metrics: Metrics) extends CitizenDetailsConnector with HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def getDateOfBirth(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[DateOfBirth]] = {
    monitor(s"ConsumedAPI") {
      val url = new URL(baseUrl, s"/citizen-details/nino/${nino.value}")
      httpGet.GET[JsValue](url.toString).map {
        json =>
          val dateString: JsResult[String] = (json \ "dateOfBirth").validate[String]
           dateString match {
            case JsSuccess(date, _) => Some(DateOfBirth(LocalDate.parse(date, DateTimeFormatter.ofPattern("ddMMyyyy"))))
            case e: JsError => None
        }
      }
    }.recover{
      case _: Exception => None
    }
  }
}
