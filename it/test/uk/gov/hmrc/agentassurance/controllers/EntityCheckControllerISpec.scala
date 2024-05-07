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

import scala.concurrent.Await

import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.NO_CONTENT
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers.CONTENT_TYPE
import play.api.Application
import test.uk.gov.hmrc.agentassurance.stubs.DesStubs
import test.uk.gov.hmrc.agentassurance.stubs.InternalAuthStub
import test.uk.gov.hmrc.agentassurance.support.AgentAuthStubs
import test.uk.gov.hmrc.agentassurance.support.InstantClockTestSupport
import test.uk.gov.hmrc.agentassurance.support.WireMockSupport
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testArn
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testArn1
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testArn2
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testArn3
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testSaUtr
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testUtr
import uk.gov.hmrc.agentassurance.models.entitycheck.VerifyEntityRequest
import uk.gov.hmrc.agentassurance.stubs.CitizenDetailsStubs
import uk.gov.hmrc.agentassurance.stubs.EmailStub
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

class EntityCheckControllerISpec
    extends PlaySpec
    with AgentAuthStubs
    with GuiceOneServerPerSuite
    with WireMockSupport
    with CleanMongoCollectionSupport
    with InstantClockTestSupport
    with DesStubs
    with InternalAuthStub
    with CitizenDetailsStubs
    with EmailStub {

  implicit override lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.host"            -> wireMockHost,
        "microservice.services.auth.port"            -> wireMockPort,
        "microservice.services.des.host"             -> wireMockHost,
        "microservice.services.des.port"             -> wireMockPort,
        "microservice.services.citizen-details.host" -> wireMockHost,
        "microservice.services.citizen-details.port" -> wireMockPort,
        "microservice.services.internal-auth.port"   -> wireMockPort,
        "microservice.services.internal-auth.host"   -> wireMockHost,
        "microservice.services.email.port"           -> wireMockPort,
        "microservice.services.email.host"           -> wireMockHost,
        "auditing.enabled"                           -> false,
        "stride.roles.agent-assurance"               -> "maintain_agent_manually_assure",
        "internal-auth-token-enabled-on-start"       -> false,
        "http-verbs.retries.intervals"               -> List("1ms"),
        "agent.cache.enabled"                        -> true,
        "agent.cache.expires"                        -> "1 second",
        "agent.entity-check.lock.expires"            -> "1 second",
        "agent.entity-check.email.lock.expires"      -> "1 seconds"
      )

  val arn       = Arn("AARN0000002")
  val clientUrl = s"http://localhost:$port/agent-assurance/client/verify-entity"
  val agentUrl  = s"http://localhost:$port/agent-assurance/agent/verify-entity"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  def doClientPostRequest(body: VerifyEntityRequest) =
    Await.result(
      wsClient
        .url(clientUrl)
        .withHttpHeaders("Authorization" -> "internal auth token", CONTENT_TYPE -> "application/json")
        .post(Json.toJson(body)),
      15.seconds
    )

  def doAgentPostRequest() =
    Await.result(
      wsClient
        .url(agentUrl)
        .withHttpHeaders("Authorization" -> "Bearer XYZ", CONTENT_TYPE -> "application/json")
        .post(""),
      15.seconds
    )

  "POST /agent-assurance/client/verify-entity" should {

    "return suspension details when agent record contains suspension details" in {
      Thread.sleep(1000) // To make sure cache expires
      stubInternalAuthorised()
      givenDESGetAgentRecordSuspendedAgent(
        testArn,
        Some(testUtr)
      )

      givenCitizenReturnDeceasedFlag(testSaUtr, false)

      val response = doClientPostRequest(VerifyEntityRequest(testArn))
      response.json mustBe Json.obj("suspensionStatus" -> true, "regimes" -> Set("ITSA"))
      response.status mustBe OK

      // TODO WG - flaky test - fix
//      val response2 = doClientPostRequest(VerifyEntityRequest(testArn))
//      response2.json mustBe Json.obj("suspensionStatus" -> true, "regimes" -> Set("ITSA"))
//      response2.status mustBe OK
//
//      verifyCitizenDetailsWasCalled(testSaUtr, 1)
//      verifyDESGetAgentRecord(testArn, 1)

    }

    "return suspension details and send email" in {
      Thread.sleep(1000) // To make sure cache expires
      stubInternalAuthorised()
      givenDESGetAgentRecordSuspendedAgent(
        testArn,
        Some(testUtr)
      )

      givenCitizenReturnDeceasedFlag(testSaUtr, true)

      val response = doClientPostRequest(VerifyEntityRequest(testArn))
      response.json mustBe Json.obj("suspensionStatus" -> true, "regimes" -> Set("ITSA"))
      response.status mustBe OK

    }

  }

  "POST /agent-assurance/agent/verify-entity" should {
    "return NO_CONTENT when agent record contains suspension status false" in {

      isLoggedInAsASAgent(testArn1)
      givenDESGetAgentRecord(testArn1, Some(testUtr))
      givenCitizenReturnDeceasedFlag(testSaUtr, false)

      val response = doAgentPostRequest()

      response.status mustBe NO_CONTENT
    }

    "return NO_CONTENT when agent record contains no suspension details" in {

      isLoggedInAsASAgent(testArn2)
      givenDESGetAgentRecordNoSuspensionDetails(
        testArn2,
        Some(testUtr)
      )
      givenCitizenReturnDeceasedFlag(testSaUtr, false)

      val response = doAgentPostRequest()

      response.status mustBe NO_CONTENT
    }

    "return suspension details when agent record contains suspension details" in {

      isLoggedInAsASAgent(testArn3)
      givenDESGetAgentRecordSuspendedAgent(
        testArn3,
        Some(testUtr)
      )
      givenCitizenReturnDeceasedFlag(testSaUtr, false)

      val response = doAgentPostRequest()

      response.status mustBe OK
      response.json mustBe Json.obj("suspensionStatus" -> true, "regimes" -> Set("ITSA"))
    }
  }

}
