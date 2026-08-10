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

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json

import java.time.LocalDate

// for the ASA AMLS journey (using POST /amls/arn/:arn)
case class AmlsRequest(
  ukRecord: Boolean,
  supervisoryBody: String,
  membershipNumber: String,
  membershipExpiresOn: Option[LocalDate],
  evidenceObjectReference: Option[String] = None
):
  def toAmlsEntity(amlsRequest: AmlsRequest): AmlsDetails =
    if amlsRequest.ukRecord then
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
end AmlsRequest

object AmlsRequest:
  implicit val format: Format[AmlsRequest] = Json.format[AmlsRequest]
end AmlsRequest
