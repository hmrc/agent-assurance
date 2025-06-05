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

import scala.concurrent.ExecutionContext

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentassurance.helpers.TestConstants._
import uk.gov.hmrc.agentassurance.mocks.MockAppConfig
import uk.gov.hmrc.agentassurance.mocks.MockAuthConnector
import uk.gov.hmrc.agentassurance.mocks.MockDesConnector
import uk.gov.hmrc.agentassurance.mocks.MockDmsService
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.affinityGroup
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.allEnrolments
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.credentials
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.auth.core.AffinityGroup

class AgentServicesControllerSpec
extends PlaySpec
with MockAuthConnector
with MockAppConfig
with MockDesConnector
with MockDmsService
with BeforeAndAfterEach
with ScalaFutures {

  val controller =
    new AgentServicesController(
      mockDesConnector,
      mockAuthConnector,
      mockDmsService,
      stubControllerComponents()
    )(
      mockAppConfig,
      ExecutionContext.global
    )

  "getAgencyDetails" should {
    "return forbidden" when {
      "not an agent or stride" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments.and(affinityGroup).and(credentials))(
            enrolmentsWithoutIrSAAgent.and(None).and(None)
          )
        }

        val response = controller.getAgencyDetails(testArn)(FakeRequest())
        status(response) mustBe FORBIDDEN

      }
    }

    "return no content for an agent " when {
      "there are no records found in the database" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments.and(affinityGroup).and(credentials))(
            enrolmentsWithNoIrSAAgent.and(Some(AffinityGroup.Agent)).and(Some(Credentials("", "GovernmentGateway")))
          )
          mockGetAgentRecord(testArn)(testAgentDetailsDesEmptyResponse)
        }

        val response = controller.getAgencyDetails(testArn)(FakeRequest())
        status(response) mustBe NO_CONTENT
      }
    }

    "return no content for a stride user " when {
      "there are no records found in the database" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments.and(affinityGroup).and(credentials))(
            enrolmentsWithStride.and(None).and(Some(Credentials("", "PrivilegedApplication")))
          )
          mockGetAgentRecord(testArn)(testAgentDetailsDesEmptyResponse)
        }

        val response = controller.getAgencyDetails(testArn)(FakeRequest())
        status(response) mustBe NO_CONTENT
      }
    }

    "return OK" when {
      "and Utr and Agent Details for an agent" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments.and(affinityGroup).and(credentials))(
            enrolmentsWithNoIrSAAgent.and(Some(AffinityGroup.Agent)).and(Some(Credentials("", "GovernmentGateway")))
          )
          mockGetAgentRecord(testArn)(testAgentDetailsDesAddressUtrResponse)
        }

        val response = controller.getAgencyDetails(testArn)(FakeRequest())

        status(response) mustBe OK
        contentAsJson(response) mustBe Json.obj(
          "agencyDetails" -> Json.obj(
            "agencyName" -> "agencyName",
            "agencyEmail" -> "agencyEmail",
            "agencyTelephone" -> "agencyTelephone",
            "agencyAddress" -> Json.obj("addressLine1" -> "addressLine1", "countryCode" -> "GB")
          ),
          "utr" -> "7000000002"
        )
      }
      "and Agent Details No UTR for an agent" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments.and(affinityGroup).and(credentials))(
            enrolmentsWithNoIrSAAgent.and(Some(AffinityGroup.Agent)).and(Some(Credentials("", "GovernmentGateway")))
          )
          mockGetAgentRecord(testArn)(testAgentDetailsDesAddressUtrResponse)
        }

        val response = controller.getAgencyDetails(testArn)(FakeRequest())

        status(response) mustBe OK
        contentAsJson(response) mustBe Json.obj(
          "agencyDetails" -> Json.obj(
            "agencyName" -> "agencyName",
            "agencyEmail" -> "agencyEmail",
            "agencyTelephone" -> "agencyTelephone",
            "agencyAddress" -> Json.obj("addressLine1" -> "addressLine1", "countryCode" -> "GB")
          ),
          "utr" -> "7000000002"
        )
      }

    }

  }

  "PostAgencyDetails" should {
    "return Created when successful" in {
      inSequence {
        mockAuthWithNoRetrievals(allEnrolments.and(affinityGroup).and(credentials))(
          enrolmentsWithNoIrSAAgent.and(Some(AffinityGroup.Agent)).and(Some(Credentials("", "GovernmentGateway")))
        )
        mockSubmitToDmsSuccess
      }

      val response =
        controller.postAgencyDetails(testArn)(
          FakeRequest()
            .withJsonBody(Json.toJson(""))
            .withHeaders(CONTENT_TYPE -> "application/json")
        )

      status(response) mustBe CREATED
    }
  }

}
