package uk.gov.hmrc.agentassurance.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.JsValue

trait AgentClientAuthorisationStub {

  def getAgentDetails(responseBody: JsValue, status: Int): StubMapping =
    stubFor(get(urlEqualTo("/agent-client-authorisation/agent/agency-details"))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(responseBody.toString)
      ))

}
