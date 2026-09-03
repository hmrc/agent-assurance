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

import com.mongodb.client.result.UpdateResult
import org.scalamock.scalatest.MockFactory
import org.scalatest.PrivateMethodTester
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.agentassurance.helpers.TestConstants.*
import uk.gov.hmrc.agentassurance.mocks.*
import uk.gov.hmrc.agentassurance.models.AgentRecordAmlsDetails
import uk.gov.hmrc.agentassurance.models.AgentRecordUpdateRequest
import uk.gov.hmrc.agentassurance.models.AmlsStatus
import uk.gov.hmrc.agentassurance.models.AmlsSubscriptionRecord
import uk.gov.hmrc.agentassurance.models.OverseasAmlsDetails
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmlsDetailsServiceSpec
extends PlaySpec
with PrivateMethodTester
with MockFactory
with MockAmlsRepository
with MockOverseasAmlsRepository
with MockDesConnector
with MockAgentServicesAccountConnector
with MockAppConfig:

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[Any] = FakeRequest()

  def service: AmlsDetailsService =
    new AmlsDetailsService(
      mockOverseasAmlsRepository,
      mockAmlsRepository,
      mockDesConnector,
      mockAgentServicesAccountConnector
    )

  "findCorrectExpiryDate" when:
    val defaultDate = Some(LocalDate.now())
    "both expiry dates are populated" should:
      "return the DES expiry date if it is after the ASA expiry date" in:
        val des = defaultDate.map(_.plusWeeks(1))
        val asa = defaultDate
        mockUpdateExpiryDate(testArn, des.get)(UpdateResult.acknowledged(1, null, null))

        val result = service.findCorrectExpiryDate(
          testArn,
          des,
          asa
        )

        result mustBe des
      "return the ASA expiry date if it is after the DES expiry date" in:
        val des = defaultDate
        val asa = defaultDate.map(_.plusWeeks(1))
        val result = service.findCorrectExpiryDate(
          testArn,
          des,
          asa
        )

        result mustBe asa
      "return the ASA expiry date if it is equal to the DES expiry date" in:
        val des = defaultDate
        val asa = defaultDate
        val result = service.findCorrectExpiryDate(
          testArn,
          des,
          asa
        )

        result mustBe asa

    "the DES expiry date is not populated and the ASA expiry date is populated" should:
      "return the ASA expiry date" in:
        val des = None
        val asa = defaultDate
        val result = service.findCorrectExpiryDate(
          testArn,
          des,
          asa
        )

        result mustBe asa

    "the DES expiry date is populated and the ASA expiry date is not populated" should:
      "return the DES expiry date" in:
        val des = defaultDate
        val asa = None
        mockUpdateExpiryDate(testArn, des.get)(UpdateResult.acknowledged(1, null, null))

        val result = service.findCorrectExpiryDate(
          testArn,
          des,
          asa
        )

        result mustBe des

    "neither expiry dates are provided" should:
      "return None" in:
        service.findCorrectExpiryDate(
          testArn,
          None,
          None
        ) mustBe None

  "getAmlsDetailsByArn with ASA feature enabled" should:
    "map non-GB ASA AMLS details to overseas status" in:
      mockAsaGetAgentRecord(testArn)(
        testAgentDetailsDesOverseas.copy(
          amlsDetails = Some(AgentRecordAmlsDetails(
            supervisoryBody = "SRA",
            membershipNumber = "XAML00000123456",
            evidenceObjectReference = Some("evidence-ref")
          ))
        )
      )

      val result = await(service.getAmlsDetailsByArn(testArn))

      result mustBe (
        AmlsStatus.ValidAmlsNonUK,
        Some(OverseasAmlsDetails(
          supervisoryBody = "SRA",
          membershipNumber = Some("XAML00000123456")
        ))
      )

    "use the ASA country to derive no-details status when no AMLS exists anywhere" in:
      mockAsaGetAgentRecord(testArn)(testAgentDetailsDesAddressUtrResponse.copy(amlsDetails = None))
      mockGetAmlsDetailsByArn(testArn)(None)
      mockGetOverseasAmlsDetailsByArn(testArn)(None)

      val result = await(service.getAmlsDetailsByArn(testArn))

      result mustBe (AmlsStatus.NoAmlsDetailsUK, None)

    "call DES to verify the AMLS status when the legacy UK AMLS supervisory body is HMRC" in:
      val hmrcAmlsDetails = testHmrcAmlsDetails.copy(membershipExpiresOn = Some(LocalDate.now().plusYears(1)))

      mockAsaGetAgentRecord(testArn)(testAgentDetailsDesAddressUtrResponse.copy(amlsDetails = None))
      mockGetAmlsDetailsByArn(testArn)(Some(hmrcAmlsDetails))
      mockGetOverseasAmlsDetailsByArn(testArn)(None)
      mockGetAmlsSubscriptionStatus(testValidApplicationReferenceNumber)(
        Future.successful(AmlsSubscriptionRecord(
          formBundleStatus = "Approved",
          safeId = "safeId",
          currentRegYearStartDate = None,
          currentRegYearEndDate = None,
          suspended = Some(false)
        ))
      )

      val result = await(service.getAmlsDetailsByArn(testArn))

      result mustBe (AmlsStatus.ValidAmlsDetailsUK, Some(hmrcAmlsDetails))

  "hasRenewalDateExpired" when:
    "not provided with a date" should:
      "return false" in:
        service.hasRenewalDateExpired(None) mustBe false

    "provided with a date that is in the future" should:
      "return false" in:
        service.hasRenewalDateExpired(Some(LocalDate.now().plusWeeks(1))) mustBe false

    "provided with a date that is in the past" should:
      "return true" in:
        service.hasRenewalDateExpired(Some(LocalDate.now().minusWeeks(1))) mustBe true

    "provided with today's date" should:
      "return true" in:
        service.hasRenewalDateExpired(Some(LocalDate.now())) mustBe true

  "storeAmlsRequest with ASA feature enabled" should:
    "update ASA and delete legacy Mongo records on success" in:
      mockAsaUpdateAmlsDetails(
        AgentRecordUpdateRequest(
          amlsDetails = Some(AgentRecordAmlsDetails(
            "supervisory",
            "0123456789",
            None
          )),
          agencyDetails = None
        )
      )(Future.successful(()))
      mockDeleteUkAmlsByArn(testArn)(Future.successful(()))
      mockDeleteOverseasAmlsByArn(testArn)(Future.successful(()))

      val result = await(service.storeAmlsRequest(testArn, testUKAmlsRequest))

      result mustBe Right(testAmlsDetails)

    "forward evidenceObjectReference to ASA when provided" in:
      val requestWithEvidence = testUKAmlsRequest.copy(evidenceObjectReference = Some("evidence-ref-123"))

      mockAsaUpdateAmlsDetails(
        AgentRecordUpdateRequest(
          amlsDetails = Some(AgentRecordAmlsDetails(
            "supervisory",
            "0123456789",
            Some("evidence-ref-123")
          )),
          agencyDetails = None
        )
      )(Future.successful(()))
      mockDeleteUkAmlsByArn(testArn)(Future.successful(()))
      mockDeleteOverseasAmlsByArn(testArn)(Future.successful(()))

      val result = await(service.storeAmlsRequest(testArn, requestWithEvidence))

      result mustBe Right(testAmlsDetails)

    "return success when ASA update succeeds but legacy cleanup fails" in:
      mockAsaUpdateAmlsDetails(
        AgentRecordUpdateRequest(
          amlsDetails = Some(AgentRecordAmlsDetails(
            "supervisory",
            "0123456789",
            None
          )),
          agencyDetails = None
        )
      )(Future.successful(()))
      mockDeleteUkAmlsByArn(testArn)(Future.failed(new RuntimeException("cleanup failed")))
      mockDeleteOverseasAmlsByArn(testArn)(Future.successful(()))

      val result = await(service.storeAmlsRequest(testArn, testUKAmlsRequest))

      result mustBe Right(testAmlsDetails)

end AmlsDetailsServiceSpec
