package uk.gov.hmrc.agentassurance.controllers

import java.time.LocalDate

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.CONTENT_TYPE
import uk.gov.hmrc.agentassurance.models.AmlsDetails
import uk.gov.hmrc.agentassurance.stubs.{DesStubs, EnrolmentStoreProxyStubs}
import uk.gov.hmrc.agentassurance.support.{AgentAuthStubs, IntegrationSpec, WireMockSupport}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.domain.{Nino, SaAgentReference}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Await
import scala.concurrent.duration._
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
        "minimumIRSAClients" -> 6)

  implicit val hc = new HeaderCarrier

  val irSaAgentEnrolmentUrl = s"http://localhost:$port/agent-assurance/irSaAgentEnrolment"

  def irSaAgentEnrolmentNinoUrl(nino: String) = s"http://localhost:$port/agent-assurance/activeCesaRelationship/nino/$nino/saAgentReference/$irAgentReference"

  def irSaAgentEnrolmentUtrUrl(utr: String) = s"http://localhost:$port/agent-assurance/activeCesaRelationship/utr/$utr/saAgentReference/$irAgentReference"

  val acceptableNumberOfPayeClientsUrl = s"http://localhost:$port/agent-assurance/acceptableNumberOfClients/service/IR-PAYE"
  val acceptableNumberOfSAClientsUrl = s"http://localhost:$port/agent-assurance/acceptableNumberOfClients/service/IR-SA"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val userId = "0000001531072644"


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

  feature("/acceptableNumberOfClients/service/IR-PAYE") {

    scenario("Logged in user is an agent with sufficient allocated PAYE clients") {
      Given("User has an user id")
      isLoggedInWithUserId(userId)

      And("Enrolment store returns sufficient allocated IR-PAYE clients for the agent")
      sufficientClientsAreAllocated("IR-PAYE", userId)

      When("GET /acceptableNumberOfClients/service/IR-PAYE is called")
      val response: WSResponse = Await.result(wsClient.url(acceptableNumberOfPayeClientsUrl).get(), 10 seconds)

      Then("204 NO_CONTENT is returned")
      response.status shouldBe 204
    }

    scenario("Logged in user is an agent with insufficient allocated PAYE clients") {
      Given("User has an user id")
      isLoggedInWithUserId(userId)

      And("Enrolment store returns insufficient allocated IR-PAYE clients for the agent")
      tooFewClientsAreAllocated("IR-PAYE", userId)

      When("GET /acceptableNumberOfClients/service/IR-PAYE is called")
      val response: WSResponse = Await.result(wsClient.url(acceptableNumberOfPayeClientsUrl).get(), 10 seconds)

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    scenario("Logged in user is an agent with no allocated PAYE clients") {
      Given("User has an user id")
      isLoggedInWithUserId(userId)

      And("Enrolment store returns no allocated IR-PAYE clients for the agent")
      noClientsAreAllocated("IR-PAYE", userId)

      When("GET /acceptableNumberOfClients/service/IR-PAYE is called")
      val response: WSResponse = Await.result(wsClient.url(acceptableNumberOfPayeClientsUrl).get(), 10 seconds)

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    scenario("Logged in user is not an agent") {
      Given("User has no user id")
      isLoggedInWithoutUserId

      When("GET /acceptableNumberOfClients/service/IR-PAYE is called")
      val response: WSResponse = Await.result(wsClient.url(acceptableNumberOfPayeClientsUrl).get(), 10 seconds)

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    scenario("User is not logged in") {
      Given("User is not logged in")
      isNotLoggedIn

      When("GET /acceptableNumberOfClients/service/IR-PAYE is called")
      val response: WSResponse = Await.result(wsClient.url(acceptableNumberOfPayeClientsUrl).get(), 10 seconds)

      Then("401 UNAUTHORISED is returned")
      response.status shouldBe 401
    }
  }

  feature("/acceptableNumberOfClients/service/IR-SA") {
    scenario("Logged in user is an agent with sufficient allocated IR-SA clients") {
      Given("User has an user id")
      isLoggedInWithUserId(userId)

      And("Enrolment store returns sufficient allocated IR-SA clients for the agent")
      sufficientClientsAreAllocated("IR-SA", userId)

      When("GET /acceptableNumberOfClients/service/IR-SA is called")
      val response: WSResponse = Await.result(wsClient.url(acceptableNumberOfSAClientsUrl).get(), 10 seconds)

      Then("204 NO_CONTENT is returned")
      response.status shouldBe 204
    }

    scenario("Logged in user is an agent with insufficient allocated PAYE clients") {
      Given("User has an user id")
      isLoggedInWithUserId(userId)

      And("Enrolment store returns insufficient allocated IR-SA clients for the agent")
      tooFewClientsAreAllocated("IR-SA", userId)

      When("GET /acceptableNumberOfClients/service/IR-SA is called")
      val response: WSResponse = Await.result(wsClient.url(acceptableNumberOfSAClientsUrl).get(), 10 seconds)

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    scenario("Logged in user is an agent with no allocated PAYE clients") {
      Given("User has an user id")
      isLoggedInWithUserId(userId)

      And("Enrolment store returns no allocated IR-SA clients for the agent")
      noClientsAreAllocated("IR-SA", userId)

      When("GET /acceptableNumberOfClients/service/IR-SA is called")
      val response: WSResponse = Await.result(wsClient.url(acceptableNumberOfSAClientsUrl).get(), 10 seconds)

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    scenario("Logged in user is not an agent") {
      Given("User has no user id")
      isLoggedInWithoutUserId

      When("GET /acceptableNumberOfClients/service/IR-SA is called")
      val response: WSResponse = Await.result(wsClient.url(acceptableNumberOfSAClientsUrl).get(), 10 seconds)

      Then("403 FORBIDDEN is returned")
      response.status shouldBe 403
    }

    scenario("User is not logged in") {
      Given("User is not logged in")
      isNotLoggedIn

      When("GET /acceptableNumberOfClients/service/IR-SA is called")
      val response: WSResponse = Await.result(wsClient.url(acceptableNumberOfSAClientsUrl).get(), 10 seconds)

      Then("401 UNAUTHORISED is returned")
      response.status shouldBe 401
    }
  }

  feature("/amls/create") {

    val amlsCreateUrl = s"http://localhost:$port/agent-assurance/amls"

    def doRequest(payload: String) =
      Await.result(
        wsClient.url(amlsCreateUrl)
          .withHeaders(CONTENT_TYPE -> "application/json")
          .post(payload), 10 seconds
      )

    def payload(maybeUtr: Option[Utr] = None,
                supervisory: String = "supervisoryBody",
                arn: Option[Arn] = None) = {
      val utr = maybeUtr.getOrElse(Utr(Random.alphanumeric.take(10).mkString("")))
      val amlsDetails = AmlsDetails(utr, supervisory, "0123456789", LocalDate.now(), arn)
      Json.toJson(amlsDetails).toString()
    }

    scenario("user logged in and is an agent should be able to create a new Amls record for the first time") {
      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      When("POST /amls/create is called")
      val response: WSResponse = doRequest(payload())

      Then("201 CREATED is returned")
      response.status shouldBe 201
    }

    scenario("User is not logged in") {
      Given("User is not logged in")
      isNotLoggedIn

      When("POST /amls/create is called")
      val response: WSResponse = doRequest(payload())

      Then("401 UnAuthorized is returned")
      response.status shouldBe 401
    }

    scenario("update existing amls record but with no ARN should return success") {

      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      val utr = Some(Utr(Random.alphanumeric.take(10).mkString("")))

      When("POST /amls/create is called")
      val response: WSResponse = doRequest(payload(utr))

      Then("201 CREATED is returned")
      response.status shouldBe 201

      When("POST /amls/create is called second time with the same UTR")
      val newResponse: WSResponse = doRequest(payload(utr,"updated-supervisory"))

      Then("201 CREATED is returned")
      newResponse.status shouldBe 201
    }

    scenario("updates to an already existing amls record with an ARN should return error") {

      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      val arn = Some(Arn("12345"))
      val utr = Some(Utr(Random.alphanumeric.take(10).mkString("")))

      When("POST /amls/create is called")
      val response: WSResponse = doRequest(payload(utr, arn = arn))

      Then("201 CREATED is returned")
      response.status shouldBe 201

      When("POST /amls/create is called second time with the same UTR")
      val newResponse: WSResponse = doRequest(payload(utr, supervisory = "updated-supervisory", arn = arn))

      Then("500 INTERNAL_SERVER_ERROR is returned")
      newResponse.status shouldBe 500
    }
  }
}
