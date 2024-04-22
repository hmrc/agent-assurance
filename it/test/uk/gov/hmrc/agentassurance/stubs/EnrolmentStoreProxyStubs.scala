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

package test.uk.gov.hmrc.agentassurance.stubs

import com.github.tomakehurst.wiremock.client.WireMock._

trait EnrolmentStoreProxyStubs {
  private def clientListUrl(service: String, userId: String) = {
    s"/enrolment-store-proxy/enrolment-store/users/$userId/enrolments?type=delegated&service=$service"
  }

  def sufficientClientsAreAllocated(service: String, userId: String, state: String = "Unknown") = {
    val responseBody =
      s"""
         |{
         |    "enrolments": [
         |        {
         |            "friendlyName": "",
         |            "identifiers": [
         |                {
         |                    "key": "TaxOfficeNumber",
         |                    "value": "754"
         |                },
         |                {
         |                    "key": "TaxOfficeReference",
         |                    "value": "DF10175"
         |                }
         |            ],
         |            "service": "$service",
         |            "state" : "$state"
         |        },
         |        {
         |            "friendlyName": "",
         |            "identifiers": [
         |                {
         |                    "key": "TaxOfficeNumber",
         |                    "value": "786"
         |                },
         |                {
         |                    "key": "TaxOfficeReference",
         |                    "value": "RZ00013"
         |                }
         |            ],
         |            "service": "$service",
         |            "state" : "$state"
         |        },
         |        {
         |            "friendlyName": "",
         |            "identifiers": [
         |                {
         |                    "key": "TaxOfficeNumber",
         |                    "value": "754"
         |                },
         |                {
         |                    "key": "TaxOfficeReference",
         |                    "value": "KG12514"
         |                }
         |            ],
         |            "service": "$service",
         |            "state" : "$state"
         |        },
         |        {
         |            "friendlyName": "",
         |            "identifiers": [
         |                {
         |                    "key": "TaxOfficeNumber",
         |                    "value": "871"
         |                },
         |                {
         |                    "key": "TaxOfficeReference",
         |                    "value": "AZ00012"
         |                }
         |            ],
         |            "service": "$service",
         |            "state" : "$state"
         |        },
         |        {
         |            "friendlyName": "",
         |            "identifiers": [
         |                {
         |                    "key": "TaxOfficeNumber",
         |                    "value": "123"
         |                },
         |                {
         |                    "key": "TaxOfficeReference",
         |                    "value": "AZ00012"
         |                }
         |            ],
         |            "service": "$service",
         |            "state" : "$state"
         |        },
         |        {
         |            "friendlyName": "",
         |            "identifiers": [
         |                {
         |                    "key": "TaxOfficeNumber",
         |                    "value": "335"
         |                },
         |                {
         |                    "key": "TaxOfficeReference",
         |                    "value": "DE55555"
         |                }
         |            ],
         |            "service": "$service",
         |            "state" : "$state"
         |        }
         |    ],
         |    "startRecord": 1,
         |    "totalRecords": 6
         |}
      """.stripMargin

    stubFor(
      get(urlEqualTo(clientListUrl(service, userId))).willReturn(
        aResponse().withStatus(200).withHeader("Content-Type", "application/json; charset=utf-8").withBody(responseBody)
      )
    )
  }

  def tooFewClientsAreAllocated(service: String, userId: String) = {
    val responseBody =
      s"""
         |{
         |    "enrolments": [
         |        {
         |            "friendlyName": "",
         |            "identifiers": [
         |                {
         |                    "key": "TaxOfficeNumber",
         |                    "value": "754"
         |                },
         |                {
         |                    "key": "TaxOfficeReference",
         |                    "value": "DF10175"
         |                }
         |            ],
         |            "service": "$service",
         |            "state": "Unknown"
         |        },
         |        {
         |            "friendlyName": "",
         |            "identifiers": [
         |                {
         |                    "key": "TaxOfficeNumber",
         |                    "value": "786"
         |                },
         |                {
         |                    "key": "TaxOfficeReference",
         |                    "value": "RZ00013"
         |                }
         |            ],
         |            "service": "$service",
         |            "state": "Unknown"
         |        },
         |        {
         |            "friendlyName": "",
         |            "identifiers": [
         |                {
         |                    "key": "TaxOfficeNumber",
         |                    "value": "754"
         |                },
         |                {
         |                    "key": "TaxOfficeReference",
         |                    "value": "KG12514"
         |                }
         |            ],
         |            "service": "$service",
         |            "state": "Unknown"
         |        },
         |        {
         |            "friendlyName": "",
         |            "identifiers": [
         |                {
         |                    "key": "TaxOfficeNumber",
         |                    "value": "871"
         |                },
         |                {
         |                    "key": "TaxOfficeReference",
         |                    "value": "AZ00012"
         |                }
         |            ],
         |            "service": "$service",
         |            "state": "Unknown"
         |        },
         |        {
         |            "friendlyName": "",
         |            "identifiers": [
         |                {
         |                    "key": "TaxOfficeNumber",
         |                    "value": "335"
         |                },
         |                {
         |                    "key": "TaxOfficeReference",
         |                    "value": "DE55555"
         |                }
         |            ],
         |            "service": "$service",
         |            "state": "Unknown"
         |        }
         |    ],
         |    "startRecord": 1,
         |    "totalRecords": 5
         |}
      """.stripMargin

    stubFor(
      get(urlEqualTo(clientListUrl(service, userId))).willReturn(
        aResponse().withStatus(200).withHeader("Content-Type", "application/json; charset=utf-8").withBody(responseBody)
      )
    )
  }

  def noClientsAreAllocated(service: String, userId: String, statusCode: Int = 204) = {
    stubFor(get(urlEqualTo(clientListUrl(service, userId))).willReturn(aResponse().withStatus(statusCode)))
  }
}
