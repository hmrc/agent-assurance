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
import uk.gov.hmrc.http.HeaderCarrier

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

  "getAmlsDetailsByArn" when {

    "there is no ASA AMLS record" should {
      "return (NoAMLSDetailsUK, None) if the agency has a UK address - Scenario #1" in {
        mockGetAmlsDetailsByArn(testArn)(None)
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockIsUkAddress()(response = true)

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe(AmlsStatus.NoAmlsDetailsUK, None)
      }
      "return (NoAMLSDetailsNonUK, None) if the agency does not have a UK address - Scenario #2" in {
        mockGetAmlsDetailsByArn(testArn)(None)
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockIsUkAddress()(response = false)

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe(AmlsStatus.NoAmlsDetailsNonUK, None)
      }
    }

    "there is a UK ASA AMLS record without HMRC as the supervisory body" should {
      "return (ExpiredAMLSDetailsUK, UkAmlsDetails) if the record has expired - Scenario #3" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)

        val result = service.getAmlsDetailsByArn(testArn)

        await(result) mustBe (AmlsStatus.ExpiredAmlsDetailsUK, Some(testAmlsDetails))
      }
      "return (ValidAMLSDetailsUK, UkAmlsDetails) if the record has not expired - Scenario #4a" in {
        val testDate = LocalDate.now().plusWeeks(1)
        mockGetAmlsDetailsByArn(testArn)(Some(testAmlsDetails.copy(membershipExpiresOn = Some(testDate))))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)

        val result = service.getAmlsDetailsByArn(testArn)

        await(result) mustBe (AmlsStatus.ValidAmlsDetailsUK, Some(testAmlsDetails.copy(membershipExpiresOn = Some(testDate))))
      }
      "return (ValidAMLSDetailsUK, UkAmlsDetails) if there is no expiry date set - Scenario #4b" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testAmlsDetails.copy(membershipExpiresOn = None)))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)

        val result = service.getAmlsDetailsByArn(testArn)

        await(result) mustBe (AmlsStatus.ValidAmlsDetailsUK, Some(testAmlsDetails.copy(membershipExpiresOn = None)))
      }
    }

    "there is a UK ASA AMLS record with HMRC as the supervisory body" should {
      "return (NoAMLSDetailsUK, UkAmlsDetails) if there is no registration number - Scenario #10" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetailsNoMembershipNumber))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)

        val result = service.getAmlsDetailsByArn(testArn)

        await(result) mustBe (AmlsStatus.NoAmlsDetailsUK, Some(testHmrcAmlsDetailsNoMembershipNumber))
      }
      "return (ExpiredAMLSDetailsUK, UkAmlsDetails) if the record has expired - Scenario #5" in {
        val testDate = LocalDate.now().minusWeeks(2)
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails.copy(membershipExpiresOn = Some(testDate))))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Approved", "1", None, Some(testDate), None))
        )

        val result = service.getAmlsDetailsByArn(testArn)

        await(result) mustBe (AmlsStatus.ExpiredAmlsDetailsUK, Some(testHmrcAmlsDetails.copy(membershipExpiresOn = Some(testDate))))
      }
      "return (ValidAMLSDetailsUK, UkAmlsDetails) if the record has not expired - Scenario #6a" in {
        val testDate = LocalDate.now().plusWeeks(2)
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Approved With Conditions", "1", None, Some(testDate), None))
        )

        val result = service.getAmlsDetailsByArn(testArn)

        await(result) mustBe(AmlsStatus.ValidAmlsDetailsUK, Some(testHmrcAmlsDetails))
      }
      "return (ValidAMLSDetailsUK, UkAmlsDetails) if there is no expiry date set - Scenario #6b" in { //TODO add scenario to docs
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Approved", "1", None, None, None))
        )

        val result = service.getAmlsDetailsByArn(testArn)

        await(result) mustBe(AmlsStatus.ValidAmlsDetailsUK, Some(testHmrcAmlsDetails))
      }
    }

    "there is a non-UK ASA AMLS record" should {
      "return (ValidAMLSNonUK, OverseasAmlsDetails) as there is no expiry date for non-Uk - Scenario #7" in {
        mockGetAmlsDetailsByArn(testArn)(None)
        mockGetOverseasAmlsDetailsByArn(testArn)(Some(testOverseasAmlsDetails))

        val result = service.getAmlsDetailsByArn(testArn)

        await(result) mustBe (AmlsStatus.ValidAmlsNonUK, Some(testOverseasAmlsDetails))
      }
    }

    "there is a pending UK ASA AMLS record with HMRC as the supervisory body" should {
      "return (PendingAMLSDetails, UkAmlsDetails) if the DES record is also 'Pending' - Scenario #8" in {
        val testDate = LocalDate.now().plusWeeks(2)
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetailsPending))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Pending", "1", None, Some(testDate), None))
        )

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe (AmlsStatus.PendingAmlsDetails, Some(testHmrcAmlsDetailsPending))
      }
      "return (PendingAMLSDetailsRejected, UkAmlsDetails) if the DES record is 'Rejected' - Scenario #9" in {
        val testDate = LocalDate.now().plusWeeks(2)
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetailsPending))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Rejected", "1", None, Some(testDate), None))
        )

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe(AmlsStatus.PendingAmlsDetailsRejected, Some(testHmrcAmlsDetailsPending))
      }
    }

    "there is a non-pending UK ASA AMLS record with HMRC as the supervisory body" should {
      "return (ExpiredAMLSDetailsUK, UkAmlsDetails) if the record has expired" in {
        val testDate = Some(LocalDate.now().minusWeeks(2))
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails.copy(membershipExpiresOn = testDate)))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Approved With Conditions", "1", None, testDate, None))
        )

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe(AmlsStatus.ExpiredAmlsDetailsUK, Some(testHmrcAmlsDetails.copy(membershipExpiresOn = testDate)))
      }
      "return (ValidAMLSDetailsUK, UkAmlsDetails) if the record has not expired" in {
        val testDate = LocalDate.now().plusWeeks(2)
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Approved With Conditions", "1", None, Some(testDate), None))
        )

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe(AmlsStatus.ValidAmlsDetailsUK, Some(testHmrcAmlsDetails))
      }
      "return (ValidAMLSDetailsUK, UkAmlsDetails) if there is no expiry date set" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.successful(
          AmlsSubscriptionRecord("Approved With Conditions", "1", None, None, None))
        )

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe(AmlsStatus.ValidAmlsDetailsUK, Some(testHmrcAmlsDetails))
      }
    }

    "unexpected failures occur" should {
      "return the exception if the ARN has both UK and non-UK AMLS data" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(Some(testOverseasAmlsDetails))

        intercept[Exception](await(service.getAmlsDetailsByArn(testArn))).getMessage mustBe "[AmlsDetailsService][getAmlsDetailsByArn] ARN has both Overseas and UK AMLS details"
      }
      "return the exception if the call to get the ASA UK amls record fails" in {
        mockGetAmlsDetailsByArnFuture(testArn)(Future.failed(new Exception("failed to return a record")))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)

        intercept[Exception](await(service.getAmlsDetailsByArn(testArn))).getMessage mustBe "failed to return a record"
      }
      "return the exception if the call to get the ASA overseas amls record fails" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails))
        mockGetOverseasAmlsDetailsByArnFuture(testArn)(Future.failed(new Exception("failed to return a record")))

        intercept[Exception](await(service.getAmlsDetailsByArn(testArn))).getMessage mustBe "failed to return a record"
      }
      "return the exception if the call to get the subscription fails" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testHmrcAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)
        mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(Future.failed(new Exception("failed to return a record")))

        intercept[Exception](await(service.getAmlsDetailsByArn(testArn))).getMessage mustBe "failed to return a record"
      }
    }

  }

  "findCorrectExpiryDate" when {
    val defaultDate = Some(LocalDate.now())

    "both expiry dates are populated" should {
      "return the DES expiry date if it is after the ASA expiry date" in {
        val des = defaultDate.map(_.plusWeeks(1))
        val asa = defaultDate
        val result = service.findCorrectExpiryDate(des, asa)

        result mustBe des
      }
      "return the ASA expiry date if it is after the DES expiry date" in {
        val des = defaultDate
        val asa = defaultDate.map(_.plusWeeks(1))
        val result = service.findCorrectExpiryDate(des, asa)

        result mustBe asa
      }
      "return the ASA expiry date if it is equal to the DES expiry date" in {
        val des = defaultDate
        val asa = defaultDate
        val result = service.findCorrectExpiryDate(des, asa)

        result mustBe asa
      }
    }

    "the DES expiry date is not populated and the ASA expiry date is populated" should {
      "return the ASA expiry date" in {
        val des = None
        val asa = defaultDate
        val result = service.findCorrectExpiryDate(des, asa)

        result mustBe asa
      }
    }

    "the DES expiry date is populated and the ASA expiry date is not populated" should {
      "return the DES expiry date" in {
        val des = defaultDate
        val asa = None
        val result = service.findCorrectExpiryDate(des, asa)

        result mustBe des
      }
    }

    "neither expiry dates are provided" should {
      "return None" in {
        service.findCorrectExpiryDate(None, None) mustBe None
      }
    }

  }

  "dateIsInThePast" when {

    "not provided with a date" should {
      "return false" in {
        service.dateIsInThePast(None) mustBe false
      }
    }

    "provided with a date that is in the future" should {
      "return false" in {
        service.dateIsInThePast(Some(LocalDate.now().plusWeeks(1))) mustBe false
      }
    }

    "provided with a date that is in the past" should {
      "return true" in {
        service.dateIsInThePast(Some(LocalDate.now().minusWeeks(1))) mustBe true
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
