/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}

case class AmlsDetails(utr: Utr,
                       supervisoryBody: String,
                       membershipNumber: String,
                       membershipExpiresOn: LocalDate,
                       arn: Option[Arn]
                      )

object AmlsDetails {
  implicit val amlsDetailsFormat: OFormat[AmlsDetails] = Json.format[AmlsDetails]
}

case class AmlsEntity(amlsDetails: AmlsDetails, createdOn: LocalDate, updatedArnOn: Option[LocalDate] = None)

object AmlsEntity {
  implicit val amlsEntityFormat: OFormat[AmlsEntity] = Json.format[AmlsEntity]
}