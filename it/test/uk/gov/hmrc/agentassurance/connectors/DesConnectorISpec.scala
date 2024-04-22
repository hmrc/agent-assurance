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

package test.uk.gov.hmrc.agentassurance.connectors

import scala.concurrent.ExecutionContext

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.Application
import test.uk.gov.hmrc.agentassurance.stubs.DataStreamStub
import test.uk.gov.hmrc.agentassurance.stubs.DesStubs
import test.uk.gov.hmrc.agentassurance.support.MetricTestSupport
import test.uk.gov.hmrc.agentassurance.support.UnitSpec
import test.uk.gov.hmrc.agentassurance.support.WireMockSupport
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.connectors.DesConnector
import uk.gov.hmrc.agentassurance.connectors.DesConnectorImpl
import uk.gov.hmrc.agentassurance.models.AgencyDetails
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentassurance.models.BusinessAddress
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.SuspensionDetails
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

class DesConnectorISpec
    extends UnitSpec
    with GuiceOneAppPerSuite
    with WireMockSupport
    with DesStubs
    with DataStreamStub
    with MetricTestSupport {

  implicit override lazy val app: Application = appBuilder
    .build()

  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val desConnector = new DesConnectorImpl(app.injector.instanceOf[HttpClientV2], app.injector.instanceOf[Metrics])

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.host"                  -> wireMockHost,
        "microservice.services.auth.port"                  -> wireMockPort,
        "microservice.services.des.host"                   -> wireMockHost,
        "microservice.services.des.port"                   -> wireMockPort,
        "microservice.services.des.environment"            -> "test",
        "microservice.services.des.authorization-token"    -> "secret",
        "microservice.services.enrolment-store-proxy.host" -> wireMockHost,
        "microservice.services.enrolment-store-proxy.port" -> wireMockPort,
        "auditing.consumer.baseUri.host"                   -> wireMockHost,
        "auditing.consumer.baseUri.port"                   -> wireMockPort,
        "internal-auth-token-enabled-on-start"             -> false
      )
      .bindings(bind[DesConnector].toInstance(desConnector))

  private implicit val hc: HeaderCarrier    = HeaderCarrier()
  private implicit val ec: ExecutionContext = ExecutionContext.global

  val arn = Arn("AARN00012345")

  "DesConnector getActiveCesaAgentRelationships with a valid NINO" should {
    behave.like(aCheckEndpoint(Nino("AB123456C")))
  }

  "DesConnector getActiveCesaAgentRelationships with a valid UTR" should {
    behave.like(aCheckEndpoint(Utr("7000000002"))) // 7000000002
  }

  "DesConnector getAgentRecord" should {
    "return agency details for a given ARN" in {

      givenDESGetAgentRecord(Arn(arn.value), Some(Utr("0123456789")))

      val result = await(desConnector.getAgentRecord(arn))
      result shouldBe
        AgentDetailsDesResponse(
          Some(Utr("0123456789")),
          Some(
            AgencyDetails(
              Some("ABC Accountants"),
              Some("abc@xyz.com"),
              Some("07345678901"),
              Some(
                BusinessAddress(
                  "Matheson House",
                  Some("Grange Central"),
                  Some("Town Centre"),
                  Some("Telford"),
                  Some("TF3 4ER"),
                  "GB"
                )
              )
            )
          ),
          Some(SuspensionDetails(suspensionStatus = false, None))
        )
    }
  }

  private def aCheckEndpoint(identifier: TaxIdentifier) = {
    "return one Agent when client has a single active agent" in {
      val agentId = SaAgentReference("bar")
      givenClientHasRelationshipWithAgentInCESA(identifier, agentId)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)) shouldBe Some(Seq(agentId))
    }

    "return multiple Agents when client has multiple active agents" in {
      val agentIds = Option(Seq("001", "002", "003", "004", "005", "005", "007").map(SaAgentReference.apply))
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
      givenAllClientRelationshipsWithAgentsCeasedInCESA(
        identifier,
        Seq("001", "002", "003", "004", "005", "005", "007")
      )
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
