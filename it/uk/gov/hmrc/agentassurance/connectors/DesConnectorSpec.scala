package uk.gov.hmrc.agentassurance.connectors

import com.kenshoo.play.metrics.Metrics
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentassurance.stubs.{DataStreamStub, DesStubs}
import uk.gov.hmrc.agentassurance.support.{MetricTestSupport, WireMockSupport}
import uk.gov.hmrc.domain.{Nino, SaAgentReference, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import wiring.WSVerbs

import scala.concurrent.ExecutionContext

class DesConnectorSpec extends UnitSpec with OneAppPerSuite with WireMockSupport with DesStubs with DataStreamStub with MetricTestSupport {

  override implicit lazy val app: Application = appBuilder
    .build()

  val desConnector = new DesConnector(wireMockBaseUrl, "token", "stub", new WSVerbs()(app.configuration), app.injector.instanceOf[Metrics])

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

    val nino = Nino("AB123456C")

    "return one Agent when client has a single active agent" in {
      val agentId = SaAgentReference("bar")
      givenClientHasRelationshipWithAgentInCESA(nino, agentId)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(nino)) shouldBe Seq(agentId)
    }

    "return multiple Agents when client has multiple active agents" in {
      val agentIds = Seq("001","002","003","004","005","005","007").map(SaAgentReference.apply)
      givenClientHasRelationshipWithMultipleAgentsInCESA(nino, agentIds)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(nino)) should contain theSameElementsAs agentIds
    }

    "return empty seq when client has no active relationship with an agent" in {
      givenClientHasNoActiveRelationshipWithAgentInCESA(nino)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(nino)) shouldBe empty
    }

    "return empty seq when client has/had no relationship with any agent" in {
      givenClientHasNoRelationshipWithAnyAgentInCESA(nino)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(nino)) shouldBe empty
    }

    "return empty seq when client relationship with agent ceased" in {
      givenClientRelationshipWithAgentCeasedInCESA(nino, "foo")
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(nino)) shouldBe empty
    }

    "return empty seq when all client's relationships with agents ceased" in {
      givenAllClientRelationshipsWithAgentsCeasedInCESA(nino, Seq("001", "002", "003", "004", "005", "005", "007"))
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(nino)) shouldBe empty
    }

    "fail when client's nino is invalid" in {
      givenNinoIsInvalid(nino)
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(nino))
    }

    "fail when client is unknown" in {
      givenClientIsUnknownInCESAFor(nino)
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(nino))
    }

    "fail when DES is unavailable" in {
      givenDesReturnsServiceUnavailable()
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(nino))
    }

    "fail when DES is throwing errors" in {
      givenDesReturnsServerError()
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(nino))
    }

    "record metrics for GetStatusAgentRelationship" in {
      givenClientHasRelationshipWithAgentInCESA(nino, SaAgentReference("bar"))
      givenCleanMetricRegistry()
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(nino))
      timerShouldExistsAndBeenUpdated("ConsumedAPI-DES-GetStatusAgentRelationship-GET")
    }
  }

  private def aCheckEndpoint(identifier: TaxIdentifier) = {
    "return one Agent when client has a single active agent" in {
      val agentId = SaAgentReference("bar")
      givenClientHasRelationshipWithAgentInCESA(identifier, agentId)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(nino)) shouldBe Seq(agentId)
    }

    "return multiple Agents when client has multiple active agents" in {
      val agentIds = Seq("001","002","003","004","005","005","007").map(SaAgentReference.apply)
      givenClientHasRelationshipWithMultipleAgentsInCESA(nino, agentIds)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(nino)) should contain theSameElementsAs agentIds
    }

    "return empty seq when client has no active relationship with an agent" in {
      givenClientHasNoActiveRelationshipWithAgentInCESA(nino)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(nino)) shouldBe empty
    }

    "return empty seq when client has/had no relationship with any agent" in {
      givenClientHasNoRelationshipWithAnyAgentInCESA(nino)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(nino)) shouldBe empty
    }

    "return empty seq when client relationship with agent ceased" in {
      givenClientRelationshipWithAgentCeasedInCESA(nino, "foo")
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(nino)) shouldBe empty
    }

    "return empty seq when all client's relationships with agents ceased" in {
      givenAllClientRelationshipsWithAgentsCeasedInCESA(nino, Seq("001", "002", "003", "004", "005", "005", "007"))
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(nino)) shouldBe empty
    }

    "fail when client's nino is invalid" in {
      givenNinoIsInvalid(nino)
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(nino))
    }

    "fail when client is unknown" in {
      givenClientIsUnknownInCESAFor(nino)
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(nino))
    }

    "fail when DES is unavailable" in {
      givenDesReturnsServiceUnavailable()
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(nino))
    }

    "fail when DES is throwing errors" in {
      givenDesReturnsServerError()
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(nino))
    }

    "record metrics for GetStatusAgentRelationship" in {
      givenClientHasRelationshipWithAgentInCESA(nino, SaAgentReference("bar"))
      givenCleanMetricRegistry()
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(nino))
      timerShouldExistsAndBeenUpdated("ConsumedAPI-DES-GetStatusAgentRelationship-GET")
    }

  }
}