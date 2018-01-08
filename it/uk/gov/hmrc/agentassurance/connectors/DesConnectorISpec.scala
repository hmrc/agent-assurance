package uk.gov.hmrc.agentassurance.connectors

import com.kenshoo.play.metrics.Metrics
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentassurance.stubs.{DataStreamStub, DesStubs}
import uk.gov.hmrc.agentassurance.support.{MetricTestSupport, WireMockSupport}
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.domain.{Nino, SaAgentReference, TaxIdentifier}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext

class DesConnectorISpec extends UnitSpec with OneAppPerSuite with WireMockSupport with DesStubs with DataStreamStub with MetricTestSupport {

  override implicit lazy val app: Application = appBuilder
    .build()

  val desConnector = new DesConnector(wireMockBaseUrl, "token", "stub", app.injector.instanceOf[HttpGet], app.injector.instanceOf[Metrics])

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.des.port" -> wireMockPort,
        "auditing.consumer.baseUri.host" -> wireMockHost,
        "auditing.consumer.baseUri.port" -> wireMockPort
      )
      .bindings(bind[DesConnector].toInstance(desConnector))

  private implicit val hc = HeaderCarrier()
  private implicit val ec = ExecutionContext.global

  "DesConnector getActiveCesaAgentRelationships with a valid NINO" should {
    behave like aCheckEndpoint(Nino("AB123456C"))
  }

  "DesConnector getActiveCesaAgentRelationships with a valid UTR" should {
    behave like aCheckEndpoint(Utr("7000000002"))
  }

  private def aCheckEndpoint(identifier: TaxIdentifier) = {
    "return one Agent when client has a single active agent" in {
      val agentId = SaAgentReference("bar")
      givenClientHasRelationshipWithAgentInCESA(identifier, agentId)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)) shouldBe Seq(agentId)
    }

    "return multiple Agents when client has multiple active agents" in {
      val agentIds = Seq("001","002","003","004","005","005","007").map(SaAgentReference.apply)
      givenClientHasRelationshipWithMultipleAgentsInCESA(identifier, agentIds)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)) should contain theSameElementsAs agentIds
    }

    "return empty seq when client has no active relationship with an agent" in {
      givenClientHasNoActiveRelationshipWithAgentInCESA(identifier)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)) shouldBe empty
    }

    "return empty seq when client has/had no relationship with any agent" in {
      givenClientHasNoRelationshipWithAnyAgentInCESA(identifier)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)) shouldBe empty
    }

    "return empty seq when client relationship with agent ceased" in {
      givenClientRelationshipWithAgentCeasedInCESA(identifier, "foo")
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)) shouldBe empty
    }

    "return empty seq when all client's relationships with agents ceased" in {
      givenAllClientRelationshipsWithAgentsCeasedInCESA(identifier, Seq("001", "002", "003", "004", "005", "005", "007"))
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)) shouldBe empty
    }

    "fail when client id is invalid" in {
      givenClientIdentifierIsInvalid(identifier)
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(identifier))
    }

    "fail when client is unknown" in {
      givenClientIsUnknownInCESAFor(identifier)
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(identifier))
    }

    "fail when DES is unavailable" in {
      givenDesReturnsServiceUnavailable()
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(identifier))
    }

    "fail when DES is throwing errors" in {
      givenDesReturnsServerError()
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(identifier))
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