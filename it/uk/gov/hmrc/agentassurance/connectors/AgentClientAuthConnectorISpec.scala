package uk.gov.hmrc.agentassurance.connectors

import com.kenshoo.play.metrics.Metrics
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.models.{AgencyDetails, BusinessAddress}
import uk.gov.hmrc.agentassurance.stubs.AgentClientAuthorisationStub
import uk.gov.hmrc.agentassurance.support.{UnitSpec, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, InternalServerException}

import scala.concurrent.ExecutionContext

class AgentClientAuthConnectorISpec extends UnitSpec with GuiceOneAppPerSuite with WireMockSupport with AgentClientAuthorisationStub {

  override implicit lazy val app: Application = appBuilder.build()

  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val acaConnector = new AgentClientAuthConnectorImpl(app.injector.instanceOf[HttpClient], app.injector.instanceOf[Metrics])

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.host" -> wireMockHost,
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.des.host" -> wireMockHost,
        "microservice.services.des.port" -> wireMockPort,
        "microservice.services.agent-client-authorisation.host" -> wireMockHost,
        "microservice.services.agent-client-authorisation.port" -> wireMockPort,
        "microservice.services.des.environment" -> "test",
        "microservice.services.des.authorization-token" -> "secret",
        "microservice.services.enrolment-store-proxy.host" -> wireMockHost,
        "microservice.services.enrolment-store-proxy.port" -> wireMockPort,
        "auditing.consumer.baseUri.host" -> wireMockHost,
        "auditing.consumer.baseUri.port" -> wireMockPort,
        "internal-auth-token-enabled" -> false
      )
      .bindings(bind[AgentClientAuthConnectorImpl].toInstance(acaConnector))

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val ec: ExecutionContext = ExecutionContext.global

  "getAgencyDetails" should {
    "return agency details for a given agent" in {
      val agentDetails = AgencyDetails(
        Some("My Agency"),
        Some("abc@abc.com"),
        Some("07345678901"),
        Some(BusinessAddress(
          "25 Any Street",
          Some("Central Grange"),
          Some("Telford"),
          None,
          Some("TF4 3TR"),
          "GB"))
      )

      getAgentDetails(Json.toJson(agentDetails), OK)

      await(acaConnector.getAgencyDetails()) shouldBe Some(agentDetails)
    }

    "return None when response is 204" in {
      getAgentDetails(Json.obj(), NO_CONTENT)
      await(acaConnector.getAgencyDetails()) shouldBe None
    }

    "return None with any other status" in {
      getAgentDetails(Json.obj(), INTERNAL_SERVER_ERROR)
      await(acaConnector.getAgencyDetails()) shouldBe None
    }

    "throw an exception if JSON does not parse" in {
      getAgentDetails(Json.obj("agencyName" -> 1), OK) //not empty because all AgencyDetails are optional

      intercept[InternalServerException]{ await(acaConnector.getAgencyDetails()) }
    }


  }

}
