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

package uk.gov.hmrc.agentassurance.controllers

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import test.uk.gov.hmrc.agentassurance.stubs.DesStubs
import test.uk.gov.hmrc.agentassurance.stubs.InternalAuthStub
import test.uk.gov.hmrc.agentassurance.support.AgentAuthStubs
import test.uk.gov.hmrc.agentassurance.support.InstantClockTestSupport
import test.uk.gov.hmrc.agentassurance.support.WireMockSupport
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class GetAgentRecordWithEntityChecksControllerISpec
extends PlaySpec
with AgentAuthStubs
with GuiceOneServerPerSuite
with WireMockSupport
with InstantClockTestSupport
with InternalAuthStub
with DesStubs {

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.host" -> wireMockHost,
      "microservice.services.auth.port" -> wireMockPort,
      "microservice.services.des.host" -> wireMockHost,
      "microservice.services.des.port" -> wireMockPort,
      "microservice.services.internal-auth.port" -> wireMockPort,
      "microservice.services.internal-auth.host" -> wireMockHost,
      "auditing.enabled" -> false,
      "stride.roles.agent-assurance" -> "maintain_agent_manually_assure",
      "internal-auth-token-enabled-on-start" -> false,
      "http-verbs.retries.intervals" -> List("1ms"),
      "agent.cache.enabled" -> false
    )

  val arn = Arn("AARN0000002")
  val url = s"http://localhost:$port/agent-assurance/agent-record-with-checks"
  val clientUrl = s"http://localhost:$port/agent-assurance/agent-record-with-checks/arn/${arn.value}"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  def doGetRequest() = Await.result(
    wsClient
      .url(url)
      .withHttpHeaders("Authorization" -> "Bearer secret")
      .get(),
    15.seconds
  )

  def doClientGetRequest() = Await.result(
    wsClient
      .url(clientUrl)
      .withHttpHeaders("Authorization" -> "internal auth token")
      .get(),
    15.seconds
  )

  "get" should {
    "return agent record" in {

      isLoggedInAsASAgent(arn)
      givenDESGetAgentRecord(arn, None)

      val response = doGetRequest()

      response.status mustBe OK

      response.json mustBe Json.obj(
        "agencyDetails" -> Json.obj(
          "agencyName" -> "ABC Accountants",
          "agencyEmail" -> "abc@xyz.com",
          "agencyTelephone" -> "07345678901",
          "agencyAddress" -> Json.obj(
            "addressLine1" -> "Matheson House",
            "addressLine2" -> "Grange Central",
            "addressLine3" -> "Town Centre",
            "addressLine4" -> "Telford",
            "postalCode" -> "TF3 4ER",
            "countryCode" -> "GB"
          )
        ),
        "suspensionDetails" -> Json.obj(
          "suspensionStatus" -> false
        ),
        "isAnIndividual" -> false
      )

    }
  }

  "get with Arn" should {
    "return agent record" in {

      stubInternalAuthorised()
      givenDESGetAgentRecord(arn, None)

      val response = doClientGetRequest()

      response.status mustBe OK

      response.json mustBe Json.obj(
        "agencyDetails" -> Json.obj(
          "agencyName" -> "ABC Accountants",
          "agencyEmail" -> "abc@xyz.com",
          "agencyTelephone" -> "07345678901",
          "agencyAddress" -> Json.obj(
            "addressLine1" -> "Matheson House",
            "addressLine2" -> "Grange Central",
            "addressLine3" -> "Town Centre",
            "addressLine4" -> "Telford",
            "postalCode" -> "TF3 4ER",
            "countryCode" -> "GB"
          )
        ),
        "suspensionDetails" -> Json.obj(
          "suspensionStatus" -> false
        ),
        "isAnIndividual" -> false
      )

    }
  }

}
