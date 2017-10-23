package uk.gov.hmrc.agentassurance.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.domain.AgentCode

trait GovernmentGatewayStubs {
  private def clientListUrl(agentCode: AgentCode) = {
    s"/agent/${agentCode.value}/client-list/IR-PAYE/all"
  }

  def sufficientClientsAreAllocated(agentCode: AgentCode) = {
    val responseBody =
      """[
        |  {"friendlyName" : "alice", "empRef": "123/AAA"},
        |  {"friendlyName" : "bob", "empRef": "123/BBB"},
        |  {"friendlyName" : "chris", "empRef": "123/CCC"},
        |  {"friendlyName" : "dilbert", "empRef": "123/DDD"},
        |  {"friendlyName" : "ed", "empRef": "123/EEE"},
        |  {"friendlyName" : "frank", "empRef": "123/FFF"}
        |]""".stripMargin

    stubFor(get(urlPathEqualTo(clientListUrl(agentCode))).willReturn(
      aResponse().withStatus(200).withHeader("Content-Type", "application/json; charset=utf-8").withBody(responseBody))
    )
  }

  def tooFewClientsAreAllocated(agentCode: AgentCode) = {
    val responseBody =
      """[
        |  {"friendlyName" : "alice", "empRef": "123/AAA"},
        |  {"friendlyName" : "bob", "empRef": "123/BBB"},
        |  {"friendlyName" : "chris", "empRef": "123/CCC"},
        |  {"friendlyName" : "dilbert", "empRef": "123/DDD"},
        |  {"friendlyName" : "ed", "empRef": "123/EEE"}
        |]""".stripMargin

    stubFor(get(urlPathEqualTo(clientListUrl(agentCode))).willReturn(
      aResponse().withStatus(200).withHeader("Content-Type", "application/json; charset=utf-8").withBody(responseBody))
    )
  }

  def noClientsAreAllocated(agentCode: AgentCode) = {
    stubFor(get(urlPathEqualTo(clientListUrl(agentCode))).willReturn(aResponse().withStatus(204)))
  }
}
