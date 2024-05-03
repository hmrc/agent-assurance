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
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testUtr
import uk.gov.hmrc.agentassurance.models.entityCheck.VerifyEntityRequest
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
    with InternalAuthStub {

  implicit override lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.host"      -> wireMockHost,
        "microservice.services.auth.port"      -> wireMockPort,
        "microservice.services.des.host"       -> wireMockHost,
        "microservice.services.des.port"       -> wireMockPort,
        "auditing.enabled"                     -> false,
        "stride.roles.agent-assurance"         -> "maintain_agent_manually_assure",
        "internal-auth-token-enabled-on-start" -> false,
        "http-verbs.retries.intervals"         -> List("1ms"),
        "agent.cache.enabled"                  -> false
      )

  val arn       = Arn("AARN0000002")
  val clientUrl = s"http://localhost:$port/agent-assurance/client/verify-entity"
  val agentUrl  = s"http://localhost:$port/agent-assurance/agent/verify-entity"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  def doClientPostRequest(body: VerifyEntityRequest) =
    Await.result(
      wsClient
        .url(clientUrl)
        .withHttpHeaders("Authorization" -> "test", CONTENT_TYPE -> "application/json")
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

      stubInternalAuthorised()
      givenDESGetAgentRecord(arn, Some(testUtr))

      val response = doClientPostRequest(VerifyEntityRequest(arn))

      response.status mustBe OK

    }
  }

  "POST /agent-assurance/agent/verify-entity" should {
    "return NO_CONTENT when agent record contains suspension status false" in {

      isLoggedInAsASAgent(testArn)
      givenDESGetAgentRecord(testArn, Some(testUtr))

      val response = doAgentPostRequest()

      response.status mustBe NO_CONTENT
    }

    "return suspension details when agent record contains suspension details" in {

      isLoggedInAsASAgent(testArn)
      givenDESGetAgentRecord(
        testArn,
        Some(testUtr)
      )

      val response = doAgentPostRequest()

      response.status mustBe OK

    }
  }

}
