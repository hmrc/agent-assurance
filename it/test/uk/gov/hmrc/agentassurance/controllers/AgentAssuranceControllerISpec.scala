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

import java.time.Clock
import java.time.LocalDate

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

import com.google.inject.AbstractModule
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.CONTENT_TYPE
import play.api.Application
import test.uk.gov.hmrc.agentassurance.stubs.DesStubs
import test.uk.gov.hmrc.agentassurance.stubs.EnrolmentStoreProxyStubs
import test.uk.gov.hmrc.agentassurance.support.AgentAuthStubs
import test.uk.gov.hmrc.agentassurance.support.IntegrationSpec
import test.uk.gov.hmrc.agentassurance.support.WireMockSupport
import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.agentassurance.repositories.AmlsRepositoryImpl
import uk.gov.hmrc.agentassurance.models.Arn
import uk.gov.hmrc.agentassurance.models.Utr
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

class AgentAssuranceControllerISpec
extends IntegrationSpec
with GuiceOneServerPerSuite
with AgentAuthStubs
with DesStubs
with WireMockSupport
with EnrolmentStoreProxyStubs
with DefaultPlayMongoRepositorySupport[UkAmlsEntity] {

  override implicit lazy val app: Application = appBuilder.build()

  override lazy val repository = new AmlsRepositoryImpl(mongoComponent)

  val moduleWithOverrides: AbstractModule =
    new AbstractModule() {
      override def configure(): Unit = {
        bind(classOf[AmlsRepositoryImpl]).toInstance(repository)
        bind(classOf[Clock]).toInstance(clock)
      }
    }

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.host" -> wireMockHost,
      "auditing.enabled" -> false,
      "microservice.services.auth.port" -> wireMockPort,
      "microservice.services.des.host" -> wireMockHost,
      "microservice.services.des.port" -> wireMockPort,
      "microservice.services.des.environment" -> "test",
      "microservice.services.des.authorization-token" -> "secret",
      "microservice.services.enrolment-store-proxy.host" -> wireMockHost,
      "microservice.services.enrolment-store-proxy.port" -> wireMockPort,
      "minimumIRPAYEClients" -> 6,
      "minimumIRSAClients" -> 6,
      "minimumVatDecOrgClients" -> 6,
      "minimumIRCTClients" -> 6,
      "stride.roles.agent-assurance" -> "maintain_agent_manually_assure",
      "internal-auth-token-enabled-on-start" -> false,
      "http-verbs.retries.intervals" -> List("1ms"),
      "agent.cache.enabled" -> true
    )
    .overrides(moduleWithOverrides)

  implicit val hc: HeaderCarrier = new HeaderCarrier

  val irSaAgentEnrolmentUrl = s"http://localhost:$port/agent-assurance/irSaAgentEnrolment"

  def irSaAgentEnrolmentNinoUrl(nino: String) = s"http://localhost:$port/agent-assurance/activeCesaRelationship/nino/$nino/saAgentReference/$irAgentReference"

  def irSaAgentEnrolmentUtrUrl(utr: String) = s"http://localhost:$port/agent-assurance/activeCesaRelationship/utr/$utr/saAgentReference/$irAgentReference"

  val irPayeKey = "IR-PAYE"
  val irSaKey = "IR-SA"
  val vatDecOrgKey = "HMCE-VATDEC-ORG"
  val irCtKey = "IR-CT"
  val acceptableNumberOfPayeClientsUrl = s"http://localhost:$port/agent-assurance/acceptableNumberOfClients/service/$irPayeKey"
  val acceptableNumberOfSAClientsUrl = s"http://localhost:$port/agent-assurance/acceptableNumberOfClients/service/$irSaKey"
  val acceptableNumberOfVatDevOrgClientsUrl = s"http://localhost:$port/agent-assurance/acceptableNumberOfClients/service/$vatDecOrgKey"
  val acceptableNumberOfIRCTClientsUrl = s"http://localhost:$port/agent-assurance/acceptableNumberOfClients/service/$irCtKey"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val userId = "0000001531072644"

  implicit val defaultTimeout: Duration = 5 seconds

  def await[A](future: Future[A])(implicit timeout: Duration) = Await.result(future, timeout)

  override def irAgentReference: String = "IRSA-123"

  Feature("/irSaAgentEnrolment") {
    Scenario("User is enrolled in IR_SA_AGENT") {
      Given("User is enrolled in IR_SA_AGENT")
      isLoggedInAndIsEnrolledToIrSaAgent

      When("GET /irSaAgentEnrolment is called")
      val response: WSResponse = Await.result(
        wsClient
          .url(irSaAgentEnrolmentUrl)
          .withHttpHeaders(("Authorization", "Bearer XYZ"))
          .get(),
        10 seconds
      )

      Then("204 NO_CONTENT is returned")
      response.status shouldBe 204
    }

    Scenario("User is not enrolled in IR_SA_AGENT") {
      Given("User is not enrolled in IR_SA_AGENT")
      isLoggedInAndNotEnrolledInIrSaAgent

      When("GET /irSaAgentEnrolment is called")
      val response: WSResponse = Await.result(
        wsClient
          .url(irSaAgentEnrolmentUrl)
          .withHttpHeaders(("Authorization", "Bearer XYZ"))
          .get(),
        10 seconds
      )

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    Scenario("User is not logged in") {
      Given("User is not logged in")
      isNotLoggedIn

      When("GET /irSaAgentEnrolment is called")
      val response: WSResponse = Await.result(
        wsClient
          .url(irSaAgentEnrolmentUrl)
          .withHttpHeaders(("Authorization", "Bearer XYZ"))
          .get(),
        10 seconds
      )

      Then("401 UNAUTHORISED is returned")
      response.status shouldBe 401
    }
  }

  Feature("/activeCesaRelationship/nino/:nino/saAgentReference/:saAgentReference") {
    Scenario("User provides a NINO which has an active relationship in CESA") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("CESA contains an active agent/client relationship for NINO AA000000A and Agent Reference IRSA-123")
      givenClientHasRelationshipWithAgentInCESA(Nino("AA000000A"), SaAgentReference(irAgentReference))

      When("GET /activeCesaRelationship/nino/AA000000A/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(
        wsClient
          .url(irSaAgentEnrolmentNinoUrl("AA000000A"))
          .withHttpHeaders(("Authorization", "Bearer XYZ"))
          .get(),
        10 seconds
      )

      Then("200 OK is returned")
      response.status shouldBe 200
    }

    Scenario("User provides a NINO which has no active relationship in CESA") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("CESA contains no active agent/client relationship for NINO AA000000A and Agent Reference IRSA-123")
      givenClientHasNoActiveRelationshipWithAgentInCESA(Nino("AA000000A"))

      When("GET /activeCesaRelationship/nino/AA000000A/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(
        wsClient
          .url(irSaAgentEnrolmentNinoUrl("AA000000A"))
          .withHttpHeaders(("Authorization", "Bearer XYZ"))
          .get(),
        10 seconds
      )

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    Scenario("User provides a NINO which has an active relationship in CESA but with a different Agent Reference") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("CESA contains no active agent/client relationship for NINO AA000000A and Agent Reference IRSA-456")
      givenClientHasRelationshipWithAgentInCESA(Nino("AA000000A"), SaAgentReference("IRSA-456"))

      When("GET /activeCesaRelationship/nino/AA000000A/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(
        wsClient
          .url(irSaAgentEnrolmentNinoUrl("AA000000A"))
          .withHttpHeaders(("Authorization", "Bearer XYZ"))
          .get(),
        10 seconds
      )

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    Scenario("User provides an invalid NINO") {
      When("GET /activeCesaRelationship/nino/AA000000/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(
        wsClient
          .url(irSaAgentEnrolmentNinoUrl("INVALID"))
          .withHttpHeaders(("Authorization", "Bearer XYZ"))
          .get(),
        10 seconds
      )

      Then("400 BADREQUEST is returned")
      response.status shouldBe 400
    }

    Scenario("DES return 500 server error when user calls the endpoint") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("DES return 500 server error")
      givenDesReturnsServerError()

      When("GET /activeCesaRelationship/nino/AA000000A/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(
        wsClient
          .url(irSaAgentEnrolmentNinoUrl("AA000000A"))
          .withHttpHeaders(("Authorization", "Bearer XYZ"))
          .get(),
        10 seconds
      )

      Then("error GET legacy relationship response: 500")
      response.status shouldBe 500
    }

    Scenario("DES return 502 server error when user calls the endpoint") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("DES return 500 server error")
      givenDesReturnBadGateway()

      When("GET /activeCesaRelationship/nino/AA000000A/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(
        wsClient
          .url(irSaAgentEnrolmentNinoUrl("AA000000A"))
          .withHttpHeaders(("Authorization", "Bearer XYZ"))
          .get(),
        10 seconds
      )

      Then("502 BadGateway is returned")
      response.status shouldBe 502

    }

    Scenario("User is not logged in") {
      Given("User is not logged in")
      isNotLoggedIn

      When("GET /activeCesaRelationship/nino/AA000000A/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(
        wsClient
          .url(irSaAgentEnrolmentNinoUrl("AA000000A"))
          .withHttpHeaders(("Authorization", "Bearer XYZ"))
          .get(),
        10 seconds
      )

      Then("401 Unauthorised is returned")
      response.status shouldBe 401
    }
  }

  Feature("/activeCesaRelationship/utr/:utr/saAgentReference/:saAgentReference") {
    Scenario("User provides a UTR which has an active relationship in CESA") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("CESA contains an active agent/client relationship for UTR 7000000002 and Agent Reference IRSA-123")
      givenClientHasRelationshipWithAgentInCESA(Utr("7000000002"), SaAgentReference(irAgentReference))

      When("GET /activeCesaRelationship/utr/7000000002/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(
        wsClient
          .url(irSaAgentEnrolmentUtrUrl("7000000002"))
          .withHttpHeaders(("Authorization", "Bearer XYZ"))
          .get(),
        10 seconds
      )

      Then("200 OK is returned")
      response.status shouldBe 200
    }

    Scenario("User provides a UTR which has no active relationship in CESA") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("CESA contains no active agent/client relationship for UTR 7000000002 and Agent Reference IRSA-123")
      givenClientHasNoActiveRelationshipWithAgentInCESA(Utr("7000000002"))

      When("GET /activeCesaRelationship/utr/7000000002/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(
        wsClient
          .url(irSaAgentEnrolmentUtrUrl("7000000002"))
          .withHttpHeaders(("Authorization", "Bearer XYZ"))
          .get(),
        10 seconds
      )

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    Scenario("User provides a UTR which has an active relationship in CESA but with a different Agent Reference") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("CESA contains no active agent/client relationship for UTR 7000000002 and Agent Reference IRSA-456")
      givenClientHasRelationshipWithAgentInCESA(Utr("7000000002"), SaAgentReference("IRSA-456"))

      When("GET /activeCesaRelationship/utr/7000000002/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(
        wsClient
          .url(irSaAgentEnrolmentUtrUrl("7000000002"))
          .withHttpHeaders(("Authorization", "Bearer XYZ"))
          .get(),
        10 seconds
      )

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    Scenario("User provides an invalid UTR") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("CESA checks for active relationships with an invalid UTR")
      givenClientIdentifierIsInvalid(Utr("INVALID"))

      When("GET /activeCesaRelationship/utr/INVALID/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(
        wsClient
          .url(irSaAgentEnrolmentUtrUrl("INVALID"))
          .withHttpHeaders(("Authorization", "Bearer XYZ"))
          .get(),
        10 seconds
      )

      Then("400 BADREQUEST is returned")
      response.status shouldBe 400
    }

    Scenario("DES return 500 server error when user calls the endpoint") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("DES return 500 server error")
      givenDesReturnsServerError()

      When("GET /activeCesaRelationship/utr/7000000002/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(
        wsClient
          .url(irSaAgentEnrolmentUtrUrl("7000000002"))
          .withHttpHeaders(("Authorization", "Bearer XYZ"))
          .get(),
        10 seconds
      )

      Then("error GET legacy relationship response: 500")
      response.status shouldBe 500
    }

    Scenario("DES return 502 server error when user calls the endpoint") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("DES return 500 server error")
      givenDesReturnBadGateway()

      When("GET /activeCesaRelationship/utr/7000000002/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(
        wsClient
          .url(irSaAgentEnrolmentUtrUrl("7000000002"))
          .withHttpHeaders(("Authorization", "Bearer XYZ"))
          .get(),
        10 seconds
      )

      Then("502 BadGateway is returned")
      response.status shouldBe 502

    }

    Scenario("User is not logged") {
      Given("User is logged in")
      isNotLoggedIn

      When("GET /activeCesaRelationship/utr/7000000002/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(
        wsClient
          .url(irSaAgentEnrolmentUtrUrl("7000000002"))
          .withHttpHeaders(("Authorization", "Bearer XYZ"))
          .get(),
        10 seconds
      )

      Then("401 Unauthorised is returned")
      response.status shouldBe 401
    }
  }

  // "acceptableNumberOfClients endpoints tests"
  delegatedEnrolmentClientCheck(irPayeKey, acceptableNumberOfPayeClientsUrl)
  delegatedEnrolmentClientCheck(irSaKey, acceptableNumberOfSAClientsUrl)
  delegatedEnrolmentClientCheck(vatDecOrgKey, acceptableNumberOfVatDevOrgClientsUrl)
  delegatedEnrolmentClientCheck(irCtKey, acceptableNumberOfIRCTClientsUrl)

  Feature("/amls") {

    val amlsCreateUrl = s"http://localhost:$port/agent-assurance/amls"

    val utr = Utr("7000000002")
    val validApplicationReferenceNumber = "XAML00000123456"
    val amlsDetails = UkAmlsDetails(
      "supervisory",
      membershipNumber = Some("0123456789"),
      appliedOn = None,
      membershipExpiresOn = Some(LocalDate.now())
    )
    val pendingAmlsDetails = UkAmlsDetails(
      "supervisory",
      membershipNumber = Some(validApplicationReferenceNumber),
      appliedOn = Some(LocalDate.now().minusDays(10)),
      membershipExpiresOn = None
    )
    val createAmlsRequest = CreateAmlsRequest(utr, amlsDetails)
    val pendingAmlsDetailsRequest = CreateAmlsRequest(utr, pendingAmlsDetails)

    def doRequest(createAmlsRequest: CreateAmlsRequest) = Await.result(
      wsClient
        .url(amlsCreateUrl)
        .withHttpHeaders(CONTENT_TYPE -> "application/json", "Authorization" -> "Bearer XYZ")
        .post(Json.toJson(createAmlsRequest)),
      10 seconds
    )

    Scenario("user logged in and is an agent should be able to create a new Amls record for the first time") {
      Given("User is logged in and is an agent")
      isLoggedInAsAnAfinityGroupAgent(userId)

      When("POST /amls/create is called")
      val response: WSResponse = doRequest(createAmlsRequest)

      Then("201 CREATED is returned")
      response.status shouldBe 201

      val dbRecord = await(repository.collection.find().toFuture()).head
      dbRecord.utr.get shouldBe utr
      dbRecord.createdOn shouldBe LocalDate.now()
    }

    Scenario("user logged in an as a stride should be able to create a new Amls record for the first time") {

      Given("User is logged in and is stride")
      Given("User has an user id")
      isLoggedInAsStride(userId)

      When("POST /amls/create is called")
      val response: WSResponse = doRequest(createAmlsRequest)

      Then("201 CREATED is returned")
      response.status shouldBe 201

      val dbRecord = await(repository.collection.find().toFuture()).head
      dbRecord.utr.get shouldBe utr
      dbRecord.createdOn shouldBe LocalDate.now()

    }

    Scenario(
      "user logged in and is an agent should be able to create a new Amls Pending Details record for the first time"
    ) {
      Given("User is logged in and is an agent")
      isLoggedInAsAnAfinityGroupAgent(userId)

      When("POST /amls/create is called with PendingAmlsDetails")
      val response: WSResponse = doRequest(pendingAmlsDetailsRequest)

      Then("201 CREATED is returned")
      response.status shouldBe 201

      val dbRecord = await(repository.collection.find().toFuture()).head
      dbRecord.utr.get shouldBe utr
      dbRecord.createdOn shouldBe LocalDate.now()
      dbRecord.amlsDetails shouldBe UkAmlsDetails(
        "supervisory",
        membershipNumber = Some(validApplicationReferenceNumber),
        appliedOn = Some(LocalDate.now().minusDays(10)),
        membershipExpiresOn = None
      )
    }

    Scenario("stride user should be able to create a registered Amls record without a date (APB-5382)") {

      Given("User is logged in and is stride")
      Given("User has an user id")
      isLoggedInAsStride(userId)

      When("POST /amls/create is called with a registered AMLS record that doesn't have an expiry date")
      val amlsDetailsNoDate = amlsDetails.copy(membershipExpiresOn = None)
      val response: WSResponse = doRequest(createAmlsRequest.copy(amlsDetails = amlsDetailsNoDate))

      Then("201 CREATED is returned")
      response.status shouldBe 201

      val dbRecord = await(repository.collection.find().toFuture()).head
      dbRecord.utr.get shouldBe utr
      dbRecord.amlsDetails shouldBe amlsDetailsNoDate
      dbRecord.createdOn shouldBe LocalDate.now()
    }

    Scenario("stride user should be able to create a pending Amls record without a date (APB-5382)") {

      Given("User is logged in and is stride")
      Given("User has an user id")
      isLoggedInAsStride(userId)

      When("POST /amls/create is called with a pending AMLS record that doesn't have an applied date")
      val amlsDetailsNoDate = pendingAmlsDetails.copy(appliedOn = None)
      val response: WSResponse = doRequest(createAmlsRequest.copy(amlsDetails = amlsDetailsNoDate))

      Then("201 CREATED is returned")
      response.status shouldBe 201

      val dbRecord = await(repository.collection.find().toFuture()).head
      dbRecord.utr.get shouldBe utr
      dbRecord.amlsDetails shouldBe amlsDetailsNoDate
      dbRecord.createdOn shouldBe LocalDate.now()
    }

    Scenario("stride user should be able to create a pending Amls record without a reference number") {

      Given("User is logged in and is stride")
      Given("User has an user id")
      isLoggedInAsStride(userId)

      When("POST /amls/create is called with a pending AMLS record without a reference number")
      val amlsDetailsNoDate = pendingAmlsDetails.copy(membershipNumber = None)
      val response: WSResponse = doRequest(createAmlsRequest.copy(amlsDetails = amlsDetailsNoDate))

      Then("201 CREATED is returned")
      response.status shouldBe 201

      val dbRecord = await(repository.collection.find().toFuture()).head
      dbRecord.utr.get shouldBe utr
      dbRecord.amlsDetails shouldBe amlsDetailsNoDate
      dbRecord.createdOn shouldBe LocalDate.now()
    }

    Scenario("User is not logged in") {
      Given("User is not logged in")
      isNotLoggedIn

      When("POST /amls/create is called")
      val response: WSResponse = doRequest(createAmlsRequest)

      Then("401 UnAuthorized is returned")
      response.status shouldBe 401
    }

    Scenario("update existing amls record no ARN should be allowed") {

      Given("User is logged in and is an agent")
      isLoggedInAsAnAfinityGroupAgent(userId)

      When("POST /amls/create is called")
      val response: WSResponse = doRequest(createAmlsRequest)

      Then("201 CREATED is returned")
      response.status shouldBe 201

      When("POST /amls/create is called second time with the same UTR")
      val newResponse: WSResponse = doRequest(
        createAmlsRequest.copy(amlsDetails = createAmlsRequest.amlsDetails.copy(supervisoryBody = "updated-supervisory"))
      )

      Then("201 CREATED is returned")
      newResponse.status shouldBe 201

      val dbRecord = await(repository.collection.find().toFuture()).head
      dbRecord.utr.get shouldBe utr
      dbRecord.amlsDetails.supervisoryBody shouldBe "updated-supervisory"
    }

    Scenario("return bad_request if UTR is not valid") {

      Given("User is logged in and is an agent")
      isLoggedInAsAnAfinityGroupAgent(userId)

      When("POST /amls/create is called with invalid utr")
      val response: WSResponse = doRequest(createAmlsRequest.copy(utr = Utr("61122334455")))

      Then("400 BAD_REQUEST is returned")
      response.status shouldBe 400
    }
  }

  Feature("GET /amls/utr/:utr") {

    val utr = Utr("7000000002")

    def amlsGetUrl(utr: Utr) = s"http://localhost:$port/agent-assurance/amls/utr/${utr.value}"

    val amlsDetails = UkAmlsDetails(
      "supervisory",
      membershipNumber = Some("0123456789"),
      appliedOn = None,
      membershipExpiresOn = Some(LocalDate.now())
    )

    def callGet(utr: Utr) = Await.result(
      wsClient.url(amlsGetUrl(utr)).withHttpHeaders("Authorization" -> "Bearer XYZ").get(),
      10 seconds
    )

    Scenario("user logged in as an agent should be able to get existing amls record") {

      await(repository.createOrUpdate(CreateAmlsRequest(utr, amlsDetails)))

      Given("User is logged in and is an agent")
      Given("User has an user id")
      isLoggedInAsAnAfinityGroupAgent(userId)

      When("GET /amls/utr/:identifier is called")
      val updateResponse: WSResponse = callGet(utr)

      Then("200 Ok is returned")
      updateResponse.status shouldBe 200

      updateResponse.json shouldBe Json.toJson(amlsDetails)
    }

    Scenario("user logged in an as a stride should be able to get existing amls record") {

      await(repository.createOrUpdate(CreateAmlsRequest(utr, amlsDetails)))

      Given("User is logged in and is stride")
      Given("User has an user id")
      isLoggedInAsStride(userId)

      When("GET /amls/utr/:identifier is called")
      val updateResponse: WSResponse = callGet(utr)

      Then("200 Ok is returned")
      updateResponse.status shouldBe 200

      updateResponse.json shouldBe Json.toJson(amlsDetails)
    }
  }

  Feature("PUT /amls/utr/:utr") {

    val utr = Utr("7000000002")
    val arn = Arn("AARN0000002")

    def amlsUpdateUrl(utr: Utr) = s"http://localhost:$port/agent-assurance/amls/utr/${utr.value}"

    val amlsDetails = UkAmlsDetails(
      "supervisory",
      membershipNumber = Some("0123456789"),
      appliedOn = None,
      membershipExpiresOn = Some(LocalDate.now())
    )

    def callPut(
      utr: Utr,
      arn: Arn
    ) = Await.result(
      wsClient
        .url(amlsUpdateUrl(utr))
        .withHttpHeaders(CONTENT_TYPE -> "application/json", "Authorization" -> "Bearer XYZ")
        .put(Json.toJson(arn).toString()),
      10 seconds
    )

    Scenario("user logged in and is an agent should be able to update existing amls record with ARN") {

      await(repository.createOrUpdate(CreateAmlsRequest(utr, amlsDetails)))

      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      When("PUT /amls/utr/:identifier is called")
      val updateResponse: WSResponse = callPut(utr, arn)

      Then("200 Ok is returned")
      updateResponse.status shouldBe 200

      updateResponse.json shouldBe Json.toJson(amlsDetails)
    }

    Scenario("User is not logged in") {
      Given("User is not logged in")
      isNotLoggedIn

      When("PUT /amls/utr/:identifier is called")
      val response: WSResponse = callPut(utr, arn)

      Then("401 UnAuthorized is returned")
      response.status shouldBe 401
    }

    Scenario("updating an existing amls record(with ARN) with the same ARN again should return existing amls record") {

      await(repository.createOrUpdate(CreateAmlsRequest(utr, amlsDetails)))

      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      When("PUT /amls/utr/:identifier is called second time with the same ARN")
      val newResponse: WSResponse = callPut(utr, arn)

      Then("200 Ok is returned")
      newResponse.status shouldBe 200
      newResponse.json shouldBe Json.toJson(amlsDetails)
    }

    Scenario("updates to an existing amls record(with ARN) with a different ARN should be Forbidden") {

      await(repository.createOrUpdate(CreateAmlsRequest(utr, amlsDetails)))
      await(repository.updateArn(utr, Arn("XX123")))

      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      When("PUT /amls/utr/:identifier is called second time with a different ARN")
      val newResponse: WSResponse = callPut(utr, arn)

      Then("403 Forbidden is returned")
      newResponse.status shouldBe 403
    }

    Scenario("updating ARN for a non-existing amls record should return a bad_request") {

      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      When("PUT /amls/utr/:identifier is called")
      val updateResponse: WSResponse = callPut(utr, arn)

      Then("404 NOT_FOUND is returned")
      updateResponse.status shouldBe 404
    }

    Scenario("ARN to be unique to each UTR in the MDTP database") {

      val newUtr = Utr("8588532862")
      await(repository.ensureIndexes())
      await(
        repository.collection
          .insertOne(
            UkAmlsEntity(
              utr = Some(utr),
              amlsDetails = amlsDetails,
              arn = Some(arn),
              createdOn = LocalDate.now(),
              amlsSource = AmlsSource.Subscription
            )
          )
          .toFuture()
      )
      await(
        repository.collection
          .insertOne(
            UkAmlsEntity(
              utr = Some(newUtr),
              amlsDetails = amlsDetails,
              arn = None,
              createdOn = LocalDate.now(),
              amlsSource = AmlsSource.Subscription
            )
          )
          .toFuture()
      )

      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      When("PUT /amls/utr/:identifier is called with the same ARN but for a different UTR")
      val updateResponse: WSResponse = callPut(newUtr, arn)

      Then("400 BAD_REQUEST is returned")
      updateResponse.status shouldBe 400
    }
  }

  Feature("GET /amls-subscription/:amlsRegistrationNumber") {
    val validAmlsRegistrationNumber = "XAML00000700000"
    def url(amlsRegistrationNumber: String) = s"http://localhost:$port/agent-assurance/amls-subscription/$amlsRegistrationNumber"

    Scenario("return 200 when amlsRegistrationNumber is valid") {

      Given("amlsRegistrationNumber valid")
      amlsSubscriptionRecordExists(validAmlsRegistrationNumber)

      When(s"GET /amls-subscription/$validAmlsRegistrationNumber is called")
      val response: WSResponse = Await.result(wsClient.url(url(validAmlsRegistrationNumber)).get(), 10 seconds)

      Then("200 OK is returned")
      response.status shouldBe 200
    }

    Scenario("return 404 when amlsRegistrationNumber is not found") {

      Given("amlsRegistrationNumber valid")
      amlsSubscriptionRecordFails(validAmlsRegistrationNumber, 404)

      When(s"GET /amls-subscription/$validAmlsRegistrationNumber is called")
      val response: WSResponse = Await.result(wsClient.url(url(validAmlsRegistrationNumber)).get(), 10 seconds)

      Then("404 NOT_FOUND is returned")
      response.status shouldBe 404
    }
  }

  def delegatedEnrolmentClientCheck(
    enrolment: String,
    acceptableClientUrl: String
  ): Unit = {
    Feature(s"/acceptableNumberOfClients/service/$enrolment") {

      Scenario(s"Logged in user is an agent with sufficient allocated $enrolment clients") {
        Given("User has an user id")
        isLoggedInWithUserId(userId)

        And(s"Enrolment store returns sufficient allocated $enrolment clients for the agent")
        sufficientClientsAreAllocated(enrolment, userId)

        When(s"GET /acceptableNumberOfClients/service/$enrolment is called")
        val response: WSResponse = Await.result(
          wsClient
            .url(acceptableClientUrl)
            .withHttpHeaders(("Authorization", "Bearer XYZ"))
            .get(),
          10 seconds
        )

        Then("204 NO_CONTENT is returned")
        response.status shouldBe 204
      }

      Scenario(s"Logged in user is an agent with insufficient allocated $enrolment clients") {
        Given("User has an user id")
        isLoggedInWithUserId(userId)

        And(s"Enrolment store returns insufficient allocated $enrolment clients for the agent")
        tooFewClientsAreAllocated(enrolment, userId)

        When(s"GET /acceptableNumberOfClients/service/$enrolment is called")
        val response: WSResponse = Await.result(
          wsClient
            .url(acceptableClientUrl)
            .withHttpHeaders(("Authorization", "Bearer XYZ"))
            .get(),
          10 seconds
        )

        Then("403 FORBIDDEN is returned")
        response.status shouldBe 403
      }

      Scenario(s"Logged in user is an agent with no allocated $enrolment clients") {
        Given("User has an user id")
        isLoggedInWithUserId(userId)

        And(s"Enrolment store returns no allocated $enrolment clients for the agent")
        noClientsAreAllocated(enrolment, userId)

        When(s"GET /acceptableNumberOfClients/service/$enrolment is called")
        val response: WSResponse = Await.result(
          wsClient
            .url(acceptableClientUrl)
            .withHttpHeaders(("Authorization", "Bearer XYZ"))
            .get(),
          10 seconds
        )

        Then("403 FORBIDDEN is returned")
        response.status shouldBe 403
      }

      Scenario("Logged in user is not an agent") {
        Given("User has no user id")
        isLoggedInWithoutUserId

        When(s"GET /acceptableNumberOfClients/service/$enrolment is called")
        val response: WSResponse = Await.result(
          wsClient
            .url(acceptableClientUrl)
            .withHttpHeaders(("Authorization", "Bearer XYZ"))
            .get(),
          10 seconds
        )

        Then("403 FORBIDDEN is returned")
        response.status shouldBe 403
      }

      Scenario(s"User is not logged in when $enrolment") {
        Given("User is not logged in")
        isNotLoggedIn

        When(s"GET /acceptableNumberOfClients/service/$enrolment is called")
        val response: WSResponse = Await.result(
          wsClient
            .url(acceptableClientUrl)
            .withHttpHeaders(("Authorization", "Bearer XYZ"))
            .get(),
          10 seconds
        )

        Then("401 UNAUTHORISED is returned")
        response.status shouldBe 401
      }
    }
  }

}
