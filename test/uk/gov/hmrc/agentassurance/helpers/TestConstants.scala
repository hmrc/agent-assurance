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

package uk.gov.hmrc.agentassurance.helpers

import java.time.LocalDate

import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.EnrolmentIdentifier
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.domain.SaUtr

object TestConstants {

  val irSaAgentEnrolment: Set[Enrolment] = Set(
    Enrolment(
      "IR-SA-AGENT",
      Seq(EnrolmentIdentifier("IRAgentReference", "IRSA-123")),
      state = "activated",
      delegatedAuthRule = None
    )
  )

  val hmrcAsAgentEnrolment: Set[Enrolment] = Set(
    Enrolment(
      "HMRC-AS-AGENT",
      Seq(EnrolmentIdentifier("AgentReferenceNumber", "ARN123")),
      state = "activated",
      delegatedAuthRule = None
    )
  )

  val strideEnrolment: Set[Enrolment] = Set(
    Enrolment("maintain_agent_manually_assure", Seq.empty, state = "activated", delegatedAuthRule = None)
  )

  val enrolmentsWithIrSAAgent: Enrolments    = Enrolments(irSaAgentEnrolment)
  val enrolmentsWithNoIrSAAgent: Enrolments  = Enrolments(hmrcAsAgentEnrolment)
  val enrolmentsWithoutIrSAAgent: Enrolments = Enrolments(Set.empty)
  val enrolmentsWithStride: Enrolments       = Enrolments(strideEnrolment)

  val testNino: Nino                         = Nino("AA000000A")
  val testUtr: Utr                           = Utr("7000000002")
  val testSaUtr: SaUtr                       = SaUtr(testUtr.value)
  val testSaAgentReference: SaAgentReference = SaAgentReference("IRSA-123")
  val testValidApplicationReferenceNumber    = "XAML00000123456"
  val testArn: Arn                           = Arn("ARN123")
  val testArn1: Arn                          = Arn("ARN1231")
  val testArn2: Arn                          = Arn("ARN1232")
  val testArn3: Arn                          = Arn("ARN1233")
  val today: LocalDate                       = LocalDate.now

  val membershipExpiresOnDate: LocalDate = LocalDate.parse("2024-01-12")
  val testAmlsDetails: UkAmlsDetails = UkAmlsDetails(
    "supervisory",
    membershipNumber = Some("0123456789"),
    appliedOn = None,
    membershipExpiresOn = Some(membershipExpiresOnDate)
  )
  val testUKAmlsEntity: UkAmlsEntity = UkAmlsEntity(
    utr = Some(testUtr),
    amlsDetails = testAmlsDetails,
    arn = Some(testArn),
    createdOn = today,
    updatedArnOn = None,
    amlsSource = AmlsSource.Subscription
  )

  val testHmrcAmlsDetails: UkAmlsDetails = UkAmlsDetails(
    "HM Revenue and Customs (HMRC)",
    membershipNumber = Some(testValidApplicationReferenceNumber),
    appliedOn = None,
    membershipExpiresOn = Some(LocalDate.now())
  )
  val testHmrcAmlsDetailsPending: UkAmlsDetails = UkAmlsDetails(
    "HM Revenue and Customs (HMRC)",
    membershipNumber = Some(testValidApplicationReferenceNumber),
    appliedOn = Some(LocalDate.now().minusWeeks(1)),
    membershipExpiresOn = None
  )
  val testHmrcAmlsDetailsNoMembershipNumber: UkAmlsDetails = UkAmlsDetails(
    "HM Revenue and Customs (HMRC)",
    membershipNumber = None,
    appliedOn = None,
    membershipExpiresOn = Some(LocalDate.now())
  )
  val testOverseasAmlsDetails: OverseasAmlsDetails =
    OverseasAmlsDetails("supervisory", membershipNumber = Some("0123456789"))
  val testOverseasAmlsEntity: OverseasAmlsEntity = OverseasAmlsEntity(testArn, testOverseasAmlsDetails, None)
  val testUKAmlsRequest: AmlsRequest =
    AmlsRequest(ukRecord = true, "supervisory", "0123456789", Some(membershipExpiresOnDate))
  val testOverseasAmlsRequest: AmlsRequest =
    AmlsRequest(ukRecord = false, "supervisory", "0123456789", Some(membershipExpiresOnDate))

  // DES Agent Record
  val agencyDetailsUk: AgencyDetails =
    AgencyDetails(None, None, None, Some(BusinessAddress("line1", None, None, None, None, "GB")))
  val agencyDetailsOverseas: AgencyDetails =
    AgencyDetails(None, None, None, Some(BusinessAddress("line1", None, None, None, None, "US")))
  val agencyDetails: AgencyDetails = AgencyDetails(
    Some(EncryptedValue("agencyName")),
    Some("agencyEmail"),
    Some("agencyTelephone"),
    Some(BusinessAddress("addressLine1", None, None, None, None, "GB"))
  )
  val testAgentDetailsDesEmptyResponse: AgentDetailsDesResponse = AgentDetailsDesResponse(None, None, None, None)
  val testAgentDetailsDesAddressUtrResponse: AgentDetailsDesResponse = AgentDetailsDesResponse(
    uniqueTaxReference = Some(testUtr),
    agencyDetails = Some(agencyDetails),
    suspensionDetails = None,
    isAnIndividual = None
  )
  val testAgentDetailsDesOverseas: AgentDetailsDesResponse =
    testAgentDetailsDesAddressUtrResponse.copy(agencyDetails = Some(agencyDetailsOverseas))
  val testAgentDetailsDesResponse: AgentDetailsDesResponse =
    AgentDetailsDesResponse(
      uniqueTaxReference = Some(testUtr),
      agencyDetails = None,
      suspensionDetails = None,
      isAnIndividual = None
    )
  val testAgentDetailsDesResponseNoUtr: AgentDetailsDesResponse =
    testAgentDetailsDesResponse.copy(uniqueTaxReference = None)

}
