/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.uk.gov.hmrc.agentassurance.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import play.api.http.Status.CREATED
import play.api.http.Status.NOT_FOUND
import play.api.http.Status.OK
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.test.Helpers.AUTHORIZATION

trait InternalAuthStub {

  def stubInternalAuthorised(): StubMapping = stubFor(
    post(urlEqualTo("/internal-auth/auth"))
      .willReturn(
        aResponse()
          .withStatus(OK)
          .withBody("""{"retrievals": {}}""".stripMargin)
      )
  )

  def getTestStubInternalAuthorised(): StubMapping = stubFor(
    get(urlMatching("/test-only/token"))
      .willReturn(aResponse().withStatus(NOT_FOUND))
  )

  def postTestStubInternalAuthorised(): StubMapping = stubFor(
    post(urlMatching("/test-only/token"))
      .willReturn(aResponse().withStatus(CREATED))
  )

  def verifyGetTestStubInternalAuth(
    authToken: String,
    count: Int = 1
  ): Unit =
    eventually(Timeout(Span(30, Seconds))) {
      verify(
        count,
        getRequestedFor(urlMatching("/test-only/token"))
          .withHeader(AUTHORIZATION, equalTo(authToken))
      )
    }

  def verifyPostTestStubInternalAuth(
    expectedRequest: JsObject,
    count: Int = 1
  ): Unit =
    eventually(Timeout(Span(30, Seconds))) {
      verify(
        count,
        postRequestedFor(urlMatching("/test-only/token"))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(expectedRequest))))
      )
    }

}
