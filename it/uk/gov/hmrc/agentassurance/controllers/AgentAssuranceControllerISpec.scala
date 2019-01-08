package uk.gov.hmrc.agentassurance.controllers

import java.time.LocalDate

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.CONTENT_TYPE
import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.agentassurance.repositories.{AmlsRepositoryImpl, OverseasAmlsRepositoryImpl}
import uk.gov.hmrc.agentassurance.stubs.{DesStubs, EnrolmentStoreProxyStubs}
import uk.gov.hmrc.agentassurance.support.{AgentAuthStubs, IntegrationSpec, WireMockSupport}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.domain.{Nino, SaAgentReference}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class AgentAssuranceControllerISpec extends IntegrationSpec
  with GuiceOneServerPerSuite with AgentAuthStubs with DesStubs with WireMockSupport with EnrolmentStoreProxyStubs {
  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure("microservice.services.auth.port" -> wireMockPort,
        "microservice.services.des.port" -> wireMockPort,
        "microservice.services.enrolment-store-proxy.port" -> wireMockPort,
        "minimumIRPAYEClients" -> 6,
        "minimumIRSAClients" -> 6,
        "minimumVatDecOrgClients" -> 6,
        "minimumIRCTClients" -> 6)

  implicit val hc = new HeaderCarrier

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

  private lazy val repo = app.injector.instanceOf[AmlsRepositoryImpl]
  private lazy val overseasAmlsRepo = app.injector.instanceOf[OverseasAmlsRepositoryImpl]

  implicit val defaultTimeout = 5 seconds

  def await[A](future: Future[A])(implicit timeout: Duration) = Await.result(future, timeout)

  override def beforeEach() {
    super.beforeEach()
    await(repo.drop)
    await(overseasAmlsRepo.drop)
  }


  override def irAgentReference: String = "IRSA-123"

  feature("/irSaAgentEnrolment") {
    scenario("User is enrolled in IR_SA_AGENT") {
      Given("User is enrolled in IR_SA_AGENT")
      isLoggedInAndIsEnrolledToIrSaAgent

      When("GET /irSaAgentEnrolment is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentUrl).get(), 10 seconds)

      Then("204 NO_CONTENT is returned")
      response.status shouldBe 204
    }

    scenario("User is not enrolled in IR_SA_AGENT") {
      Given("User is not enrolled in IR_SA_AGENT")
      isLoggedInAndNotEnrolledInIrSaAgent

      When("GET /irSaAgentEnrolment is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentUrl).get(), 10 seconds)

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    scenario("User is not logged in") {
      Given("User is not logged in")
      isNotLoggedIn

      When("GET /irSaAgentEnrolment is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentUrl).get(), 10 seconds)

      Then("401 UNAUTHORISED is returned")
      response.status shouldBe 401
    }
  }

  feature("/activeCesaRelationship/nino/:nino/saAgentReference/:saAgentReference") {
    scenario("User provides a NINO which has an active relationship in CESA") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("CESA contains an active agent/client relationship for NINO AA000000A and Agent Reference IRSA-123")
      givenClientHasRelationshipWithAgentInCESA(Nino("AA000000A"), SaAgentReference(irAgentReference))

      When("GET /activeCesaRelationship/nino/AA000000A/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentNinoUrl("AA000000A")).get(), 10 seconds)

      Then("200 OK is returned")
      response.status shouldBe 200
    }

    scenario("User provides a NINO which has no active relationship in CESA") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("CESA contains no active agent/client relationship for NINO AA000000A and Agent Reference IRSA-123")
      givenClientHasNoActiveRelationshipWithAgentInCESA(Nino("AA000000A"))

      When("GET /activeCesaRelationship/nino/AA000000A/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentNinoUrl("AA000000A")).get(), 10 seconds)

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    scenario("User provides a NINO which has an active relationship in CESA but with a different Agent Reference") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("CESA contains no active agent/client relationship for NINO AA000000A and Agent Reference IRSA-456")
      givenClientHasRelationshipWithAgentInCESA(Nino("AA000000A"), SaAgentReference("IRSA-456"))

      When("GET /activeCesaRelationship/nino/AA000000A/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentNinoUrl("AA000000A")).get(), 10 seconds)

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    scenario("User provides an invalid NINO") {
      When("GET /activeCesaRelationship/nino/AA000000/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentNinoUrl("INVALID")).get(), 10 seconds)

      Then("400 BADREQUEST is returned")
      response.status shouldBe 400
    }

    scenario("DES return 500 server error when user calls the endpoint") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("DES return 500 server error")
      givenDesReturnsServerError()

      When("GET /activeCesaRelationship/nino/AA000000A/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentNinoUrl("AA000000A")).get(), 10 seconds)

      Then("502 BadGateway is returned")
      response.status shouldBe 502
    }

    scenario("User is not logged in") {
      Given("User is not logged in")
      isNotLoggedIn

      When("GET /activeCesaRelationship/nino/AA000000A/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentNinoUrl("AA000000A")).get(), 10 seconds)

      Then("401 Unauthorised is returned")
      response.status shouldBe 401
    }
  }

  feature("/activeCesaRelationship/utr/:utr/saAgentReference/:saAgentReference") {
    scenario("User provides a UTR which has an active relationship in CESA") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("CESA contains an active agent/client relationship for UTR 7000000002 and Agent Reference IRSA-123")
      givenClientHasRelationshipWithAgentInCESA(Utr("7000000002"), SaAgentReference(irAgentReference))

      When("GET /activeCesaRelationship/utr/7000000002/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentUtrUrl("7000000002")).get(), 10 seconds)

      Then("200 OK is returned")
      response.status shouldBe 200
    }

    scenario("User provides a UTR which has no active relationship in CESA") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("CESA contains no active agent/client relationship for UTR 7000000002 and Agent Reference IRSA-123")
      givenClientHasNoActiveRelationshipWithAgentInCESA(Utr("7000000002"))

      When("GET /activeCesaRelationship/utr/7000000002/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentUtrUrl("7000000002")).get(), 10 seconds)

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    scenario("User provides a UTR which has an active relationship in CESA but with a different Agent Reference") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("CESA contains no active agent/client relationship for UTR 7000000002 and Agent Reference IRSA-456")
      givenClientHasRelationshipWithAgentInCESA(Utr("7000000002"), SaAgentReference("IRSA-456"))

      When("GET /activeCesaRelationship/utr/7000000002/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentUtrUrl("7000000002")).get(), 10 seconds)

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    scenario("User provides an invalid UTR") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("CESA checks for active relationships with an invalid UTR")
      givenClientIdentifierIsInvalid(Utr("INVALID"))

      When("GET /activeCesaRelationship/utr/INVALID/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentUtrUrl("INVALID")).get(), 10 seconds)

      Then("400 BADREQUEST is returned")
      response.status shouldBe 400
    }

    scenario("DES return 500 server error when user calls the endpoint") {
      Given("User is logged in")
      isLoggedInWithoutUserId

      And("DES return 500 server error")
      givenDesReturnsServerError()

      When("GET /activeCesaRelationship/utr/7000000002/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentUtrUrl("7000000002")).get(), 10 seconds)

      Then("502 BadGateway is returned")
      response.status shouldBe 502
    }

    scenario("User is not logged") {
      Given("User is logged in")
      isNotLoggedIn

      When("GET /activeCesaRelationship/utr/7000000002/saAgentReference/IRSA-123 is called")
      val response: WSResponse = Await.result(wsClient.url(irSaAgentEnrolmentUtrUrl("7000000002")).get(), 10 seconds)

      Then("401 Unauthorised is returned")
      response.status shouldBe 401
    }
  }

  // "acceptableNumberOfClients endpoints tests"
  delegatedEnrolmentClientCheck(irPayeKey, acceptableNumberOfPayeClientsUrl)
  delegatedEnrolmentClientCheck(irSaKey, acceptableNumberOfSAClientsUrl)
  delegatedEnrolmentClientCheck(vatDecOrgKey, acceptableNumberOfVatDevOrgClientsUrl)
  delegatedEnrolmentClientCheck(irCtKey, acceptableNumberOfIRCTClientsUrl)

  feature("/amls") {

    val amlsCreateUrl = s"http://localhost:$port/agent-assurance/amls"

    val utr = Utr("7000000002")
    val amlsDetails = AmlsDetails("supervisory", "0123456789", LocalDate.now())
    val createAmlsRequest = CreateAmlsRequest(utr, amlsDetails)

    def doRequest(createAmlsRequest: CreateAmlsRequest) =
      Await.result(
        wsClient.url(amlsCreateUrl)
          .withHeaders(CONTENT_TYPE -> "application/json")
          .post(Json.toJson(createAmlsRequest)), 10 seconds
      )

    def payload(maybeUtr: Option[Utr] = None,
                supervisory: String = "supervisoryBody",
                arn: Option[Arn] = None) = {
      val utr = maybeUtr.getOrElse(Utr(Random.alphanumeric.take(10).mkString("")))
      val amlsDetails = AmlsDetails(supervisory, "0123456789", LocalDate.now())
      Json.toJson(amlsDetails).toString()
    }

    scenario("user logged in and is an agent should be able to create a new Amls record for the first time") {
      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      When("POST /amls/create is called")
      val response: WSResponse = doRequest(createAmlsRequest)

      Then("201 CREATED is returned")
      response.status shouldBe 201

      val dbRecord = await(repo.find()).head
      dbRecord.utr shouldBe utr
      dbRecord.createdOn shouldBe LocalDate.now()
    }

    scenario("User is not logged in") {
      Given("User is not logged in")
      isNotLoggedIn

      When("POST /amls/create is called")
      val response: WSResponse = doRequest(createAmlsRequest)

      Then("401 UnAuthorized is returned")
      response.status shouldBe 401
    }

    scenario("update existing amls record no ARN should be allowed") {

      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      When("POST /amls/create is called")
      val response: WSResponse = doRequest(createAmlsRequest)

      Then("201 CREATED is returned")
      response.status shouldBe 201

      When("POST /amls/create is called second time with the same UTR")
      val newResponse: WSResponse = doRequest(createAmlsRequest.copy(amlsDetails = createAmlsRequest.amlsDetails.copy(supervisoryBody = "updated-supervisory")))

      Then("201 CREATED is returned")
      newResponse.status shouldBe 201

      val dbRecord = await(repo.find()).head
      dbRecord.utr shouldBe utr
      dbRecord.amlsDetails.supervisoryBody shouldBe "updated-supervisory"
    }

    scenario("return bad_request if UTR is not valid") {

      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      When("POST /amls/create is called with invalid utr")
      val response: WSResponse = doRequest(createAmlsRequest.copy(utr = Utr("61122334455")))

      Then("400 BAD_REQUEST is returned")
      response.status shouldBe 400
    }
  }

  feature("/amls/utr/:utr") {

    val utr = Utr("7000000002")
    val arn = Arn("AARN0000002")

    def amlsUpdateUrl(utr: Utr) = s"http://localhost:$port/agent-assurance/amls/utr/${utr.value}"

    val amlsDetails = AmlsDetails("supervisory", "0123456789", LocalDate.now())

    def callPut(utr: Utr, arn: Arn) =
      Await.result(
        wsClient.url(amlsUpdateUrl(utr))
          .withHeaders(CONTENT_TYPE -> "application/json")
          .put(Json.toJson(arn).toString()), 10 seconds
      )

    scenario("user logged in and is an agent should be able to update existing amls record with ARN") {

      await(repo.insert(AmlsEntity(utr, amlsDetails, None, LocalDate.now())))

      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      When("PUT /amls/utr/:identifier is called")
      val updateResponse: WSResponse = callPut(utr, arn)

      Then("200 Ok is returned")
      updateResponse.status shouldBe 200

      updateResponse.json shouldBe Json.toJson(amlsDetails)
    }

    scenario("User is not logged in") {
      Given("User is not logged in")
      isNotLoggedIn

      When("PUT /amls/utr/:identifier is called")
      val response: WSResponse = callPut(utr, arn)

      Then("401 UnAuthorized is returned")
      response.status shouldBe 401
    }

    scenario("updating an existing amls record(with ARN) with the same ARN again should return existing amls record") {

      await(repo.insert(AmlsEntity(utr, amlsDetails, Some(arn), LocalDate.now())))

      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      When("PUT /amls/utr/:identifier is called second time with the same ARN")
      val newResponse: WSResponse = callPut(utr, arn)

      Then("200 Ok is returned")
      newResponse.status shouldBe 200
      newResponse.json shouldBe Json.toJson(amlsDetails)
    }

    scenario("updates to an existing amls record(with ARN) with a different ARN should be Forbidden") {

      await(repo.insert(AmlsEntity(utr, amlsDetails, Some(Arn("123")), LocalDate.now())))

      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      When("PUT /amls/utr/:identifier is called second time with a different ARN")
      val newResponse: WSResponse = callPut(utr, arn)

      Then("403 Forbidden is returned")
      newResponse.status shouldBe 403
    }

    scenario("updating ARN for a non-existing amls record should return a bad_request") {

      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      When("PUT /amls/utr/:identifier is called")
      val updateResponse: WSResponse = callPut(utr, arn)

      Then("404 NOT_FOUND is returned")
      updateResponse.status shouldBe 404
    }

    scenario("ARN to be unique to each UTR in the MDTP database") {

      val newUtr = Utr("8588532862")
      await(repo.ensureIndexes)
      await(repo.insert(AmlsEntity(utr, amlsDetails, Some(arn), LocalDate.now())))
      await(repo.insert(AmlsEntity(newUtr, amlsDetails, None, LocalDate.now())))

      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      When("PUT /amls/utr/:identifier is called with the same ARN but for a different UTR")
      val updateResponse: WSResponse = callPut(newUtr, arn)

      Then("400 BAD_REQUEST is returned")
      updateResponse.status shouldBe 400
    }
  }

  feature("/overseas-agents/amls") {
    val amlsCreateUrl = s"http://localhost:$port/agent-assurance/overseas-agents/amls"

    val arn = Arn("AARN0000002")

    val amlsDetails = OverseasAmlsDetails("supervisory", Some("0123456789"))
    val createOverseasAmlsRequest = OverseasAmlsEntity(arn, amlsDetails)

    def doRequest(request: OverseasAmlsEntity) =
      Await.result(
        wsClient.url(amlsCreateUrl)
          .withHeaders(CONTENT_TYPE -> "application/json")
          .post(Json.toJson(request)), 10 seconds
      )

    scenario("user logged in and is an agent should be able to create a new Amls record for the first time") {
      Given("User is logged in and is an overseas agent")
      withAffinityGroupAgent

      When("POST /overseas-agents/amls is called")
      val response: WSResponse = doRequest(createOverseasAmlsRequest)

      Then("201 CREATED is returned")
      response.status shouldBe 201

      val dbRecord = await(overseasAmlsRepo.find()).head
      dbRecord.arn shouldBe arn
    }

    scenario("user logged in and is an agent should be able to create a new Amls record without membershipId") {
      Given("User is logged in and is an overseas agent")
      withAffinityGroupAgent

      When("POST /overseas-agents/amls is called")
      val response: WSResponse = doRequest(createOverseasAmlsRequest.copy(amlsDetails = OverseasAmlsDetails("AOSS", None)))

      Then("201 CREATED is returned")
      response.status shouldBe 201

      val dbRecord = await(overseasAmlsRepo.find()).head
      dbRecord.arn shouldBe arn
      dbRecord.amlsDetails.membershipNumber shouldBe None
    }

    scenario("User is not logged in") {
      Given("User is not logged in")
      isNotLoggedIn

      When("POST /overseas-agents/amls is called")
      val response: WSResponse = doRequest(createOverseasAmlsRequest)

      Then("401 UnAuthorized is returned")
      response.status shouldBe 401
    }

    scenario("return bad_request if Arn is not valid") {
      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      When("POST /overseas-agents/amls is called with invalid arn")
      val response: WSResponse = doRequest(createOverseasAmlsRequest.copy(arn = Arn("61122334455")))

      Then("400 BAD_REQUEST is returned")
      response.status shouldBe 400
    }

    scenario("return conflict if the amls record exists") {
      Given("User is logged in and is an overseas agent")
      withAffinityGroupAgent

      When("POST /overseas-agents/amls is called")
      val response: WSResponse = doRequest(createOverseasAmlsRequest)

      Then("201 CREATED is returned")
      response.status shouldBe 201

      When("POST /overseas-agents/amls is called again")
      val response2: WSResponse = doRequest(createOverseasAmlsRequest)

      Then("409 CONFLICT is returned")
      response2.status shouldBe 409

      await(overseasAmlsRepo.find()).size shouldBe 1
    }
  }

  def delegatedEnrolmentClientCheck(enrolment: String, acceptableClientUrl: String): Unit = {
    feature(s"/acceptableNumberOfClients/service/$enrolment") {

      scenario(s"Logged in user is an agent with sufficient allocated $enrolment clients") {
        Given("User has an user id")
        isLoggedInWithUserId(userId)

        And(s"Enrolment store returns sufficient allocated $enrolment clients for the agent")
        sufficientClientsAreAllocated(enrolment, userId)

        When(s"GET /acceptableNumberOfClients/service/$enrolment is called")
        val response: WSResponse = Await.result(wsClient.url(acceptableClientUrl).get(), 10 seconds)

        Then("204 NO_CONTENT is returned")
        response.status shouldBe 204
      }

      scenario(s"Logged in user is an agent with insufficient allocated $enrolment clients") {
        Given("User has an user id")
        isLoggedInWithUserId(userId)

        And(s"Enrolment store returns insufficient allocated $enrolment clients for the agent")
        tooFewClientsAreAllocated(enrolment, userId)

        When(s"GET /acceptableNumberOfClients/service/$enrolment is called")
        val response: WSResponse = Await.result(wsClient.url(acceptableClientUrl).get(), 10 seconds)

        Then("403 FORBIDDEN is returned")
        response.status shouldBe 403
      }

      scenario(s"Logged in user is an agent with no allocated $enrolment clients") {
        Given("User has an user id")
        isLoggedInWithUserId(userId)

        And(s"Enrolment store returns no allocated $enrolment clients for the agent")
        noClientsAreAllocated(enrolment, userId)

        When(s"GET /acceptableNumberOfClients/service/$enrolment is called")
        val response: WSResponse = Await.result(wsClient.url(acceptableClientUrl).get(), 10 seconds)

        Then("403 FORBIDDEN is returned")
        response.status shouldBe 403
      }

      scenario("Logged in user is not an agent") {
        Given("User has no user id")
        isLoggedInWithoutUserId

        When(s"GET /acceptableNumberOfClients/service/$enrolment is called")
        val response: WSResponse = Await.result(wsClient.url(acceptableClientUrl).get(), 10 seconds)

        Then("403 FORBIDDEN is returned")
        response.status shouldBe 403
      }

      scenario(s"User is not logged in when $enrolment") {
        Given("User is not logged in")
        isNotLoggedIn

        When(s"GET /acceptableNumberOfClients/service/$enrolment is called")
        val response: WSResponse = Await.result(wsClient.url(acceptableClientUrl).get(), 10 seconds)

        Then("401 UNAUTHORISED is returned")
        response.status shouldBe 401
      }
    }
  }

}
