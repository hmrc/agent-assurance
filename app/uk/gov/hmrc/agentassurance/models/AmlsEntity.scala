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

import java.time.Clock
import java.time.Instant
import java.time.LocalDate

import play.api.libs.json.Format
import play.api.libs.json.Json
import uk.gov.hmrc.agentassurance.models.Arn
import uk.gov.hmrc.agentassurance.models.Utr

sealed trait AmlsEntity

case class UkAmlsEntity(
  utr: Option[Utr],
  amlsDetails: UkAmlsDetails,
  arn: Option[Arn] = None,
  createdOn: LocalDate,
  updatedArnOn: Option[LocalDate] = None,
  amlsSource: AmlsSource
)
extends AmlsEntity

object UkAmlsEntity {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val jsonReads: Reads[UkAmlsEntity] =
    (__ \ "utr")
      .readNullable[Utr]
      .and((__ \ "amlsDetails").read[UkAmlsDetails])
      .and((__ \ "arn").readNullable[Arn])
      .and((__ \ "createdOn").read[LocalDate])
      .and((__ \ "updatedArnOn").readNullable[LocalDate])
      .and((__ \ "amlsSource").readWithDefault[AmlsSource](AmlsSource.Subscription))(UkAmlsEntity.apply _)

  val jsonWrites: OWrites[UkAmlsEntity] = Json.writes[UkAmlsEntity]

  implicit val amlsEntityFormat: OFormat[UkAmlsEntity] = OFormat(jsonReads, jsonWrites)

}

case class OverseasAmlsEntity(
  arn: Arn,
  amlsDetails: OverseasAmlsDetails,
  createdDate: Option[Instant]
)
extends AmlsEntity {
  def withDefaultCreatedDate(implicit clock: Clock): OverseasAmlsEntity = copy(createdDate = Some(createdDate.getOrElse(Instant.now(clock))))
}

object OverseasAmlsEntity {
  implicit val format: Format[OverseasAmlsEntity] = Json.format[OverseasAmlsEntity]
}
