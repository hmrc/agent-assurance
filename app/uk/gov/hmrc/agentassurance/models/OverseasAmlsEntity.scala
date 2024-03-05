/*
 * Copyright 2023 HM Revenue & Customs
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

import uk.gov.hmrc.agentmtdidentifiers.model.Arn

case class OverseasAmlsEntity(arn: Arn, amlsDetails: OverseasAmlsDetails, amlsSource: AmlsSource)

object OverseasAmlsEntity {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._
  import AmlsSources._
  val jsonReads: Reads[OverseasAmlsEntity] = (
    (__ \ "arn").read[Arn]  and
      (__ \ "amlsDetails").read[OverseasAmlsDetails]  and
      (__ \ "amlsSource").readWithDefault[AmlsSource](AmlsSources.Subscription)
    )(OverseasAmlsEntity.apply _)

  val jsonWrites: OWrites[OverseasAmlsEntity] = Json.writes[OverseasAmlsEntity]

  implicit val format: Format[OverseasAmlsEntity] = Format(jsonReads, jsonWrites)

}