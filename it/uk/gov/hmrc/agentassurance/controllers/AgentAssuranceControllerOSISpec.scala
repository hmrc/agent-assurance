package uk.gov.hmrc.agentassurance.controllers

import com.google.inject.AbstractModule
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.{CONTENT_TYPE, await, defaultAwaitTimeout}
import uk.gov.hmrc.agentassurance.models.{AmlsSources, OverseasAmlsDetails, OverseasAmlsEntity}
import uk.gov.hmrc.agentassurance.repositories.OverseasAmlsRepositoryImpl
import uk.gov.hmrc.agentassurance.support.{AgentAuthStubs, IntegrationSpec, WireMockSupport}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class AgentAssuranceControllerOSISpec extends IntegrationSpec with AgentAuthStubs with GuiceOneServerPerSuite with WireMockSupport with
  DefaultPlayMongoRepositorySupport[OverseasAmlsEntity] {

  override lazy val repository = new OverseasAmlsRepositoryImpl(mongoComponent)

  override implicit lazy val app: Application = appBuilder.build()

  val moduleWithOverrides: AbstractModule = new AbstractModule() {
    override def configure(): Unit = {
      bind(classOf[OverseasAmlsRepositoryImpl]).toInstance(repository)
    }
  }

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure("microservice.services.auth.host" -> wireMockHost,
        "microservice.services.auth.port" -> wireMockPort,
        "auditing.enabled" -> false,
        "stride.roles.agent-assurance" -> "maintain_agent_manually_assure")
      .overrides(moduleWithOverrides)

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  override def irAgentReference: String = "IRSA-123"

  Feature("/overseas-agents/amls") {
    val amlsCreateUrl = s"http://localhost:$port/agent-assurance/overseas-agents/amls"

    val arn = Arn("AARN0000002")

    val amlsDetails = OverseasAmlsDetails("supervisory", Some("0123456789"))
    val createOverseasAmlsRequest = OverseasAmlsEntity(arn, amlsDetails, AmlsSources.Subscription)

    def doRequest(request: OverseasAmlsEntity) =
      Await.result(
        wsClient.url(amlsCreateUrl)
          .withHttpHeaders(CONTENT_TYPE -> "application/json", "Authorization" -> "Bearer XYZ")
          .post(Json.toJson(request)), 10.seconds
      )

    Scenario("user logged in and is an agent should be able to create a new Amls record for the first time") {
      Given("User is logged in and is an overseas agent")
      withAffinityGroupAgent

      When("POST /overseas-agents/amls is called")
      val response: WSResponse = doRequest(createOverseasAmlsRequest)

      Then("201 CREATED is returned")
      response.status shouldBe 201
      val dbRecord = await(repository.collection.find().toFuture()).head
      dbRecord.arn shouldBe arn
    }

    Scenario("user logged in and is an agent should be able to create a new Amls record without membershipId") {
      Given("User is logged in and is an overseas agent")
      withAffinityGroupAgent

      When("POST /overseas-agents/amls is called")
      val response: WSResponse = doRequest(createOverseasAmlsRequest.copy(amlsDetails = OverseasAmlsDetails("AOSS", None)))

      Then("201 CREATED is returned")
      response.status shouldBe 201

      val dbRecord = await(repository.collection.find().toFuture()).head
      dbRecord.arn shouldBe arn
      dbRecord.amlsDetails.membershipNumber shouldBe None
    }

    Scenario("User is not logged in") {
      Given("User is not logged in")
      isNotLoggedIn

      When("POST /overseas-agents/amls is called")
      val response: WSResponse = doRequest(createOverseasAmlsRequest)

      Then("401 UnAuthorized is returned")
      response.status shouldBe 401
    }

    Scenario("return bad_request if Arn is not valid") {
      Given("User is logged in and is an agent")
      withAffinityGroupAgent

      When("POST /overseas-agents/amls is called with invalid arn")
      val response: WSResponse = doRequest(createOverseasAmlsRequest.copy(arn = Arn("61122334455")))

      Then("400 BAD_REQUEST is returned")
      response.status shouldBe 400
    }

    Scenario("return conflict if the amls record exists") {
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

      await(repository.collection.find().toFuture()).size shouldBe 1
    }
  }

}
