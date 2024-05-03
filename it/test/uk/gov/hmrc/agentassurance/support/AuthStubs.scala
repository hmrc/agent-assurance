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

package test.uk.gov.hmrc.agentassurance.support

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

trait BasicUserAuthStubs { WiremockAware =>
  def isNotLoggedIn = {
    stubFor(
      post(urlPathEqualTo(s"/auth/authorise")).willReturn(
        aResponse()
          .withStatus(401)
          .withHeader("WWW-Authenticate", s"""MDTP detail="MissingBearerToken"""")
      )
    )
    this
  }
}

trait AgentAuthStubs extends BasicUserAuthStubs {
  def irAgentReference: String = "IRSA-123"

  def isLoggedInAsAnAfinityGroupAgent(userId: String) = {
    stubFor(
      post(urlPathEqualTo(s"/auth/authorise")).willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               |  "affinityGroup": "Agent",
               |  "allEnrolments": [
               |  ],
               |  "optionalCredentials" : {
               |    "providerId" : "$userId",
               |    "providerType" : "GovernmentGateway"
               |    }
               |}
       """.stripMargin
          )
      )
    )
    this
  }

  def isLoggedInAsStride(userId: String) = {
    stubFor(
      post(urlPathEqualTo(s"/auth/authorise")).willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               |  "allEnrolments": [
               |    {
               |      "key": "maintain_agent_manually_assure",
               |      "identifiers": [
               |      ],
               |      "state": "Activated"
               |    }
               |  ],
               |  "optionalCredentials" : {
               |    "providerId" : "$userId",
               |    "providerType" : "PrivilegedApplication"
               |    }
               |}
       """.stripMargin
          )
      )
    )
    this
  }

  def isLoggedInAndIsEnrolledToIrSaAgent = {
    stubFor(
      post(urlPathEqualTo(s"/auth/authorise")).willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               |  "affinityGroup": "Agent",
               |  "allEnrolments": [
               |    {
               |      "key": "IR-SA-AGENT",
               |      "identifiers": [
               |        {
               |          "key": "IRAgentReference",
               |          "value": "$irAgentReference"
               |        }
               |      ],
               |      "state": "Activated"
               |    },
               |    {
               |      "key": "HMRC-AS-AGENT",
               |      "identifiers": [
               |        {
               |          "key": "arn",
               |          "value": "NARN0123456"
               |        }
               |      ],
               |      "state": "Activated"
               |    }
               |  ]
               |}
       """.stripMargin
          )
      )
    )
    this
  }

  def isLoggedInAndNotEnrolledInIrSaAgent = {
    stubFor(
      post(urlPathEqualTo(s"/auth/authorise")).willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               |  "affinityGroup": "Agent",
               |  "allEnrolments": [
               |    {
               |      "key": "HMRC-AGENT-AGENT",
               |      "identifiers": [
               |        {
               |          "key": "IRAgentReference",
               |          "value": "JARN1234567"
               |        }
               |      ],
               |      "state": "Activated"
               |    },
               |    {
               |      "key": "IR-PAYE-AGENT",
               |      "identifiers": [
               |        {
               |          "key": "IrAgentReference",
               |          "value": "HZ1234"
               |        }
               |      ],
               |      "state": "Activated"
               |    }
               |  ]
               |}
       """.stripMargin
          )
      )
    )
    this
  }

  def isLoggedInWithUserId(userId: String) = {
    stubFor(
      post(urlPathEqualTo(s"/auth/authorise"))
        .withRequestBody(equalToJson("""{
                                       |	"authorise": [{"authProviders": ["GovernmentGateway"]}],
                                       |	"retrieve": ["optionalCredentials"]
                                       |}""".stripMargin))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              s"""
                 |{
                 |  "optionalCredentials" : {
                 |    "providerId" : "$userId",
                 |    "providerType" : "GovernmentGateway"
                 |    }
                 |}
       """.stripMargin
            )
        )
    )
    this
  }

  def isLoggedInWithoutUserId = {
    stubFor(post(urlPathEqualTo(s"/auth/authorise")).willReturn(aResponse().withStatus(200).withBody("{}")))
    this
  }

  def withAffinityGroupAgent = {
    stubFor(
      post(urlPathEqualTo(s"/auth/authorise"))
        .withRequestBody(
          equalToJson("""{
                        |	"authorise": [{"authProviders": ["GovernmentGateway"]}, {"affinityGroup": "Agent"}],
                        | "retrieve" : [ ]
                        |}""".stripMargin)
        )
        .willReturn(aResponse().withStatus(200).withBody("{}"))
    )
    this
  }

  def isLoggedInAsASAgent(arn: Arn): AgentAuthStubs = {
    stubFor(
      post(urlPathEqualTo(s"/auth/authorise")).willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               |  "affinityGroup": "Agent",
               |  "allEnrolments": [
               |    {
               |      "key": "HMRC-AS-AGENT",
               |      "identifiers": [
               |        {
               |          "key": "AgentReferenceNumber",
               |          "value": "${arn.value}"
               |        }
               |      ],
               |      "state": "Activated"
               |    }
               |  ]
               |}
       """.stripMargin
          )
      )
    )
    this
  }
}
