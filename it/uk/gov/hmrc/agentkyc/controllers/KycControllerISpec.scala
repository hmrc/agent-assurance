package uk.gov.hmrc.agentkyc.controllers

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.agentkyc.support.{AgentAuthStubs, IntegrationSpec, WireMockSupport}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Await
import scala.concurrent.duration._


class KycControllerISpec extends IntegrationSpec with GuiceOneServerPerSuite with AgentAuthStubs with WireMockSupport{
  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder().configure("microservice.services.auth.port" -> wireMockPort)

  implicit val hc = new HeaderCarrier

  val irSaAgentEnrolmentUrl = s"http://localhost:$port/agent-kyc/irSaAgentEnrolment"
  val wsClient: WSClient = app.injector.instanceOf[WSClient]
  override def irAgentReference: String = "REF879"

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
}
