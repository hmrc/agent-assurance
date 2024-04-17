package uk.gov.hmrc.agentassurance.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}

trait DmsSubmissionStubs {

  def givenDmsSubmissionSuccess: StubMapping =
    stubFor(
      post(urlEqualTo("/dms-submission/submit"))
        .willReturn(aResponse()
          .withStatus(ACCEPTED))

    )

  def givenDmsSubmission5xx: StubMapping =
    stubFor(
      post(urlEqualTo("/dms-submission/submit"))
        .willReturn(aResponse()
          .withStatus(INTERNAL_SERVER_ERROR))

    )

}
