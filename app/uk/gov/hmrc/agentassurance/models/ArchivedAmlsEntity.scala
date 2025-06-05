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

package uk.gov.hmrc.agentassurance.models

import java.time.temporal.ChronoUnit
import java.time.Instant
import java.time.LocalDate

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.Utr

case class ArchivedAmlsEntity(
  ukRecord: Boolean,
  createdAt: JsValue = Json.obj("$date" -> Instant.now().truncatedTo(ChronoUnit.SECONDS)),
  arn: Arn,
  utr: Option[Utr],
  supervisoryBody: String,
  originalCreatedOn: Option[LocalDate], // when the record was created in the other collection
  membershipNumber: Option[String],
  amlsSafeId: Option[String],
  agentBprSafeId: Option[String],
  appliedOn: Option[LocalDate], // when they applied for HMRC AMLS
  membershipExpiresOn: Option[LocalDate]
)

object ArchivedAmlsEntity {

  implicit val format: Format[ArchivedAmlsEntity] = Json.format[ArchivedAmlsEntity]

  def apply(
    arn: Arn,
    amlsEntity: AmlsEntity
  ): ArchivedAmlsEntity = {
    amlsEntity match {
      case uk: UkAmlsEntity =>
        val amlsDetails = uk.amlsDetails
        ArchivedAmlsEntity(
          ukRecord = true,
          arn = arn,
          utr = uk.utr,
          supervisoryBody = amlsDetails.supervisoryBody,
          originalCreatedOn = Option(uk.createdOn),
          membershipNumber = amlsDetails.membershipNumber,
          amlsSafeId = amlsDetails.amlsSafeId,
          agentBprSafeId = amlsDetails.agentBPRSafeId,
          appliedOn = amlsDetails.appliedOn,
          membershipExpiresOn = amlsDetails.membershipExpiresOn
        )
      case os: OverseasAmlsEntity =>
        val amlsDetails = os.amlsDetails
        ArchivedAmlsEntity(
          ukRecord = false,
          arn = arn,
          utr = None,
          supervisoryBody = amlsDetails.supervisoryBody,
          originalCreatedOn = None,
          membershipNumber = amlsDetails.membershipNumber,
          amlsSafeId = None,
          agentBprSafeId = None,
          appliedOn = None,
          membershipExpiresOn = None
        )
    }
  }

}

// for the ASA AMLS journey (using POST /amls/arn/:arn)
case class AmlsRequest(
  ukRecord: Boolean,
  supervisoryBody: String,
  membershipNumber: String,
  membershipExpiresOn: Option[LocalDate]
) {
  def toAmlsEntity(amlsRequest: AmlsRequest): AmlsDetails = {
    if (amlsRequest.ukRecord)
      UkAmlsDetails(
        supervisoryBody = amlsRequest.supervisoryBody,
        membershipNumber = Some(amlsRequest.membershipNumber),
        amlsSafeId = None,
        agentBPRSafeId = None,
        appliedOn = None,
        membershipExpiresOn = amlsRequest.membershipExpiresOn
      )
    else
      OverseasAmlsDetails(
        supervisoryBody = amlsRequest.supervisoryBody,
        membershipNumber = Some(amlsRequest.membershipNumber)
      )
  }
}

object AmlsRequest {
  implicit val format: Format[AmlsRequest] = Json.format[AmlsRequest]
}
