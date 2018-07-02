package uk.gov.hmrc.agentassurance.support

import com.github.tomakehurst.wiremock.client.WireMock._

trait BasicUserAuthStubs { WiremockAware =>
  def isNotLoggedIn = {
    stubFor(post(urlPathEqualTo(s"/auth/authorise")).willReturn(aResponse().withStatus(401)
      .withHeader("WWW-Authenticate", s"""MDTP detail="MissingBearerToken"""")))
    this
  }
}

trait AgentAuthStubs extends BasicUserAuthStubs {
  def irAgentReference: String

  def isLoggedInAndIsEnrolledToIrSaAgent = {
    stubFor(post(urlPathEqualTo(s"/auth/authorise")).willReturn(aResponse().withStatus(200).withBody(
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
    )))
    this
  }

  def isLoggedInAndNotEnrolledInIrSaAgent = {
    stubFor(post(urlPathEqualTo(s"/auth/authorise")).willReturn(aResponse().withStatus(200).withBody(
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
    )))
    this
  }

  def isLoggedInWithUserId(userId: String) = {
    stubFor(post(urlPathEqualTo(s"/auth/authorise"))
        .withRequestBody(equalToJson(
          """{
            |	"authorise": [{"authProviders": ["GovernmentGateway"]}],
            |	"retrieve": ["credentials"]
            |}""".stripMargin))
      .willReturn(aResponse().withStatus(200).withBody(
      s"""
         |{
         |  "credentials" : {
         |    "providerId" : "$userId",
         |    "providerType" : "GovernmentGateway"
         |    }
         |}
       """.stripMargin
    )))
    this
  }

  def isLoggedInWithoutUserId = {
    stubFor(post(urlPathEqualTo(s"/auth/authorise")).willReturn(aResponse().withStatus(200).withBody("{}")))
    this
  }
}