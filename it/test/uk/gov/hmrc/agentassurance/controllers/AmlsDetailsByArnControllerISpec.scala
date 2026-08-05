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

import java.time.LocalDate
import scala.concurrent.duration.DurationInt
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import com.google.inject.AbstractModule
import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.SingleObservableFuture
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.BodyWritable
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.*
import play.api.Application
import play.api.libs.ws.DefaultBodyWritables.writeableOf_String
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testAgentDetailsDesAddressUtrResponse
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testAgentDetailsDesOverseas
import uk.gov.hmrc.agentassurance.stubs.ASAStubs
import uk.gov.hmrc.agentassurance.stubs.DesStubs
import uk.gov.hmrc.agentassurance.support.AgentAuthStubs
import uk.gov.hmrc.agentassurance.support.InstantClockTestSupport
import uk.gov.hmrc.agentassurance.support.WireMockSupport
import uk.gov.hmrc.agentassurance.models.*
import uk.gov.hmrc.agentassurance.repositories.*
import uk.gov.hmrc.agentassurance.models.Arn
import uk.gov.hmrc.agentassurance.models.Utr
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

class AmlsDetailsByArnControllerISpec
extends PlaySpec
with AgentAuthStubs
with GuiceOneServerPerSuite
with WireMockSupport
with CleanMongoCollectionSupport
with InstantClockTestSupport
with DesStubs
with ASAStubs {

  override implicit lazy val app: Application = appBuilder.build()

  protected val ukAmlsRepository: PlayMongoRepository[UkAmlsEntity] = new AmlsRepositoryImpl(mongoComponent)

  protected val overseasAmlsRepository: PlayMongoRepository[OverseasAmlsEntity] = new OverseasAmlsRepositoryImpl(mongoComponent)

  val moduleWithOverrides: AbstractModule =
    new AbstractModule() {
      override def configure(): Unit = {
        bind(classOf[OverseasAmlsRepository]).toInstance(overseasAmlsRepository.asInstanceOf[OverseasAmlsRepositoryImpl])
        bind(classOf[AmlsRepository]).toInstance(ukAmlsRepository.asInstanceOf[AmlsRepositoryImpl])
      }
    }

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.host" -> wireMockHost,
      "microservice.services.auth.port" -> wireMockPort,
      "microservice.services.des.host" -> wireMockHost,
      "microservice.services.des.port" -> wireMockPort,
      "microservice.services.agent-services-account.host" -> wireMockHost,
      "microservice.services.agent-services-account.port" -> wireMockPort,
      "auditing.enabled" -> false,
      "stride.roles.agent-assurance" -> "maintain_agent_manually_assure",
      "internal-auth-token-enabled-on-start" -> false,
      "http-verbs.retries.intervals" -> List("1ms"),
      "agent.cache.enabled" -> false
    )
    .overrides(moduleWithOverrides)

  val arn = Arn("AARN0000002")
  val url = s"http://localhost:$port/agent-assurance/amls/arn/${arn.value}"

  override def irAgentReference: String = "IRSA-123"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  def doRequest(): WSResponse = Await.result(
    wsClient
      .url(url)
      .withHttpHeaders("Authorization" -> "Bearer XYZ")
      .get(),
    15.seconds
  )

  def doPostRequest[T](body: T)(using writes: BodyWritable[T]): WSResponse = Await.result(
    wsClient
      .url(url)
      .withHttpHeaders("Authorization" -> "Bearer XYZ", CONTENT_TYPE -> "application/json")
      .post(body),
    15.seconds
  )

  def agentDetails(countryCode: String = "GB") = AgencyDetails(
    Some("My Agency"),
    Some("abc@abc.com"),
    Some("07345678901"),
    Some(BusinessAddress(
      "25 Any Street",
      Some("Central Grange"),
      Some("Telford"),
      None,
      Some("TF4 3TR"),
      countryCode
    ))
  )

  val testUtr: Utr = Utr("7000000002")
  val membershipExpiresOnDate: LocalDate = LocalDate.now.plusWeeks(4)
  val testAmlsDetails: UkAmlsDetails = UkAmlsDetails(
    "supervisory",
    membershipNumber = Some("0123456789"),
    appliedOn = None,
    membershipExpiresOn = Some(membershipExpiresOnDate)
  )
  val testOverseasAmlsDetails: OverseasAmlsDetails = OverseasAmlsDetails("supervisory", membershipNumber = Some("0123456789"))
  val testOverseasAmlsEntity: OverseasAmlsEntity = OverseasAmlsEntity(
    arn,
    testOverseasAmlsDetails,
    None
  )

  val testCreatedDate: LocalDate = LocalDate.now.plusWeeks(2)
  val amlsEntity: UkAmlsEntity = UkAmlsEntity(
    utr = Some(testUtr),
    amlsDetails = testAmlsDetails,
    arn = Some(arn),
    createdOn = testCreatedDate
  )

  "GET /amls/arn/:arn" should {
    s"return OK with status NoAmlsDetailsUK when no AMLS records found for the ARN" in {
      isLoggedInAsStride("stride")
      givenASAGetAgentRecord(arn, testAgentDetailsDesAddressUtrResponse)
      val response = doRequest()
      response.status mustBe OK
      response.json mustBe Json.obj("status" -> "NoAmlsDetailsUK")
    }

    s"return OK with status NoAmlsDetailsNonUK when no AMLS records found for the ARN" in {
      isLoggedInAsStride("stride")
      givenASAGetAgentRecord(arn, testAgentDetailsDesOverseas)
      val response = doRequest()
      response.status mustBe OK
      response.json mustBe Json.obj("status" -> "NoAmlsDetailsNonUK")
    }

    s"return OK with status when UK AMLS records found for the ARN through agent record" in {
      isLoggedInAsStride("stride")
      givenASAGetAgentRecord(
        arn,
        testAgentDetailsDesAddressUtrResponse.copy(
          amlsDetails = Some(
            AgentRecordAmlsDetails(
              supervisoryBody = "supervisory",
              membershipNumber = "0123456789"
            )
          )
        )
      )

      val response = doRequest()
      response.status mustBe OK
      response.json mustBe Json.obj(
        "status" -> "ValidAmlsDetailsUK",
        "details" -> Json.obj(
          "supervisoryBody" -> "supervisory",
          "membershipNumber" -> "0123456789"
        )
      )
    }

    s"return OK with status when UK AMLS records found for the ARN through amls repository" in {
      isLoggedInAsStride("stride")
      givenASAGetAgentRecord(arn, testAgentDetailsDesAddressUtrResponse)
      ukAmlsRepository.collection.insertOne(amlsEntity).toFuture().futureValue
      val response = doRequest()
      response.status mustBe OK
      response.json mustBe Json.obj(
        "status" -> "ValidAmlsDetailsUK",
        "details" -> Json.obj(
          "supervisoryBody" -> "supervisory",
          "membershipNumber" -> "0123456789",
          "membershipExpiresOn" -> "2026-09-02"
        )
      )
    }

    s"return OK with status when overseas AMLS details found for the ARN through agent record" in {
      isLoggedInAsStride("stride")
      givenASAGetAgentRecord(
        arn,
        testAgentDetailsDesOverseas.copy(
          amlsDetails = Some(
            AgentRecordAmlsDetails(
              supervisoryBody = "supervisory",
              membershipNumber = "0123456789"
            )
          )
        )
      )
      val response = doRequest()
      response.status mustBe OK
      response.json mustBe Json.obj(
        "status" -> "ValidAmlsNonUK",
        "details" -> Json.obj("supervisoryBody" -> "supervisory", "membershipNumber" -> "0123456789")
      )
    }

    s"return OK with status when overseas AMLS details found for the ARN through overseas amls repository" in {
      isLoggedInAsStride("stride")
      givenASAGetAgentRecord(arn, testAgentDetailsDesOverseas)
      overseasAmlsRepository.collection.insertOne(testOverseasAmlsEntity).toFuture().futureValue
      val response = doRequest()
      response.status mustBe OK
      response.json mustBe Json.obj(
        "status" -> "ValidAmlsNonUK",
        "details" -> Json.obj("supervisoryBody" -> "supervisory", "membershipNumber" -> "0123456789")
      )
    }

    "return INTERNAL_SERVER_ERROR when overseas and UK AMLS records found for the ARN" in {
      isLoggedInAsStride("stride")
      givenASAGetAgentRecord(arn, testAgentDetailsDesOverseas)
      overseasAmlsRepository.collection.insertOne(testOverseasAmlsEntity).toFuture().futureValue
      ukAmlsRepository.collection.insertOne(amlsEntity).toFuture().futureValue
      val response = doRequest()
      response.status mustBe INTERNAL_SERVER_ERROR
    }
  }

  "POST /amls/arn/:arn" should {
    "return CREATED for UK AMLS" when {
      "UK record is deleted from repository if record exists for the ARN" in {
        isLoggedInAsAnAfinityGroupAgent("agent1")
        givenASAAgentRecordUpdateSuccess()

        ukAmlsRepository.collection.insertOne(amlsEntity).toFuture().futureValue
        ukAmlsRepository.collection.find().toFuture().futureValue.size mustBe 1

        val amlsRequest = AmlsRequest(
          ukRecord = true,
          supervisoryBody = "supervisory",
          membershipNumber = "0123456789",
          membershipExpiresOn = None
        )

        val response = doPostRequest(Json.toJson(amlsRequest))
        response.status mustBe CREATED

        eventually(Timeout(Span(5, Seconds))){
          ukAmlsRepository.collection.find().toFuture().futureValue.size mustBe 0
        }
      }
    }
    "return 201 Created for overseas AMLS" when {
      "UK record is deleted from repository if record exists for the ARN" in {
        isLoggedInAsAnAfinityGroupAgent("agent1")
        givenASAAgentRecordUpdateSuccess()

        overseasAmlsRepository.collection.insertOne(testOverseasAmlsEntity).toFuture().futureValue
        overseasAmlsRepository.collection.find().toFuture().futureValue.size mustBe 1

        val amlsRequest = AmlsRequest(
          ukRecord = false,
          supervisoryBody = "Indian AC",
          membershipNumber = "X243",
          membershipExpiresOn = Some(LocalDate.parse("2024-12-31"))
        )

        val response = doPostRequest(Json.toJson(amlsRequest))
        response.status mustBe CREATED

        overseasAmlsRepository.collection.find().toFuture().futureValue.size mustBe 0

      }

    }
    "return BAD_REQUEST" when {
      "empty body is sent" in {
        isLoggedInAsAnAfinityGroupAgent("agent1")

        val response = doPostRequest("")
        response.status mustBe BAD_REQUEST
        response.body.contains("No JSON found in request") mustBe true
      }
      "invalid JSON sent" in {
        isLoggedInAsAnAfinityGroupAgent("agent1")

        val response = doPostRequest(Json.obj("not" -> "acceptable"))
        response.status mustBe BAD_REQUEST
        response.body.contains("Could not parse JSON body:") mustBe true
      }
    }
  }

}
