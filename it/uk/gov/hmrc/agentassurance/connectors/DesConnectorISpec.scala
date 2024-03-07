package uk.gov.hmrc.agentassurance.connectors

import com.kenshoo.play.metrics.Metrics
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.stubs.{DataStreamStub, DesStubs}
import uk.gov.hmrc.agentassurance.support.{MetricTestSupport, UnitSpec, WireMockSupport}
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.domain.{Nino, SaAgentReference, TaxIdentifier}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.ExecutionContext

class DesConnectorISpec extends UnitSpec with GuiceOneAppPerSuite with WireMockSupport with DesStubs with DataStreamStub with MetricTestSupport {

  override implicit lazy val app: Application = appBuilder
    .build()

  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val desConnector = new DesConnectorImpl(app.injector.instanceOf[HttpClient], app.injector.instanceOf[Metrics])

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.host" -> wireMockHost,
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.des.host" -> wireMockHost,
        "microservice.services.des.port" -> wireMockPort,
        "microservice.services.des.environment" -> "test",
        "microservice.services.des.authorization-token" -> "secret",
        "microservice.services.enrolment-store-proxy.host" -> wireMockHost,
        "microservice.services.enrolment-store-proxy.port" -> wireMockPort,
        "auditing.consumer.baseUri.host" -> wireMockHost,
        "auditing.consumer.baseUri.port" -> wireMockPort
      )
      .bindings(bind[DesConnector].toInstance(desConnector))


  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val ec: ExecutionContext = ExecutionContext.global

  "DesConnector getActiveCesaAgentRelationships with a valid NINO" should {
    behave like aCheckEndpoint(Nino("AB123456C"))
  }

  "DesConnector getActiveCesaAgentRelationships with a valid UTR" should {
    behave like aCheckEndpoint(Utr("7000000002")) // 7000000002
  }

  private def aCheckEndpoint(identifier: TaxIdentifier) = {
    "return one Agent when client has a single active agent" in {
      val agentId = SaAgentReference("bar")
      givenClientHasRelationshipWithAgentInCESA(identifier, agentId)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)) shouldBe Some(Seq(agentId))
    }

    "return multiple Agents when client has multiple active agents" in {
      val agentIds = Option(Seq("001","002","003","004","005","005","007").map(SaAgentReference.apply))
      givenClientHasRelationshipWithMultipleAgentsInCESA(identifier, agentIds.get)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)).get should contain theSameElementsAs agentIds.get
    }

    "return empty seq when client has no active relationship with an agent" in {
      givenClientHasNoActiveRelationshipWithAgentInCESA(identifier)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)).get shouldBe empty
    }

    "return empty seq when client has/had no relationship with any agent" in {
      givenClientHasNoRelationshipWithAnyAgentInCESA(identifier)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)).get shouldBe empty
    }

    "return empty seq when client relationship with agent ceased" in {
      givenClientRelationshipWithAgentCeasedInCESA(identifier, "foo")
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)).get shouldBe empty
    }

    "return empty seq when all client's relationships with agents ceased" in {
      givenAllClientRelationshipsWithAgentsCeasedInCESA(identifier, Seq("001", "002", "003", "004", "005", "005", "007"))
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)).get shouldBe empty
    }

    "fail when client id is invalid" in {
      givenClientIdentifierIsInvalid(identifier)
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(identifier)).get
    }

    "fail when client is unknown" in {
      givenClientIsUnknownInCESAFor(identifier)
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(identifier)).get
    }

    "When NOT_FOUND(404) occurs return None" in {
      givenClientIsUnknown404(identifier)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)) shouldBe None
    }

    "fail when DES is unavailable" in {
      givenDesReturnsServiceUnavailable()
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(identifier)).get
    }


    "return 502 when DES returns BadGateway error" in {
      givenDesReturnBadGateway()
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(identifier)).get
    }

    "fail when DES is throwing errors" in {
      givenDesReturnsServerError()
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(identifier)).get
    }

    "record metrics for GetStatusAgentRelationship" in {
      givenClientHasRelationshipWithAgentInCESA(identifier, SaAgentReference("bar"))
      givenCleanMetricRegistry()
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier))
      timerShouldExistsAndBeenUpdated("ConsumedAPI-DES-GetStatusAgentRelationship-GET")
    }

  }
}