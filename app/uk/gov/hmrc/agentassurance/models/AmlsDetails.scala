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

package uk.gov.hmrc.agentassurance.models

import java.time.LocalDate

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}

case class AmlsDetails(supervisoryBody: String,
                       membershipNumber: String,
                       membershipExpiresOn: LocalDate
                      )

object AmlsDetails {
  implicit val amlsDetailsFormat: OFormat[AmlsDetails] = Json.format[AmlsDetails]
}

case class CreateAmlsRequest(utr: Utr, amlsDetails: AmlsDetails)

object CreateAmlsRequest {
  implicit val format: Format[CreateAmlsRequest] = Json.format[CreateAmlsRequest]
}

case class AmlsEntity(utr: Utr, amlsDetails: AmlsDetails, arn: Option[Arn] = None, createdOn: LocalDate, updatedArnOn: Option[LocalDate] = None)

object AmlsEntity {
  implicit val amlsEntityFormat: OFormat[AmlsEntity] = Json.format[AmlsEntity]
}