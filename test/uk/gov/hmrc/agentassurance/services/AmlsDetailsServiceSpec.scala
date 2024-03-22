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

package uk.gov.hmrc.agentassurance.services

import org.scalatest.PrivateMethodTester
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.agentassurance.helpers.TestConstants._
import uk.gov.hmrc.agentassurance.mocks._
import uk.gov.hmrc.agentassurance.models.AmlsError.AmlsUnexpectedMongoError
import uk.gov.hmrc.agentassurance.models.{AmlsStatus, AmlsSubscriptionRecord, ArchivedAmlsEntity}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmlsDetailsServiceSpec extends PlaySpec
  with PrivateMethodTester
  with MockAmlsRepository
  with MockOverseasAmlsRepository
  with MockArchivedAmlsRepository
  with MockDesConnector
  with MockAgencyDetailsService {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val service = new AmlsDetailsService(
    mockOverseasAmlsRepository,
    mockAmlsRepository,
    mockArchivedAmlsRepository,
    mockDesConnector,
    mockAgencyDetailsService)

  "getAmlsDetailsByArn" should {
    "include amls UK HMRC status " when {
      "there is no membership number" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetailsNoMembershipNumber))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        val result = service.getAmlsDetailsByArn(testArn)
        await(result) mustBe (AmlsStatus.NoAmlsDetailsUK, Some(testHmrcAmlsDetailsNoMembershipNumber))
      }

      "there is no amls subscription record" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.failed(new Exception("failed to return a record")))
        val result = service.getAmlsDetailsByArn(testArn)
        await(result) mustBe (AmlsStatus.NoAmlsDetailsUK, Some(testHmrcAmlsDetails))
      }

      "the subscription record's end date is before the amls details expiry date" in {
        val testDate = LocalDate.now().minusWeeks(2)
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Approved", "1", None, Some(testDate), None))
        )
        val result = service.getAmlsDetailsByArn(testArn)
        await(result) mustBe (AmlsStatus.ValidAmlsDetailsUK, Some(testHmrcAmlsDetails))
      }
    }
    "include amls UK HMRC status Expired " when {
      "there is a membership number, the supervisory body is HMRC, the form bundle status is `approved` and the end date is after the expiry date" in {
        val testDate = LocalDate.now().plusWeeks(2)
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Approved", "1", None, Some(testDate), None))
        )
        val result = service.getAmlsDetailsByArn(testArn)
        await(result) mustBe (AmlsStatus.ExpiredAmlsDetailsUK, Some(testHmrcAmlsDetails))
      }

      "there is a membership number, the supervisory body is HMRC, the form bundle status is `approved with conditions` and the end date is after the expiry date" in {
        val testDate = LocalDate.now().plusWeeks(2)
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("ApprovedWithConditions", "1", None, Some(testDate), None))
        )
        val result = service.getAmlsDetailsByArn(testArn)
        await(result) mustBe (AmlsStatus.ExpiredAmlsDetailsUK, Some(testHmrcAmlsDetails))
      }
    }

    "include amls UK NON HMRC status " when {
      "the expiry date is in the past" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        val result = service.getAmlsDetailsByArn(testArn)
        await(result) mustBe (AmlsStatus.ExpiredAmlsDetailsUK, Some(testAmlsDetails))
      }

      "the expiry date is in the future" in {
        val testDate = LocalDate.now().plusWeeks(2)
        mockGetAmlsDetailsByArn(testArn)(Some(testAmlsDetails.copy(membershipExpiresOn = Some(testDate))))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        val result = service.getAmlsDetailsByArn(testArn)
        await(result) mustBe (AmlsStatus.ValidAmlsDetailsUK, Some(testAmlsDetails.copy(membershipExpiresOn = Some(testDate))))
      }

      "there is no expiry date" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testAmlsDetails.copy(membershipExpiresOn = None)))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        val result = service.getAmlsDetailsByArn(testArn)
        await(result) mustBe (AmlsStatus.ExpiredAmlsDetailsUK, Some(testAmlsDetails.copy(membershipExpiresOn = None)))
      }
    }

    "include amls NON UK NON HMRC status " when {
      "the repo contains amls record" in {
        mockGetAmlsDetailsByArn(testArn)(None)
        mockGetOverseasAmlsDetailsByArn(testArn)(Some(testOverseasAmlsDetails))
        val result = service.getAmlsDetailsByArn(testArn)
        await(result) mustBe (AmlsStatus.ValidAmlsNonUK, Some(testOverseasAmlsDetails))
      }
    }

    "include amls NO AMLS Details UK status " when {
      "the repo contains amls record with pending state and HMRC body" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetailsNoMembershipNumber.copy(membershipExpiresOn = None)))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        val result = service.getAmlsDetailsByArn(testArn)
        await(result) mustBe(AmlsStatus.NoAmlsDetailsUK, Some(testHmrcAmlsDetailsNoMembershipNumber.copy(membershipExpiresOn = None)))
      }
    }

    "return Future(NoAmlsDetailsUK, None)" when {
      "when none are available for UK address" in {
        mockGetAmlsDetailsByArn(testArn)(None)
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockIsUkAddress()(true)

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe(AmlsStatus.NoAmlsDetailsUK, None)
      }
    }

    "return Future(NoAmlsDetailsNonUK, None)" when {
      "when none are available for Overseas address" in {
        mockGetAmlsDetailsByArn(testArn)(None)
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockIsUkAddress()(false)

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe(AmlsStatus.NoAmlsDetailsNonUK, None)
      }
    }

    "throw an exception" when {
      "when both are available (technically should never happen)" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(Some(testOverseasAmlsDetails))

        intercept[InternalServerException](await(service.getAmlsDetailsByArn(testArn))).message mustBe "[AmlsDetailsService][getAmlsDetailsByArn] ARN has both Overseas and UK AMLS details"
      }
    }
  }

  "getAmlsDetailsByArn calling getAmlsStatusForHmrcBody" should { // TODO - replace with the ignored tests below once Play upgrade is completed
    "return amls UK HMRC status" when {
      "there is no membership number" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetailsNoMembershipNumber))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe (AmlsStatus.NoAmlsDetailsUK, Some(testHmrcAmlsDetailsNoMembershipNumber))
      }

      "there is no amls subscription record" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.failed(new Exception("failed to return a record")))

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe (AmlsStatus.NoAmlsDetailsUK, Some(testHmrcAmlsDetails))
      }

      "the subscription record's end date is before the amls details expiry date" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)

        val testDate = LocalDate.now().minusWeeks(2)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Approved", "1", None, Some(testDate), None))
        )

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe (AmlsStatus.ValidAmlsDetailsUK, Some(testHmrcAmlsDetails))
      }

      //TODO
      "the subscription record's has no end date and there is no expiry date" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails.copy(membershipExpiresOn = None)))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)

        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Approved", "1", None, None, None))
        )

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe(AmlsStatus.NoAmlsDetailsUK, Some(testHmrcAmlsDetails.copy(membershipExpiresOn = None)))
      }

      "the subscription record's status is not Pending/Rejected/Approved" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)

        val testDate = LocalDate.now().minusWeeks(2)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Other", "1", None, Some(testDate), None))
        )

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe(AmlsStatus.ValidAmlsDetailsUK, Some(testHmrcAmlsDetails))
      }

      "the subscription record's status is not Pending/Rejected/Approved and the AMLS has expired" in {
        val testDate = LocalDate.now().minusWeeks(2)
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails.copy(membershipExpiresOn = Some(testDate))))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)

        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Other", "1", None, Some(testDate), None))
        )

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe(AmlsStatus.ExpiredAmlsDetailsUK, Some(testHmrcAmlsDetails.copy(membershipExpiresOn = Some(testDate))))
      }
    }

    "return amls HMRC status Expire status" when {
      "there is a membership number, the supervisory body is HMRC, the form bundle status is `approved` and the end date is after the expiry date" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)

        val testDate = LocalDate.now().plusWeeks(2)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Approved", "1", None, Some(testDate), None))
        )

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe (AmlsStatus.ExpiredAmlsDetailsUK, Some(testHmrcAmlsDetails))
      }

      "there is a membership number, the supervisory body is HMRC, the form bundle status is `approved with conditions` and the end date is after the expiry date" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)

        val testDate = LocalDate.now().plusWeeks(2)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("ApprovedWithConditions", "1", None, Some(testDate), None))
        )

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe (AmlsStatus.ExpiredAmlsDetailsUK, Some(testHmrcAmlsDetails))
      }
    }

    "return amls HMRC  status Pending status" when {
      "there is a membership number, the supervisory body is HMRC, the form bundle status is `approved` and the end date is after the expiry date" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)

        val testDate = LocalDate.now().plusWeeks(2)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Pending", "1", None, Some(testDate), None))
        )

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe (AmlsStatus.PendingAmlsDetails, Some(testHmrcAmlsDetails))
      }

      "there is a membership number, the supervisory body is HMRC, the form bundle status is `approved with conditions` and the end date is after the expiry date" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)

        val testDate = LocalDate.now().plusWeeks(2)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Rejected", "1", None, Some(testDate), None))
        )

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe (AmlsStatus.PendingAmlsDetailsRejected, Some(testHmrcAmlsDetails))
      }
    }
  }



  "getAmlsStatusForHmrcBody" ignore { // TODO - remove the ignore when Play upgrade is completed
    // The tests don't pass because there a bug between ScalaTest 5.1.0 and Scala 2.13 regarding the Symbol
    // Scala has changed the result of Symbol.toString method to "Symbol(methodName)" instead of "'methodName".
    // ScalaTest 5.1.0 drops the first character to create the name and reflection fails trying to find "ymbol(methodName)".
    // Upgrading ScalaTest will break the ITs until we complete the Play upgrade for this service
    val privateMethod = PrivateMethod[Future[AmlsStatus]](Symbol("getAmlsStatusForHmrcBody"))

    "return amls UK HMRC status " when {
      "there is no membership number" in {
        val result = service invokePrivate privateMethod(testHmrcAmlsDetailsNoMembershipNumber, hc)

        await(result) mustBe AmlsStatus.NoAmlsDetailsUK
      }

      "there is no amls subscription record" in {
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.failed(new Exception("failed to return a record")))

        val result = service invokePrivate privateMethod(testHmrcAmlsDetails, hc)

        await(result) mustBe AmlsStatus.NoAmlsDetailsUK
      }
      "the subscription record's end date is before the amls details expiry date" in {
        val testDate = LocalDate.now().minusWeeks(2)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Approved", "1", None, Some(testDate), None))
        )

        val result = service invokePrivate privateMethod(testHmrcAmlsDetails, hc)

        await(result) mustBe AmlsStatus.ValidAmlsDetailsUK
      }
      "the supervisory body is not HMRC" in {
        val result = service invokePrivate privateMethod(testAmlsDetails, hc)

        await(result) mustBe AmlsStatus.NoAmlsDetailsUK
      }
    }
    "return amls HMRC  status Expire status" when {
      "there is a membership number, the supervisory body is HMRC, the form bundle status is `approved` and the end date is after the expiry date" in {
        val testDate = LocalDate.now().plusWeeks(2)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Approved", "1", None, Some(testDate), None))
        )

        val result = service invokePrivate privateMethod(testHmrcAmlsDetails, hc)

        await(result) mustBe AmlsStatus.ExpiredAmlsDetailsUK
      }

      "there is a membership number, the supervisory body is HMRC, the form bundle status is `approved with conditions` and the end date is after the expiry date" in {
        val testDate = LocalDate.now().plusWeeks(2)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("ApprovedWithConditions", "1", None, Some(testDate), None))
        )

        val result = service invokePrivate privateMethod(testHmrcAmlsDetails, hc)

        await(result) mustBe AmlsStatus.ExpiredAmlsDetailsUK
      }
    }

    "return amls HMRC  status Pending status" when {
      "there is a membership number, the supervisory body is HMRC, the form bundle status is `approved` and the end date is after the expiry date" in {
        val testDate = LocalDate.now().plusWeeks(2)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Pending", "1", None, Some(testDate), None))
        )

        val result = service invokePrivate privateMethod(testHmrcAmlsDetails, hc)

        await(result) mustBe AmlsStatus.PendingAmlsDetails
      }

      "there is a membership number, the supervisory body is HMRC, the form bundle status is `approved with conditions` and the end date is after the expiry date" in {
        val testDate = LocalDate.now().plusWeeks(2)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Rejected", "1", None, Some(testDate), None))
        )

        val result = service invokePrivate privateMethod(testHmrcAmlsDetails, hc)

        await(result) mustBe AmlsStatus.PendingAmlsDetailsRejected
      }
    }
  }

  "storeAmlsRequest" should {
    "return Right(testAmlsDetails) when storing a UK AMLS record and there was no existing record" in {
      mockCreateOrUpdate(testArn, testUKAmlsEntity)(None)

      val result = await(service.storeAmlsRequest(testArn, testUKAmlsRequest))

      result mustBe Right(testAmlsDetails)
    }

    "return Right(testAmlsDetails) when UK AMLS and there is an existing AMLS record" in {
      mockCreateOrUpdate(testArn, testUKAmlsEntity)(Some(testUKAmlsEntity))
      mockCreate(ArchivedAmlsEntity(testArn, testUKAmlsEntity))(Right(()))

      val result = await(service.storeAmlsRequest(testArn, testUKAmlsRequest))

      result mustBe Right(testAmlsDetails)
    }

    "return Right(testOverseasAmlsDetails) when Overseas AMLS and there is no existing AMLS record" in {
      mockCreateOrUpdate(testOverseasAmlsEntity)(None)

      val result = await(service.storeAmlsRequest(testArn, testOverseasAmlsRequest))

      result mustBe Right(testOverseasAmlsDetails)
    }

    "return Right(testOverseasAmlsDetails) when Overseas AMLS and there is an existing AMLS record" in {
      mockCreateOrUpdate(testOverseasAmlsEntity)(Some(testOverseasAmlsEntity))
      mockCreate(ArchivedAmlsEntity(testArn, testOverseasAmlsEntity))(Right(()))

      val result = await(service.storeAmlsRequest(testArn, testOverseasAmlsRequest))

      result mustBe Right(testOverseasAmlsDetails)
    }

    "return Left(AmlsUnexpectedMongoError) when there was a problem with storing new AMLS record" in {
      mockCreateOrUpdate(testOverseasAmlsEntity)(Some(testOverseasAmlsEntity))
      mockCreate(ArchivedAmlsEntity(testArn, testOverseasAmlsEntity))(Left(AmlsUnexpectedMongoError))

      val result = await(service.storeAmlsRequest(testArn, testOverseasAmlsRequest))

      result mustBe Left(AmlsUnexpectedMongoError)
    }
  }

}
