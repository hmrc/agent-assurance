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

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

import com.google.inject.AbstractModule
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
import uk.gov.hmrc.agentassurance.helpers.TestConstants._
import uk.gov.hmrc.agentassurance.models.entitycheck.VerifyEntityRequest
import uk.gov.hmrc.agentassurance.models.EmailInformation
import uk.gov.hmrc.agentassurance.models.Property
import uk.gov.hmrc.agentassurance.models.Value
import uk.gov.hmrc.agentassurance.repositories.PropertiesRepository
import uk.gov.hmrc.agentassurance.repositories.PropertiesRepositoryImpl
import uk.gov.hmrc.agentassurance.stubs.CitizenDetailsStubs
import uk.gov.hmrc.agentassurance.stubs.EmailStub
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
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

  protected val propertiesRepository: PlayMongoRepository[Property] =
    new PropertiesRepositoryImpl(mongoComponent)

  val moduleWithOverrides: AbstractModule = new AbstractModule() {
    override def configure(): Unit = {
      bind(classOf[PropertiesRepository]).toInstance(propertiesRepository.asInstanceOf[PropertiesRepositoryImpl])
    }
  }

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
      .overrides(moduleWithOverrides)

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

    "return suspension details and send email for deceased" in {
      Thread.sleep(1000) // To make sure cache expires
      val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy h:mma")
      val dateTime  = formatter.format(LocalDateTime.now())

      stubInternalAuthorised()
      givenDESGetAgentRecordSuspendedAgent(
        testArn,
        Some(testUtr)
      )

      givenCitizenReturnDeceasedFlag(testSaUtr, true)

      givenEmailSent(emailInformation =
        EmailInformation(
          to = Seq("test@example.com"),
          templateId = "entity_check_notification",
          parameters = Map(
            "arn"          -> "ARN123",
            "dateTime"     -> dateTime,
            "agencyName"   -> "ABC Accountants",
            "failedChecks" -> "Agent is deceased",
            "utr"          -> "7000000002"
          ),
          force = true
        )
      )

      val response = doClientPostRequest(VerifyEntityRequest(testArn))

      verifyEmailRequestWasSent(1)
      response.json mustBe Json.obj("suspensionStatus" -> true, "regimes" -> Set("ITSA"))
      response.status mustBe OK

    }

    "return suspension details and send email for refuse to deal with" in {
      Thread.sleep(1000) // To make sure cache expires
      val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy h:mma")
      val dateTime  = formatter.format(LocalDateTime.now())

      stubInternalAuthorised()
      givenDESGetAgentRecordSuspendedAgent(
        testArn,
        Some(testUtr)
      )

      givenCitizenReturnDeceasedFlag(testSaUtr, false)
      propertiesRepository.collection
        .insertOne(Value(testSaUtr.value).toProperty("refusal-to-deal-with"))
        .toFuture()
        .futureValue

      givenEmailSent(emailInformation =
        EmailInformation(
          to = Seq("test@example.com"),
          templateId = "entity_check_notification",
          parameters = Map(
            "arn"          -> "ARN123",
            "dateTime"     -> dateTime,
            "agencyName"   -> "ABC Accountants",
            "failedChecks" -> "Agent is on the 'Refuse To Deal With' list",
            "utr"          -> "7000000002"
          ),
          force = true
        )
      )

      val response = doClientPostRequest(VerifyEntityRequest(testArn))

      verifyEmailRequestWasSent(1)
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
