package uk.gov.hmrc.agentassurance.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.domain.AgentCode

trait GovernmentGatewayStubs {
  private def clientListUrl(service: String, agentCode: AgentCode) = {
    s"/agent/${agentCode.value}/client-list/$service/all"
  }

  def sufficientClientsAreAllocated(service: String, agentCode: AgentCode) = {
    val responseBody =
      """[
        |{"friendlyName":"","identifiersForDisplay":[{"type":"TaxOfficeNumber","value":"754"},{"type":"TaxOfficeReference","value":"DF10175"}]},
        |{"friendlyName":"","identifiersForDisplay":[{"type":"TaxOfficeNumber","value":"786"},{"type":"TaxOfficeReference","value":"RZ00013"}]},
        |{"friendlyName":"","identifiersForDisplay":[{"type":"TaxOfficeNumber","value":"754"},{"type":"TaxOfficeReference","value":"KG12514"}]},
        |{"friendlyName":"","identifiersForDisplay":[{"type":"TaxOfficeNumber","value":"871"},{"type":"TaxOfficeReference","value":"AZ00012"}]},
        |{"friendlyName":"","identifiersForDisplay":[{"type":"TaxOfficeNumber","value":"123"},{"type":"TaxOfficeReference","value":"AZ00012"}]},
        |{"friendlyName":"","identifiersForDisplay":[{"type":"TaxOfficeNumber","value":"335"},{"type":"TaxOfficeReference","value":"DE55555"}]}]""".stripMargin

    stubFor(get(urlPathEqualTo(clientListUrl(service, agentCode))).willReturn(
      aResponse().withStatus(200).withHeader("Content-Type", "application/json; charset=utf-8").withBody(responseBody))
    )
  }

  def tooFewClientsAreAllocated(service: String, agentCode: AgentCode) = {
    val responseBody =
      """[
        |{"friendlyName":"","identifiersForDisplay":[{"type":"TaxOfficeNumber","value":"754"},{"type":"TaxOfficeReference","value":"DF10175"}]},
        |{"friendlyName":"","identifiersForDisplay":[{"type":"TaxOfficeNumber","value":"786"},{"type":"TaxOfficeReference","value":"RZ00013"}]},
        |{"friendlyName":"","identifiersForDisplay":[{"type":"TaxOfficeNumber","value":"754"},{"type":"TaxOfficeReference","value":"KG12514"}]},
        |{"friendlyName":"","identifiersForDisplay":[{"type":"TaxOfficeNumber","value":"871"},{"type":"TaxOfficeReference","value":"AZ00012"}]},
        |{"friendlyName":"","identifiersForDisplay":[{"type":"TaxOfficeNumber","value":"335"},{"type":"TaxOfficeReference","value":"DE55555"}]}]""".stripMargin

    stubFor(get(urlPathEqualTo(clientListUrl(service, agentCode))).willReturn(
      aResponse().withStatus(200).withHeader("Content-Type", "application/json; charset=utf-8").withBody(responseBody))
    )
  }

  def noClientsAreAllocated(service: String, agentCode: AgentCode, statusCode: Int = 204) = {
    stubFor(get(urlPathEqualTo(clientListUrl(service, agentCode))).willReturn(aResponse().withStatus(statusCode)))
  }
}
