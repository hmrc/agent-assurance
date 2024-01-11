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

package uk.gov.hmrc.agentassurance.controllers

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentassurance.helpers.TestConstants._
import uk.gov.hmrc.agentassurance.mocks.{MockAmlsDetailsService, MockAppConfig, MockAuthConnector}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, credentials}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved

import scala.concurrent.ExecutionContext

class GetAmlsDetailsByArnControllerSpec extends PlaySpec
  with MockAuthConnector
  with MockAppConfig
  with MockAmlsDetailsService
  with BeforeAndAfterEach {

  val controller = new GetAmlsDetailsByArnController(mockAmlsDetailsService, mockAuthConnector, stubControllerComponents())(mockAppConfig, ExecutionContext.global)

  "getAmlsDetails" should {
    "return forbidden" when {
      "not an agent or stride" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithoutIrSAAgent and None and None)
        }

        val response = controller.getAmlsDetails(testArn)(FakeRequest())
        status(response) mustBe FORBIDDEN

      }
    }

    "return not found for an agent " when {
     "there are no records found in the database" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))
          mockGetAmlsDetailsByArn(testArn)(Nil)
        }

        val response = controller.getAmlsDetails(testArn)(FakeRequest())
        status(response) mustBe NOT_FOUND

      }
    }

    "return not found for a stride user " when {
     "there are no records found in the database" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithStride and None and Some(Credentials("", "PrivilegedApplication")))
          mockGetAmlsDetailsByArn(testArn)(Nil)
        }

        val response = controller.getAmlsDetails(testArn)(FakeRequest())
        status(response) mustBe NOT_FOUND

      }
    }

    "return OK and AMLS Details for an agent" when {
     "only a UK AMLS Details record exists in the database" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))
          mockGetAmlsDetailsByArn(testArn)(Seq(testAmlsDetails))
        }

        val response = controller.getAmlsDetails(testArn)(FakeRequest())
        status(response) mustBe OK
      }

      "only an Overseas AMLS Details record exists in the database" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))
          mockGetAmlsDetailsByArn(testArn)(Seq(testOverseasAmlsDetails))
        }

        val response = controller.getAmlsDetails(testArn)(FakeRequest())
        status(response) mustBe OK
      }
    }

    "return OK and AMLS Details for an stride user" when {
     "only a UK AMLS Details record exists in the database" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithStride and None and Some(Credentials("", "PrivilegedApplication")))
          mockGetAmlsDetailsByArn(testArn)(Seq(testAmlsDetails))
        }

        val response = controller.getAmlsDetails(testArn)(FakeRequest())
        status(response) mustBe OK
      }

      "only an Overseas AMLS Details record exists in the database" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithStride and None and Some(Credentials("", "PrivilegedApplication")))
          mockGetAmlsDetailsByArn(testArn)(Seq(testOverseasAmlsDetails))
        }

        val response = controller.getAmlsDetails(testArn)(FakeRequest())
        status(response) mustBe OK
      }
    }

    /*
    TODO - add test for exception case using mockGetAmlsDetailsByArn(testArn)(Seq(testOverseasAmlsDetails, testAmlsDetails))
    TODO - check Json in OK responses equals Json.toJson(AmlsDetails) or Json.toJson(OverseasAmlsDetails)
     */
  }

}
