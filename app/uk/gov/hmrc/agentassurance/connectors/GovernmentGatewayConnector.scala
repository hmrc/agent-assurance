/*
 * Copyright 2017 HM Revenue & Customs
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
import javax.inject.{Inject, Named, Singleton}

import com.kenshoo.play.metrics.Metrics
import play.api.libs.json.JsValue
import play.api.libs.json.Json.{format, fromJson}
import uk.gov.hmrc.domain.{AgentCode, EmpRef}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpReads, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}


case class ClientAllocation(friendlyName: String)

object ClientAllocation {
  implicit val formats = format[ClientAllocation]
}

case class ClientAllocationResponse(clients: Seq[ClientAllocation])

@Singleton
class  GovernmentGatewayConnector @Inject()(@Named("government-gateway-baseUrl") baseUrl: URL, httpGet: HttpGet, metrics: Metrics) {
  def getClientCount(service: String, agentCode: AgentCode)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] = {
    val clientListUrl = s"$baseUrl/agent/$agentCode/client-list/$service/all"

    implicit val responseHandler = new HttpReads[ClientAllocationResponse] {
      override def read(method: String, url: String, response: HttpResponse) = {
        response.status match {
          case 200 => ClientAllocationResponse(parseClients(response.json))
          case 204 =>  ClientAllocationResponse(Seq.empty)
          case 202 => ClientAllocationResponse(Seq.empty)
          case _ => throw new RuntimeException(s"Error retrieving client list from $url: status ${response.status} body ${response.body}")
        }
      }
    }

    httpGet.GET[ClientAllocationResponse](clientListUrl).map(
      response =>
        response.clients.size)
  }

  private def parseClients(jsonResponse: JsValue): Seq[ClientAllocation]  = {
    fromJson[Seq[ClientAllocation]](jsonResponse).getOrElse {
      throw new RuntimeException(s"Invalid payload received from government gateway: $jsonResponse")
    }
  }
}

