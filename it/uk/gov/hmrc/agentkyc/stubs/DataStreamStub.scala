package uk.gov.hmrc.agentkyc.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.Eventually

trait DataStreamStub extends Eventually {

  def givenAuditConnector() = {
    stubFor(post(urlPathEqualTo(auditUrl)).willReturn(aResponse().withStatus(200)))
  }

  private def auditUrl = "/write/audit"

}
