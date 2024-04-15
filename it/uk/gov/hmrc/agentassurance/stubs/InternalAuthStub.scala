package uk.gov.hmrc.agentassurance.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import play.api.http.Status.{CREATED, NOT_FOUND, OK}
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.AUTHORIZATION

trait InternalAuthStub {

  def stubInternalAuthorised(): StubMapping =
    stubFor(
      post(urlEqualTo("/internal-auth/auth"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody("""{"retrievals": {}}""".stripMargin)
        ))

  def getTestStubInternalAuthorised(): StubMapping =
    stubFor(
      get(urlMatching("/test-only/token"))
        .willReturn(aResponse().withStatus(NOT_FOUND))
    )

  def postTestStubInternalAuthorised(): StubMapping =
    stubFor(
      post(urlMatching("/test-only/token"))
        .willReturn(aResponse().withStatus(CREATED))
    )

  def verifyGetTestStubInternalAuth(authToken: String, count:Int = 1): Unit =
    eventually(Timeout(Span(30, Seconds))) {
      verify(
        count,
        getRequestedFor(urlMatching("/test-only/token"))
          .withHeader(AUTHORIZATION, equalTo(authToken))
      )
    }

  def verifyPostTestStubInternalAuth(expectedRequest: JsObject, count:Int = 1): Unit =
    eventually(Timeout(Span(30, Seconds))) {
      verify(
        count,
        postRequestedFor(urlMatching("/test-only/token"))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(expectedRequest))))
      )
    }


}
