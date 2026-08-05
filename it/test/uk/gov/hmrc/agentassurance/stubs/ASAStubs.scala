package uk.gov.hmrc.agentassurance.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.client.WireMock.*
import play.api.libs.json.Json
import uk.gov.hmrc.agentassurance.models.Arn

trait ASAStubs {

  def givenASAGetAgentRecord(arn: Arn, hasUkAddress: Boolean): StubMapping = stubFor(
    get(urlEqualTo(s"/agent-services-account/agent-record-with-checks/arn/${arn.value}"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(Json.obj(
            "agencyDetails" -> Json.obj(
              "agencyAddress" -> Json.obj(
                "countryCode" -> (if hasUkAddress then "GB" else "IN")
              )
            )
          ).toString)
      )
  )

}
