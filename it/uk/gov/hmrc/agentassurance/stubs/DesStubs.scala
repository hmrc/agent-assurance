package uk.gov.hmrc.agentassurance.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.domain.{Nino, SaAgentReference, TaxIdentifier}

trait DesStubs {

  def givenClientIdentifierIsInvalid(identifier: TaxIdentifier) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlMatching(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse().withStatus(400))
    )
  }

  val someAlienAgent = """{"hasAgent":false,"agentId":"alien"}"""
  val someCeasedAgent = """{"hasAgent":true,"agentId":"ex-agent","agentCeasedDate":"someDate"}"""

  def givenClientHasRelationshipWithAgentInCESA(identifier: TaxIdentifier, agentId: SaAgentReference) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlEqualTo(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse().withStatus(200)
          .withBody(s"""{"agents":[$someCeasedAgent,{"hasAgent":true,"agentId":"${agentId.value}"}, $someAlienAgent]}"""))
    )
  }

  def givenClientHasRelationshipWithMultipleAgentsInCESA(identifier: TaxIdentifier, agentIds: Seq[SaAgentReference]) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlEqualTo(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse().withStatus(200)
          .withBody(s"""{"agents":[${agentIds.map(saRef =>s"""{"hasAgent":true,"agentId":"${saRef.value}"}""").mkString(",")}, $someAlienAgent, $someCeasedAgent ]}"""))
    )
  }

  def givenClientRelationshipWithAgentCeasedInCESA(identifier: TaxIdentifier, agentId: String) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlEqualTo(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse().withStatus(200)
          .withBody(s"""{"agents":[{"hasAgent":true,"agentId":"$agentId","agentCeasedDate":"2010-01-01"}]}"""))
    )
  }

  def givenAllClientRelationshipsWithAgentsCeasedInCESA(identifier: TaxIdentifier, agentIds: Seq[String]) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlEqualTo(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse().withStatus(200)
          .withBody(s"""{"agents":[${agentIds.map(id =>s"""{"hasAgent":true,"agentId":"$id","agentCeasedDate":"2020-12-31"}""").mkString(",")}]}"""))
    )
  }

  def givenClientIsUnknown404(identifier: TaxIdentifier) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlEqualTo(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse()
          .withStatus(404)))
  }


  def givenClientHasNoActiveRelationshipWithAgentInCESA(identifier: TaxIdentifier) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlEqualTo(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse().withStatus(200)
          .withBody(s"""{"agents":[$someCeasedAgent, $someAlienAgent]}"""))
    )
  }

  def givenClientHasNoRelationshipWithAnyAgentInCESA(identifier: TaxIdentifier) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlEqualTo(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse().withStatus(200)
          .withBody(s"""{}"""))
    )
  }

  def givenClientIsUnknownInCESAFor(identifier: TaxIdentifier) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlEqualTo(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse().withStatus(404))
    )
  }

  def givenDesReturnsServerError() = {
    stubFor(
      get(urlMatching(s"/registration/.*"))
        .willReturn(aResponse().withStatus(500))
    )
  }

  def givenDesReturnBadGateway() = {
    stubFor(
      get(urlMatching(s"/registration/.*"))
        .willReturn(aResponse().withStatus(502))
    )
  }

  def givenDesReturnsServiceUnavailable() = {
    stubFor(
      get(urlMatching(s"/registration/.*"))
        .willReturn(aResponse().withStatus(503))
    )
  }

  def amlsSubscriptionRecordExists(amlsRegNumber: String) = {
    stubFor(get(urlEqualTo(s"/anti-money-laundering/subscription/$amlsRegNumber/status"))
      .willReturn(aResponse()
        .withStatus(200).withBody(
        s"""{
           |"formBundleStatus": "Approved",
           |"safeId": "xyz",
           |"currentRegYearStartDate": "2021-01-01",
           |"currentRegYearEndDate": "2021-12-31",
           |"suspended": false
           |}""".stripMargin)))
  }

  def amlsSubscriptionRecordFails(amlsRegNumber: String, status: Int) = {
    stubFor(get(urlEqualTo(s"/anti-money-laundering/subscription/$amlsRegNumber/status"))
      .willReturn(aResponse()
        .withStatus(status)))
  }

  private def clientIdentifierType(identifer: TaxIdentifier): String =
    identifer match {
      case _: Nino => "nino"
      case _: Utr => "utr"
      case e => throw new RuntimeException(s"Unacceptable taxIdentifier: $e")
    }
}
