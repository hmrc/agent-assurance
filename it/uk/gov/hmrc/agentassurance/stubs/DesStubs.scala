package uk.gov.hmrc.agentassurance.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.domain.{Nino, SaAgentReference, TaxIdentifier}

trait DesStubs {

  def givenNinoIsInvalid(nino: Nino) = {
    stubFor(
      get(urlMatching(s"/registration/relationship/nino/${nino.value}"))
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

  def givenClientHasRelationshipWithMultipleAgentsInCESA(nino: Nino, agentIds: Seq[SaAgentReference]) = {
    stubFor(
      get(urlEqualTo(s"/registration/relationship/nino/${nino.value}"))
        .willReturn(aResponse().withStatus(200)
          .withBody(s"""{"agents":[${agentIds.map(saRef =>s"""{"hasAgent":true,"agentId":"${saRef.value}"}""").mkString(",")}, $someAlienAgent, $someCeasedAgent ]}"""))
    )
  }

  def givenClientRelationshipWithAgentCeasedInCESA(nino: Nino, agentId: String) = {
    stubFor(
      get(urlEqualTo(s"/registration/relationship/nino/${nino.value}"))
        .willReturn(aResponse().withStatus(200)
          .withBody(s"""{"agents":[{"hasAgent":true,"agentId":"$agentId","agentCeasedDate":"2010-01-01"}]}"""))
    )
  }

  def givenAllClientRelationshipsWithAgentsCeasedInCESA(nino: Nino, agentIds: Seq[String]) = {
    stubFor(
      get(urlEqualTo(s"/registration/relationship/nino/${nino.value}"))
        .willReturn(aResponse().withStatus(200)
          .withBody(s"""{"agents":[${agentIds.map(id =>s"""{"hasAgent":true,"agentId":"$id","agentCeasedDate":"2020-12-31"}""").mkString(",")}]}"""))
    )
  }

  def givenClientHasNoActiveRelationshipWithAgentInCESA(nino: Nino) = {
    stubFor(
      get(urlEqualTo(s"/registration/relationship/nino/${nino.value}"))
        .willReturn(aResponse().withStatus(200)
          .withBody(s"""{"agents":[$someCeasedAgent, $someAlienAgent]}"""))
    )
  }

  def givenClientHasNoRelationshipWithAnyAgentInCESA(nino: Nino) = {
    stubFor(
      get(urlEqualTo(s"/registration/relationship/nino/${nino.value}"))
        .willReturn(aResponse().withStatus(200)
          .withBody(s"""{}"""))
    )
  }

  def givenClientIsUnknownInCESAFor(nino: Nino) = {
    stubFor(
      get(urlEqualTo(s"/registration/relationship/nino/${nino.value}"))
        .willReturn(aResponse().withStatus(404))
    )
  }

  def givenDesReturnsServerError() = {
    stubFor(
      get(urlMatching(s"/registration/.*"))
        .willReturn(aResponse().withStatus(500))
    )
  }

  def givenDesReturnsServiceUnavailable() = {
    stubFor(
      get(urlMatching(s"/registration/.*"))
        .willReturn(aResponse().withStatus(503))
    )
  }

  private def clientIdentifierType(identifer: TaxIdentifier): String =
    identifer match {
      case _: Nino => "nino"
      case _: Utr => "utr"
    }
}
