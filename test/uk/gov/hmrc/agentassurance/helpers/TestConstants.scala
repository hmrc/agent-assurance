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

import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.domain.{Nino, SaAgentReference}

import java.time.LocalDate

object TestConstants {

  val irSaAgentEnrolment: Set[Enrolment] = Set(
    Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", "IRSA-123")), state = "activated", delegatedAuthRule = None)
  )

  val hmrcAsAgentEnrolment: Set[Enrolment] = Set(
    Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", "ARN123")), state = "activated", delegatedAuthRule = None)
  )

  val strideEnrolment: Set[Enrolment] = Set(
    Enrolment("maintain_agent_manually_assure", Seq.empty, state = "activated", delegatedAuthRule = None)
  )

  val enrolmentsWithIrSAAgent = Enrolments(irSaAgentEnrolment)
  val enrolmentsWithNoIrSAAgent = Enrolments(hmrcAsAgentEnrolment)
  val enrolmentsWithoutIrSAAgent = Enrolments(Set.empty)
  val enrolmentsWithStride = Enrolments(strideEnrolment)

  val testNino = Nino("AA000000A")
  val testUtr = Utr("7000000002")
  val testSaAgentReference = SaAgentReference("IRSA-123")
  val testValidApplicationReferenceNumber = "XAML00000123456"
  val testArn = Arn("ARN123")
  val today = LocalDate.now

  val membershipExpiresOnDate = LocalDate.parse("2024-01-12")
  val testAmlsDetails = UkAmlsDetails("supervisory", membershipNumber = Some("0123456789"), appliedOn = None, membershipExpiresOn = Some(membershipExpiresOnDate))
  val testOverseasAmlsDetails = OverseasAmlsDetails("supervisory", membershipNumber = Some("0123456789"))
  val testUKAmlsEntity = UkAmlsEntity(Some(testUtr), testAmlsDetails, Some(testArn), today, None)
  val testOverseasAmlsEntity = OverseasAmlsEntity(testArn, testOverseasAmlsDetails)
  val testUKAmlsRequest = AmlsRequest(ukRecord = true, Some(testUtr), "supervisory", "0123456789", Some(membershipExpiresOnDate))
  val testOverseasAmlsRequest = AmlsRequest(ukRecord = false, None, "supervisory", "0123456789", Some(membershipExpiresOnDate))
}
