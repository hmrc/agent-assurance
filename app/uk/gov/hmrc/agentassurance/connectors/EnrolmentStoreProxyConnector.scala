/*
 * Copyright 2022 HM Revenue & Customs
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
import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.libs.json.Json.{format, fromJson}
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class ClientAllocation(friendlyName: String, state: String)

object ClientAllocation {
  implicit val formats = format[ClientAllocation]
}

case class ClientAllocationResponse(clients: Seq[ClientAllocation])

@ImplementedBy(classOf[EnrolmentStoreProxyConnectorImpl])
trait EnrolmentStoreProxyConnector {
  def getClientCount(service: String, userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int]
}

@Singleton
class EnrolmentStoreProxyConnectorImpl @Inject()(httpGet: HttpClient, metrics: Metrics)(implicit appConfig: AppConfig) extends
  EnrolmentStoreProxyConnector with HttpAPIMonitor with HistogramMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  private val emacBaseUrl = s"${appConfig.esProxyUrl}/enrolment-store-proxy/enrolment-store"

  implicit val responseHandler = new HttpReads[ClientAllocationResponse] {
    override def read(method: String, url: String, response: HttpResponse) = {
      Try(response.status match {
        case 200 => ClientAllocationResponse(parseClients((response.json \ "enrolments").get))
        case 204 => ClientAllocationResponse(Seq.empty)
      }).getOrElse(throw new RuntimeException(s"Error retrieving client list from $url: status ${response.status} body ${response.body}"))
    }
  }

  def getClientCount(service: String, userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] = {
    val clientListUrl = s"$emacBaseUrl/users/$userId/enrolments?type=delegated&service=$service"

    reportHistogramValue(s"Size-ESP-ES2-GetAgentClientList-$service") {
      monitor(s"ConsumedAPI-ESP-ES2-GetAgentClientList-$service-GET") {
        httpGet.GET[ClientAllocationResponse](clientListUrl).map{
          _.clients.count{
            _.state.toLowerCase match {
              case "activated" => true
              case "unknown" => true
              case _ => false
            }
          }
        }
      }
    }
  }

  private def parseClients(jsonResponse: JsValue): Seq[ClientAllocation] = {
    fromJson[Seq[ClientAllocation]](jsonResponse).getOrElse {
      throw new RuntimeException(s"Invalid payload received from enrolment store proxy: $jsonResponse")
    }
  }

}
