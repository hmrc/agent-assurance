package uk.gov.hmrc.agentassurance.stubs

import com.github.tomakehurst.wiremock.client.WireMock._

trait EnrolmentStoreProxyStubs {
  private def clientListUrl(service: String, userId: String) = {
    s"/enrolment-store-proxy/enrolment-store/users/$userId/enrolments?type=delegated&service=$service"
  }

  def sufficientClientsAreAllocated(service: String, userId: String) = {
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
        |                    "value": "123"
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
        |    "totalRecords": 6
        |}
      """.stripMargin

    stubFor(get(urlEqualTo(clientListUrl(service, userId))).willReturn(
      aResponse().withStatus(200).withHeader("Content-Type", "application/json; charset=utf-8").withBody(responseBody))
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

    stubFor(get(urlEqualTo(clientListUrl(service, userId))).willReturn(
      aResponse().withStatus(200).withHeader("Content-Type", "application/json; charset=utf-8").withBody(responseBody))
    )
  }

  def noClientsAreAllocated(service: String, userId: String, statusCode: Int = 204) = {
    stubFor(get(urlEqualTo(clientListUrl(service, userId))).willReturn(aResponse().withStatus(statusCode)))
  }
}
