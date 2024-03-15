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
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentassurance.helpers.TestConstants._
import uk.gov.hmrc.agentassurance.mocks.{MockAmlsDetailsService, MockAppConfig, MockAuthConnector}
import uk.gov.hmrc.agentassurance.models.AmlsStatus
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, credentials}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.ExecutionContext

class AmlsDetailsByArnControllerSpec extends PlaySpec
  with MockAuthConnector
  with MockAppConfig
  with MockAmlsDetailsService
  with BeforeAndAfterEach with ScalaFutures {

  val controller = new AmlsDetailsByArnController(mockAmlsDetailsService, mockAuthConnector, stubControllerComponents())(mockAppConfig, ExecutionContext.global)

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

    "return no content for an agent " when {
     "there are no records found in the database" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))
          mockGetAmlsDetailsByArn(testArn)(Nil)
        }

        val response = controller.getAmlsDetails(testArn)(FakeRequest())
        status(response) mustBe NO_CONTENT

      }
    }

    "return no content for a stride user " when {
     "there are no records found in the database" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithStride and None and Some(Credentials("", "PrivilegedApplication")))
          mockGetAmlsDetailsByArn(testArn)(Nil)
        }

        val response = controller.getAmlsDetails(testArn)(FakeRequest())
        status(response) mustBe NO_CONTENT

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
       contentAsString(response) mustBe """{"supervisoryBody":"supervisory","membershipNumber":"0123456789","membershipExpiresOn":"2024-01-12"}"""

      }

      "only an Overseas AMLS Details record exists in the database" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))
          mockGetAmlsDetailsByArn(testArn)(Seq(testOverseasAmlsDetails))
        }

        val response = controller.getAmlsDetails(testArn)(FakeRequest())
        status(response) mustBe OK
        contentAsString(response) mustBe """{"supervisoryBody":"supervisory","membershipNumber":"0123456789"}"""
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
        contentAsString(response) mustBe """{"supervisoryBody":"supervisory","membershipNumber":"0123456789"}"""
      }
    }

    "return internal server error" when {
      "both UK and overseas records are found" in {
        inSequence{
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithStride and None and Some(Credentials("", "PrivilegedApplication")))
          mockGetAmlsDetailsByArn(testArn)(Seq(testOverseasAmlsDetails, testAmlsDetails))
        }

        an[InternalServerException] mustBe thrownBy {
                  await(controller.getAmlsDetails(testArn)(FakeRequest()))
        }
      }
    }
  }


  "getAmlsStatus" should {
    "return forbidden" when {
      "not an agent or stride" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithoutIrSAAgent and None and None)
        }

        val response = controller.getAmlsDetails(testArn)(FakeRequest())
        status(response) mustBe FORBIDDEN

      }
    }
    "return no content for an agent " when {
      "there are no records found in the database" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))
          mockGetAmlsStatus(testArn)(AmlsStatus.NoAmlsDetailsUK)
        }

        val response = controller.getAmlsStatus(testArn)(FakeRequest())
        status(response) mustBe OK
        contentAsString(response) mustBe """{"NoAmlsDetailsUK":{}}"""
      }
    }

  }
}
