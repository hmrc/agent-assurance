/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.FakeRequest
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.helpers.TestConstants._
import uk.gov.hmrc.agentassurance.mocks._
import uk.gov.hmrc.agentassurance.models.AmlsError.{AmlsRecordExists, AmlsUnexpectedMongoError, UniqueKeyViolationError}
import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, credentials}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, EmptyRetrieval}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AgentAssuranceControllerSpec extends PlaySpec
  with MockFactory
  with MockAuthConnector
  with MockAmlsRepository
  with MockOverseasAmlsRepository
  with MockEnrolmentStoreProxyConnector
  with MockDesConnector
  with MockAppConfig
  with BeforeAndAfterEach {

  implicit val appConfig: AppConfig = mockAppConfig

  val controller =
    new AgentAssuranceController(
      mockAuthConnector,
      mockDesConnector,
      mockEspConnector,
      mockOverseasAmlsRepository,
      stubControllerComponents(),
      mockAmlsRepository
    )

  implicit val hc: HeaderCarrier = new HeaderCarrier

  "AgentAssuranceController" when {
    "enrolledForIrSAAgent is called" should {
      "return NO_CONTENT where the current user is enrolled in IR-SA-AGENT" in {
        mockAuth()(Right(enrolmentsWithIrSAAgent))

        val response = controller.enrolledForIrSAAgent()(FakeRequest())

        status(response) mustBe NO_CONTENT
      }

      "return FORBIDDEN where the current user is not enrolled in IR-SA-AGENT" in {
        mockAuth()(Right(enrolmentsWithNoIrSAAgent))
        val response = controller.enrolledForIrSAAgent()(FakeRequest())

        status(response) mustBe FORBIDDEN
      }

      "return FORBIDDEN where the current user has no enrolments" in {
        mockAuth()(Right(enrolmentsWithoutIrSAAgent))
        val response = controller.enrolledForIrSAAgent()(FakeRequest())

        status(response) mustBe FORBIDDEN
      }
    }

    "activeCesaRelationship is called with NINO" should {
      "return OK where the user provides a valid NINO and saAgentReference nad has an active relationship in CESA" in {
        inSequence {
          mockAuthWithNoRetrievals(EmptyRetrieval)(())
          mockDes(testNino)(Right(Some(Seq(testSaAgentReference))))
        }

        val response = controller.activeCesaRelationshipWithNino(testNino, testSaAgentReference)(FakeRequest())
        status(response) mustBe OK
      }

      "return FORBIDDEN when called with a valid NINO that is not active in CESA" in {
        inSequence {
          mockAuthWithNoRetrievals(EmptyRetrieval)(())
          mockDes(testNino)(Right(Some(Seq.empty)))
        }

        val response = controller.activeCesaRelationshipWithNino(testNino, testSaAgentReference)(FakeRequest())
        status(response) mustBe FORBIDDEN
      }

      "return FORBIDDEN when called with a valid NINO that is active in CESA but with a different IRAgentReference" in {
        inSequence {
          mockAuthWithNoRetrievals(EmptyRetrieval)(())
          mockDes(testNino)(Right(Some(Seq(SaAgentReference("IRSA-456")))))
        }
        val response = controller.activeCesaRelationshipWithNino(testNino, testSaAgentReference)(FakeRequest())

        status(response) mustBe FORBIDDEN
      }
    }

    "activeCesaRelationship is called with UTR" should {
      "return OK where the user provides a valid UTR and saAgentReference nad has an active relationship in CESA" in {
        inSequence {
          mockAuthWithNoRetrievals(EmptyRetrieval)(())
          mockDes(testUtr)(Right(Some(Seq(testSaAgentReference))))
        }
        val response = controller.activeCesaRelationshipWithUtr(testUtr, testSaAgentReference)(FakeRequest())

        status(response) mustBe OK
      }

      "return FORBIDDEN when called with a valid UTR that is not active in CESA" in {
        inSequence {
          mockAuthWithNoRetrievals(EmptyRetrieval)(())
          mockDes(testUtr)(Right(Some(Seq.empty)))
        }
        val response = controller.activeCesaRelationshipWithUtr(testUtr, testSaAgentReference)(FakeRequest())
        status(response) mustBe FORBIDDEN
      }

      "return FORBIDDEN when called with a valid UTR that is active in CESA but with a different IRAgentReference" in {
        inSequence {
          mockAuthWithNoRetrievals(EmptyRetrieval)(())
          mockDes(testUtr)(Right(Some(Seq(SaAgentReference("IRSA-456")))))
        }
        val response = controller.activeCesaRelationshipWithUtr(testUtr, testSaAgentReference)(FakeRequest())

        status(response) mustBe FORBIDDEN
      }
    }

    "getAmlsDetails" should {

      val utr = Utr("7000000002")

      def doRequest(): Future[Result] = controller.getAmlsDetails(utr)(FakeRequest()
        .withHeaders(CONTENT_TYPE -> "application/json"))

      "not an agent or stride should return forbidden" in {

        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithoutIrSAAgent and None and None)
        }

        val response = doRequest()
        status(response) mustBe FORBIDDEN

      }

      "an agent with non existing utr record should return not found" in {

        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))
          mockGetAmls(utr)(None)
        }

        val response = doRequest()
        status(response) mustBe NOT_FOUND

      }

      "an agent with existing aml record should return amls details" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))
          mockGetAmls(utr)(Some(UkAmlsDetails("supervisory", membershipNumber = Some("0123456789"), appliedOn = None, membershipExpiresOn = Some(LocalDate.now()))))
        }

        val response = doRequest()
        status(response) mustBe OK
      }

      "a stride user with non existing utr record user should return not found" in {

        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithStride and None and Some(Credentials("", "PrivilegedApplication")))
          mockGetAmls(utr)(None)
        }

        val response = doRequest()
        status(response) mustBe NOT_FOUND
      }

      "a stride user with existing aml record should return amls details" in {
        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithStride and None and Some(Credentials("", "PrivilegedApplication")))
          mockGetAmls(utr)(Some(UkAmlsDetails("abc", membershipNumber = Some("001"), appliedOn = None, membershipExpiresOn = Some(LocalDate.now()))))
        }

        val response = doRequest()
        status(response) mustBe OK
      }

    }

    "storeAmlsDetails" should {

      val amlsDetails = UkAmlsDetails("supervisory", membershipNumber = Some("0123456789"), appliedOn = None, membershipExpiresOn = Some(LocalDate.now()))
      val createAmlsRequest = CreateAmlsRequest(testUtr, amlsDetails, AmlsSources.Subscription)

      def doRequest(createAmlsRequest: CreateAmlsRequest = createAmlsRequest) =
        controller.storeAmlsDetails()(FakeRequest()
          .withJsonBody(Json.toJson(createAmlsRequest))
          .withHeaders(CONTENT_TYPE -> "application/json")
        )

      "store amlsDetails successfully in mongo" in {

        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))
          mockCreateAmls(createAmlsRequest)(Right(()))
        }
        val response = doRequest()
        status(response) mustBe CREATED
      }

      "return bad_request if the utr is not valid" in {

        val amlsRequestWithInvalidUtr = createAmlsRequest.copy(utr = Utr("61122334455"))

        mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))

        val response = doRequest(amlsRequestWithInvalidUtr)
        status(response) mustBe BAD_REQUEST
      }

      "handle mongo errors during storing amlsDetails" in {

        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))
          mockCreateAmls(createAmlsRequest)(Left(AmlsUnexpectedMongoError))
        }
        val response = doRequest()
        status(response) mustBe INTERNAL_SERVER_ERROR
      }

      "handle invalid amlsDetails json case in the request" in {

        mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))

        val response = controller.storeAmlsDetails()(FakeRequest().withJsonBody(Json.toJson("""{"invalid": "amls-json"}""")).withHeaders(CONTENT_TYPE -> "application/json"))

        status(response) mustBe BAD_REQUEST
      }

      "handle no json case in the request" in {

        mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))

        val response = controller.storeAmlsDetails()(FakeRequest().withHeaders(CONTENT_TYPE -> "application/json"))

        status(response) mustBe BAD_REQUEST
      }

      "accept registered AMLS details without a date (APB-5382)" in {
        val amlsDetailsNoDateR = UkAmlsDetails("supervisoryBody", membershipNumber = Some("0123456789"), appliedOn = None, membershipExpiresOn = None)
        val createAmlsRequestNoDateR = CreateAmlsRequest(testUtr, amlsDetailsNoDateR, AmlsSources.Subscription)

        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))
          mockCreateAmls(createAmlsRequestNoDateR)(Right(()))
        }

        val responseR = doRequest(createAmlsRequestNoDateR)

        status(responseR) mustBe CREATED
      }

      "accept pending AMLS details without a date (APB-5382)" in {
        val amlsDetailsNoDateL = UkAmlsDetails("supervisoryBody", membershipNumber = Some(testValidApplicationReferenceNumber), appliedOn = None, membershipExpiresOn = None)
        val createAmlsRequestNoDateL = CreateAmlsRequest(testUtr, amlsDetailsNoDateL, AmlsSources.Subscription)

        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))
          mockCreateAmls(createAmlsRequestNoDateL)(Right(()))
        }

        val responseL = doRequest(createAmlsRequestNoDateL)

        status(responseL) mustBe CREATED
      }

      "accept pending AMLS details without a reference number" in {
        val amlsDetailsNoDateL = UkAmlsDetails("supervisoryBody", membershipNumber = None, appliedOn = Some(LocalDate.now()), membershipExpiresOn = None)
        val createAmlsRequestNoDateL = CreateAmlsRequest(testUtr, amlsDetailsNoDateL, AmlsSources.Subscription)

        inSequence {
          mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))
          mockCreateAmls(createAmlsRequestNoDateL)(Right(()))
        }

        val responseL = doRequest(createAmlsRequestNoDateL)

        status(responseL) mustBe CREATED
      }
    }

    "updateAmlsDetails" should {

      val utr = Utr("7000000002")
      val arn = Arn("AARN0000002")

      def doRequest(): Future[Result] =
        controller.updateAmlsDetails(utr)(FakeRequest()
          .withJsonBody(Json.toJson(arn))
          .withHeaders(CONTENT_TYPE -> "application/json")
        )

      "update existing amlsDetails successfully in mongo" in {

        inSequence {
          mockAgentAuth()(Right(()))
          mockUpdateAmls(utr, arn)(Right(UkAmlsDetails("supervisory", membershipNumber = Some("0123456789"), appliedOn = None, membershipExpiresOn = Some(LocalDate.now()))))
        }
        val response = doRequest()
        status(response) mustBe OK
        contentAsString(response) must include("supervisory")
      }

      "handle mongo errors during updating amls with Arn" in {

        inSequence {
          mockAgentAuth()(Right(()))
          mockUpdateAmls(utr, arn)(Left(AmlsUnexpectedMongoError))
        }
        val response = doRequest()
        status(response) mustBe INTERNAL_SERVER_ERROR
      }

      "handle allow duplicate ARN errors from mongo" in {
        inSequence {
          mockAgentAuth()(Right(()))
          mockUpdateAmls(utr, arn)(Left(UniqueKeyViolationError))
        }
        val response = doRequest()
        status(response) mustBe BAD_REQUEST
      }

      "handle Arns which don't match the ARN pattern json case in the request" in {

        mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))

        val response = controller.storeAmlsDetails()(FakeRequest().withJsonBody(Json.toJson("""{"invalid": "amls-json"}""")).withHeaders(CONTENT_TYPE -> "application/json"))

        status(response) mustBe BAD_REQUEST
      }

      "handle invalid Arn json case in the request" in {

        mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))

        val response = controller.storeAmlsDetails()(FakeRequest().withJsonBody(Json.toJson("""{"invalid": "amls-json"}""")).withHeaders(CONTENT_TYPE -> "application/json"))

        status(response) mustBe BAD_REQUEST
      }

      "handle no json case in the request" in {

        mockAuthWithNoRetrievals(allEnrolments and affinityGroup and credentials)(enrolmentsWithNoIrSAAgent and Some(AffinityGroup.Agent) and Some(Credentials("", "GovernmentGateway")))

        val response = controller.storeAmlsDetails()(FakeRequest().withHeaders(CONTENT_TYPE -> "application/json"))

        status(response) mustBe BAD_REQUEST
      }

    }

    "storeOverseasAmlsDetails" should {
      val arn = Arn("AARN0000002")

      val amlsDetails = OverseasAmlsDetails("supervisoryBody", Some("0123456789"))
      val overseasAmlsEntity = OverseasAmlsEntity(arn, amlsDetails, AmlsSources.ManageAccountUpdate)

      def doRequest(request: OverseasAmlsEntity = overseasAmlsEntity) =
        controller.storeOverseasAmlsDetails(FakeRequest()
          .withJsonBody(Json.toJson(request))
          .withHeaders(CONTENT_TYPE -> "application/json")
        )

      "store amlsDetails successfully in mongo" in {

        inSequence {
          mockAgentAuth()(Right(()))
          mockCreateOverseasAmls(overseasAmlsEntity)(Right(()))
        }
        val response = doRequest()
        status(response) mustBe CREATED
      }

      "return bad_request if the ARN is not valid" in {

        val amlsRequestWithInvalidArn = overseasAmlsEntity.copy(arn = Arn("61122334455"))

        mockAgentAuth()(Right(()))

        val response = doRequest(amlsRequestWithInvalidArn)
        status(response) mustBe BAD_REQUEST
      }

      "return conflict if the record already available in the database" in {

        inSequence {
          mockAgentAuth()(Right(()))
          mockCreateOverseasAmls(overseasAmlsEntity)(Left(AmlsRecordExists))
        }

        val response = doRequest()
        status(response) mustBe CONFLICT
      }

      "handle mongo errors during storing amlsDetails" in {

        inSequence {
          mockAgentAuth()(Right(Credentials("", "")))
          mockCreateOverseasAmls(overseasAmlsEntity)(Left(AmlsUnexpectedMongoError))
        }
        val response = doRequest()
        status(response) mustBe INTERNAL_SERVER_ERROR
      }

      "handle invalid amlsDetails json case in the request" in {

        mockAgentAuth()(Right(()))

        val response = controller.storeOverseasAmlsDetails(FakeRequest()
          .withJsonBody(Json.toJson("""{"invalid": "amls-json"}""")).withHeaders(CONTENT_TYPE -> "application/json"))

        status(response) mustBe BAD_REQUEST
      }

      "handle no json case in the request" in {

        mockAgentAuth()(Right(()))

        val response = controller.storeOverseasAmlsDetails(FakeRequest().withHeaders(CONTENT_TYPE -> "application/json"))

        status(response) mustBe BAD_REQUEST
      }
    }
  }
}
