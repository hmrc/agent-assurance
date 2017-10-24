/*
 * Copyright 2017 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.{any, eq => eqs}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.agentassurance.connectors.{DesConnector, GovernmentGatewayConnector}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.domain.{Nino, SaAgentReference}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future


class AgentAssuranceControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach {
  val desConnector = mock[DesConnector]
  val governmentGatewayConnector =  mock[GovernmentGatewayConnector]
  val authConnector =  mock[AuthConnector]

  val controller = new AgentAssuranceController(6, 6, authConnector, desConnector, governmentGatewayConnector)

  implicit val hc = new HeaderCarrier

  val irSaAgentEnrolment = Set(
    Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", "IRSA-123")), state = "activated", delegatedAuthRule = None)
  )

  val hmrcAsAgentEnrolment = Set(
    Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", "ARN123")), state = "activated", delegatedAuthRule = None)
  )

  val enrolmentsWithIrSAAgent = Enrolments(irSaAgentEnrolment)
  val enrolmentsWithNoIrSAAgent = Enrolments(hmrcAsAgentEnrolment)
  val enrolmentsWithoutIrSAAgent = Enrolments(Set.empty)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(desConnector, authConnector)
  }

  "AgentAssuranceController" when {
    "enrolledForIrSAAgent is called" should {
      "return NO_CONTENT where the current user is enrolled in IR-SA-AGENT" in {
        when(authConnector.authorise(any[Predicate], any[Retrieval[Enrolments]])(any(), any())).thenReturn(Future.successful(enrolmentsWithIrSAAgent))

        val response = controller.enrolledForIrSAAgent()(FakeRequest())

        status(response) mustBe NO_CONTENT
      }

      "return FORBIDDEN where the current user is not enrolled in IR-SA-AGENT" in {
        when(authConnector.authorise(any[Predicate], any[Retrieval[Enrolments]])(any(), any())).thenReturn(Future.successful(enrolmentsWithNoIrSAAgent))

        val response = controller.enrolledForIrSAAgent()(FakeRequest())

        status(response) mustBe FORBIDDEN
      }

      "return FORBIDDEN where the current user has no enrolments" in {
        when(authConnector.authorise(any[Predicate], any[Retrieval[Enrolments]])(any(), any())).thenReturn(Future.successful(enrolmentsWithoutIrSAAgent))

        val response = controller.enrolledForIrSAAgent()(FakeRequest())

        status(response) mustBe FORBIDDEN
      }
    }

    "activeCesaRelationship is called with a valid NINO that exists in CESA with the same IRAgentReference as the logged in user" should {

      "return NO_CONTENT where the current user is enrolled in IR-SA-AGENT" in {
        when(authConnector.authorise(any[Predicate], any[Retrieval[Enrolments]])(any(), any())).thenReturn(Future.successful(enrolmentsWithIrSAAgent))
        when(desConnector.getActiveCesaAgentRelationships(eqs(Nino("AA000000A")))(any(), any())).thenReturn(Future.successful(Seq(SaAgentReference("IRSA-123"))))

        val response = controller.activeCesaRelationship(Nino("AA000000A"))(FakeRequest())

        status(response) mustBe NO_CONTENT
        verify(desConnector, times(1)).getActiveCesaAgentRelationships(eqs(Nino("AA000000A")))(any(), any())
      }

      "return UNAUTHORIZED where the current user is not logged in" in {
        when(authConnector.authorise(any[Predicate], any[Retrieval[Enrolments]])(any(), any())).thenReturn(Future.failed(new MissingBearerToken))

        val response = controller.activeCesaRelationship(Nino("AA000000A"))(FakeRequest())

        status(response) mustBe UNAUTHORIZED
      }

      "return FORBIDDEN where the current user is not enrolled in IR-SA-AGENT" in {
        when(authConnector.authorise(any[Predicate], any[Retrieval[Enrolments]])(any(), any())).thenReturn(Future.successful(enrolmentsWithNoIrSAAgent))

        val response = controller.activeCesaRelationship(Nino("AA000000A"))(FakeRequest())

        status(response) mustBe FORBIDDEN
      }

      "return FORBIDDEN where the current user has no enrolments" in {
        when(authConnector.authorise(any[Predicate], any[Retrieval[Enrolments]])(any(), any())).thenReturn(Future.successful(enrolmentsWithoutIrSAAgent))

        val response = controller.activeCesaRelationship(Nino("AA000000A"))(FakeRequest())

        status(response) mustBe FORBIDDEN
      }
    }

    "activeCesaRelationship is called where the current user is enrolled in IR-SA-AGENT" should {
      "return FORBIDDEN when called with a valid NINO that is not active in CESA" in {
        when(authConnector.authorise(any[Predicate], any[Retrieval[Enrolments]])(any(), any())).thenReturn(Future.successful(enrolmentsWithIrSAAgent))
        when(desConnector.getActiveCesaAgentRelationships(eqs(Nino("AA000000A")))(any(), any())).thenReturn(Future.successful(Seq.empty))

        val response = controller.activeCesaRelationship(Nino("AA000000A"))(FakeRequest())

        status(response) mustBe FORBIDDEN
        verify(desConnector, times(1)).getActiveCesaAgentRelationships(eqs(Nino("AA000000A")))(any(), any())
      }

      "return FORBIDDEN when called with a valid NINO that is active in CESA but with a different IRAgentReference to the logged in user" in {
        when(authConnector.authorise(any[Predicate], any[Retrieval[Enrolments]])(any(), any())).thenReturn(Future.successful(enrolmentsWithIrSAAgent))
        when(desConnector.getActiveCesaAgentRelationships(eqs(Nino("AA000000A")))(any(), any())).thenReturn(Future.successful(Seq(SaAgentReference("IRSA-456"))))

        val response = controller.activeCesaRelationship(Nino("AA000000A"))(FakeRequest())

        status(response) mustBe FORBIDDEN
        verify(desConnector, times(1)).getActiveCesaAgentRelationships(eqs(Nino("AA000000A")))(any(), any())
      }
    }
  }
}
