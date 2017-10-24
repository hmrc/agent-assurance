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
        |  {"friendlyName" : "alice", "empRef": "123/AAA"},
        |  {"friendlyName" : "bob", "empRef": "123/BBB"},
        |  {"friendlyName" : "chris", "empRef": "123/CCC"},
        |  {"friendlyName" : "dilbert", "empRef": "123/DDD"},
        |  {"friendlyName" : "ed", "empRef": "123/EEE"},
        |  {"friendlyName" : "frank", "empRef": "123/FFF"}
        |]""".stripMargin

    stubFor(get(urlPathEqualTo(clientListUrl(service, agentCode))).willReturn(
      aResponse().withStatus(200).withHeader("Content-Type", "application/json; charset=utf-8").withBody(responseBody))
    )
  }

  def tooFewClientsAreAllocated(service: String, agentCode: AgentCode) = {
    val responseBody =
      """[
        |  {"friendlyName" : "alice", "empRef": "123/AAA"},
        |  {"friendlyName" : "bob", "empRef": "123/BBB"},
        |  {"friendlyName" : "chris", "empRef": "123/CCC"},
        |  {"friendlyName" : "dilbert", "empRef": "123/DDD"},
        |  {"friendlyName" : "ed", "empRef": "123/EEE"}
        |]""".stripMargin

    stubFor(get(urlPathEqualTo(clientListUrl(service, agentCode))).willReturn(
      aResponse().withStatus(200).withHeader("Content-Type", "application/json; charset=utf-8").withBody(responseBody))
    )
  }

  def noClientsAreAllocated(service: String, agentCode: AgentCode, statusCode: Int = 204) = {
    stubFor(get(urlPathEqualTo(clientListUrl(service, agentCode))).willReturn(aResponse().withStatus(statusCode)))
  }
}
