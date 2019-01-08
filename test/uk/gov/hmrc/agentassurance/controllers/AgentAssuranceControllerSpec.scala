/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.LocalDate

import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.agentassurance.connectors.{DesConnector, EnrolmentStoreProxyConnector}
import uk.gov.hmrc.agentassurance.models
import uk.gov.hmrc.agentassurance.models.AmlsError.{AmlsRecordExists, AmlsUnexpectedMongoError, UniqueKeyViolationError}
import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.agentassurance.repositories.{AmlsRepository, OverseasAmlsRepository}
import uk.gov.hmrc.agentassurance.util.toFuture
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, EmptyRetrieval, Retrieval, Retrievals}
import uk.gov.hmrc.domain.{Nino, SaAgentReference, TaxIdentifier}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AgentAssuranceControllerSpec extends PlaySpec with MockFactory with BeforeAndAfterEach {
  val desConnector = mock[DesConnector]
  val espConnector = mock[EnrolmentStoreProxyConnector]
  val authConnector = mock[AuthConnector]
  val amlsRepository = mock[AmlsRepository]
  val overseasAmlsRepository = mock[OverseasAmlsRepository]

  val controller = new AgentAssuranceController(6, 6, 6, 6,
    authConnector, desConnector, espConnector, overseasAmlsRepository, amlsRepository)

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

  private val nino = Nino("AA000000A")
  private val utr = Utr("7000000002")
  private val saAgentReference = SaAgentReference("IRSA-123")

  def mockAuth()(response: Either[String, Enrolments]) = {
    (authConnector.authorise(_: Predicate, _: Retrieval[Enrolments])(_: HeaderCarrier, _: ExecutionContext))
      .expects(AuthProviders(GovernmentGateway), Retrievals.allEnrolments, *, *)
      .returning(response.fold[Future[Enrolments]](e => Future.failed(new Exception(e)), r => toFuture(r)))
  }

  def mockAuthWithNoRetrievals[A](retrieval: Retrieval[A])(result: A) = {
    (authConnector.authorise[A](_: Predicate, _: Retrieval[A])(_: HeaderCarrier, _: ExecutionContext))
      .expects(EmptyPredicate, retrieval, *, *)
      .returning(toFuture(result))
  }

  def mockDes(ti: TaxIdentifier)(response: Either[String, Seq[SaAgentReference]]) = {
    (desConnector.getActiveCesaAgentRelationships(_: TaxIdentifier)(_: HeaderCarrier, _: ExecutionContext))
      .expects(ti, *, *)
      .returning(response.fold[Future[Seq[SaAgentReference]]](e => Future.failed(new Exception(e)), r => toFuture(r)))
  }

  def mockAgentAuth()(response: Either[String, Unit]) = {
    (authConnector.authorise(_: Predicate, _: EmptyRetrieval.type)(_: HeaderCarrier, _: ExecutionContext))
      .expects(AuthProviders(GovernmentGateway) and AffinityGroup.Agent, EmptyRetrieval, *, *)
      .returning(response.fold[Future[Unit]](e => Future.failed(new Exception(e)), r => toFuture(r)))
  }

  def mockCreateAmls(createAmlsRequest: CreateAmlsRequest)(response: Either[AmlsError, Unit]) = {
    (amlsRepository.createOrUpdate(_: CreateAmlsRequest)(_: ExecutionContext))
      .expects(createAmlsRequest, *)
      .returning(toFuture(response))
  }

  def mockUpdateAmls(utr: Utr, arn: Arn)(response: Either[AmlsError, AmlsDetails]) = {
    (amlsRepository.updateArn(_: Utr, _: Arn)(_: ExecutionContext))
      .expects(utr, arn, *)
      .returning(toFuture(response))
  }

  def mockCreateOverseasAmls(amlsEntity: OverseasAmlsEntity)(response: Either[AmlsError, Unit]) = {
    (overseasAmlsRepository.create(_: OverseasAmlsEntity)(_: ExecutionContext))
      .expects(amlsEntity, *)
      .returning(toFuture(response))
  }

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
          mockDes(nino)(Right(Seq(saAgentReference)))
        }

        val response = controller.activeCesaRelationshipWithNino(nino, saAgentReference)(FakeRequest())
        status(response) mustBe OK
      }

      "return FORBIDDEN when called with a valid NINO that is not active in CESA" in {
        inSequence {
          mockAuthWithNoRetrievals(EmptyRetrieval)(())
          mockDes(nino)(Right(Seq.empty))
        }

        val response = controller.activeCesaRelationshipWithNino(nino, saAgentReference)(FakeRequest())
        status(response) mustBe FORBIDDEN
      }

      "return FORBIDDEN when called with a valid NINO that is active in CESA but with a different IRAgentReference" in {
        inSequence {
          mockAuthWithNoRetrievals(EmptyRetrieval)(())
          mockDes(nino)(Right(Seq(SaAgentReference("IRSA-456"))))
        }
        val response = controller.activeCesaRelationshipWithNino(nino, saAgentReference)(FakeRequest())

        status(response) mustBe FORBIDDEN
      }
    }

    "activeCesaRelationship is called with UTR" should {
      "return OK where the user provides a valid UTR and saAgentReference nad has an active relationship in CESA" in {
        inSequence {
          mockAuthWithNoRetrievals(EmptyRetrieval)(())
          mockDes(utr)(Right(Seq(saAgentReference)))
        }
        val response = controller.activeCesaRelationshipWithUtr(utr, saAgentReference)(FakeRequest())

        status(response) mustBe OK
      }

      "return FORBIDDEN when called with a valid UTR that is not active in CESA" in {
        inSequence {
          mockAuthWithNoRetrievals(EmptyRetrieval)(())
          mockDes(utr)(Right(Seq.empty))
        }
        val response = controller.activeCesaRelationshipWithUtr(utr, saAgentReference)(FakeRequest())
        status(response) mustBe FORBIDDEN
      }

      "return FORBIDDEN when called with a valid UTR that is active in CESA but with a different IRAgentReference" in {
        inSequence {
          mockAuthWithNoRetrievals(EmptyRetrieval)(())
          mockDes(utr)(Right(Seq(SaAgentReference("IRSA-456"))))
        }
        val response = controller.activeCesaRelationshipWithUtr(utr, saAgentReference)(FakeRequest())

        status(response) mustBe FORBIDDEN
      }
    }

    "storeAmlsDetails" should {

      val amlsDetails = AmlsDetails("supervisoryBody", "0123456789", LocalDate.now())
      val createAmlsRequest = CreateAmlsRequest(utr, amlsDetails)

      def doRequest(createAmlsRequest: CreateAmlsRequest = createAmlsRequest) =
        controller.storeAmlsDetails()(FakeRequest()
          .withJsonBody(Json.toJson(createAmlsRequest))
          .withHeaders(CONTENT_TYPE -> "application/json")
        )

      "store amlsDetails successfully in mongo" in {

        inSequence {
          mockAgentAuth()(Right(()))
          mockCreateAmls(createAmlsRequest)(Right(()))
        }
        val response = doRequest()
        status(response) mustBe CREATED
      }

      "return bad_request if the utr is not valid" in {

        val amlsRequestWithInvalidUtr = createAmlsRequest.copy(utr = Utr("61122334455"))

        mockAgentAuth()(Right(()))

        val response = doRequest(amlsRequestWithInvalidUtr)
        status(response) mustBe BAD_REQUEST
      }

      "handle mongo errors during storing amlsDetails" in {

        inSequence {
          mockAgentAuth()(Right(Credentials("", "")))
          mockCreateAmls(createAmlsRequest)(Left(AmlsUnexpectedMongoError))
        }
        val response = doRequest()
        status(response) mustBe INTERNAL_SERVER_ERROR
      }

      "handle invalid amlsDetails json case in the request" in {

        mockAgentAuth()(Right(()))

        val response = controller.storeAmlsDetails()(FakeRequest().withJsonBody(Json.toJson("""{"invalid": "amls-json"}""")).withHeaders(CONTENT_TYPE -> "application/json"))

        status(response) mustBe BAD_REQUEST
      }

      "handle no json case in the request" in {

        mockAgentAuth()(Right(()))

        val response = controller.storeAmlsDetails()(FakeRequest().withHeaders(CONTENT_TYPE -> "application/json"))

        status(response) mustBe BAD_REQUEST
      }

    }

    "updateAmlsDetails" should {

      val utr = Utr("7000000002")
      val arn = Arn("AARN0000002")

      def doRequest =
        controller.updateAmlsDetails(utr)(FakeRequest()
          .withJsonBody(Json.toJson(arn))
          .withHeaders(CONTENT_TYPE -> "application/json")
        )

      "update existing amlsDetails successfully in mongo" in {

        inSequence {
          mockAgentAuth()(Right(()))
          mockUpdateAmls(utr, arn)(Right(AmlsDetails("supervisory", "123", LocalDate.now())))
        }
        val response = doRequest
        status(response) mustBe OK
        contentAsString(response) must include("supervisory")
      }

      "handle mongo errors during updating amls with Arn" in {

        inSequence {
          mockAgentAuth()(Right(()))
          mockUpdateAmls(utr, arn)(Left(AmlsUnexpectedMongoError))
        }
        val response = doRequest
        status(response) mustBe INTERNAL_SERVER_ERROR
      }

      "handle allow duplicate ARN errors from mongo" in {
        inSequence {
          mockAgentAuth()(Right(()))
          mockUpdateAmls(utr, arn)(Left(UniqueKeyViolationError))
        }
        val response = doRequest
        status(response) mustBe BAD_REQUEST
      }

      "handle Arns which don't match the ARN pattern json case in the request" in {

        mockAgentAuth()(Right(()))

        val response = controller.storeAmlsDetails()(FakeRequest().withJsonBody(Json.toJson("""{"invalid": "amls-json"}""")).withHeaders(CONTENT_TYPE -> "application/json"))

        status(response) mustBe BAD_REQUEST
      }

      "handle invalid Arn json case in the request" in {

        mockAgentAuth()(Right(()))

        val response = controller.storeAmlsDetails()(FakeRequest().withJsonBody(Json.toJson("""{"invalid": "amls-json"}""")).withHeaders(CONTENT_TYPE -> "application/json"))

        status(response) mustBe BAD_REQUEST
      }

      "handle no json case in the request" in {

        mockAgentAuth()(Right(()))

        val response = controller.storeAmlsDetails()(FakeRequest().withHeaders(CONTENT_TYPE -> "application/json"))

        status(response) mustBe BAD_REQUEST
      }

    }

    "storeOverseasAmlsDetails" should {
      val arn = Arn("AARN0000002")

      val amlsDetails = OverseasAmlsDetails("supervisoryBody", Some("0123456789"))
      val overseasAmlsEntity = OverseasAmlsEntity(arn, amlsDetails)

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
