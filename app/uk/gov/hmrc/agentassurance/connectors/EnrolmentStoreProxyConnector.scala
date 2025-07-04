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

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Try

import com.google.inject.ImplementedBy
import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json.format
import play.api.libs.json.Json.fromJson
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

case class ClientAllocation(
  friendlyName: String,
  state: String
)

object ClientAllocation {
  implicit val formats: Format[ClientAllocation] = format[ClientAllocation]
}

case class ClientAllocationResponse(clients: Seq[ClientAllocation])

@ImplementedBy(classOf[EnrolmentStoreProxyConnectorImpl])
trait EnrolmentStoreProxyConnector {
  def getClientCount(
    service: String,
    userId: String
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Int]
}

@Singleton
class EnrolmentStoreProxyConnectorImpl @Inject() (
  httpGet: HttpClientV2,
  val metrics: Metrics
)(
  implicit appConfig: AppConfig
)
extends EnrolmentStoreProxyConnector
with HistogramMonitor {

  private val emacBaseUrl: String = s"${appConfig.esProxyUrl}/enrolment-store-proxy/enrolment-store"

  implicit val responseHandler: HttpReads[ClientAllocationResponse] =
    new HttpReads[ClientAllocationResponse] {
      override def read(
        method: String,
        url: String,
        response: HttpResponse
      ) = {
        Try(response.status match {
          case 200 => ClientAllocationResponse(parseClients((response.json \ "enrolments").get))
          case 204 => ClientAllocationResponse(Seq.empty)
        }).getOrElse(
          throw new RuntimeException(
            s"Error retrieving client list from $url: status ${response.status} body ${response.body}"
          )
        )
      }
    }

  def getClientCount(
    service: String,
    userId: String
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Int] = {
    val clientListUrl = s"$emacBaseUrl/users/$userId/enrolments?type=delegated&service=$service"

    reportHistogramValue(s"Size-ESP-ES2-GetAgentClientList-$service") {
      val timer = metrics.defaultRegistry.timer(s"ConsumedAPI-ESP-ES2-GetAgentClientList-$service-GET")
      timer.time()
      httpGet
        .get(url"$clientListUrl")
        .execute[ClientAllocationResponse]
        .map(result => {
          timer.time().stop
          result.clients.count {
            _.state.toLowerCase match {
              case "activated" => true
              case "unknown" => true
              case _ => false
            }
          }
        })
    }
  }

  private def parseClients(jsonResponse: JsValue): Seq[ClientAllocation] = {
    fromJson[Seq[ClientAllocation]](jsonResponse).getOrElse {
      throw new RuntimeException(s"Invalid payload received from enrolment store proxy: $jsonResponse")
    }
  }

}
