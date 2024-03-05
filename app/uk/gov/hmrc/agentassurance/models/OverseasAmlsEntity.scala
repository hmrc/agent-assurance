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
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class OverseasAmlsEntity(arn: Arn,
                              amlsDetails: OverseasAmlsDetails,
                              amlsSource: Option[AmlsSource],
                              createdDate: Option[Instant])

object OverseasAmlsEntity {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._
  import AmlsSources._

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  val jsonReads: Reads[OverseasAmlsEntity] = (
    (__ \ "arn").read[Arn]  and
      (__ \ "amlsDetails").read[OverseasAmlsDetails]  and
      (__ \ "amlsSource").readNullableWithDefault[AmlsSource](Some(AmlsSources.Subscription)) and
        (__ \ "createdDate").readNullable[Instant]
    )(OverseasAmlsEntity.apply _)


  val jsonWrites: Writes[OverseasAmlsEntity] = (
    (JsPath \ "arn").write[Arn] and
      (JsPath \ "amlsDetails").write[OverseasAmlsDetails] and
      (JsPath \ "amlsSource").writeNullable[AmlsSource] and
      (JsPath \ "createdDate").writeNullable[Instant]
    )(r => (r.arn, r.amlsDetails, r.amlsSource, r.createdDate.orElse(Some(Instant.now()))))

  implicit val format: Format[OverseasAmlsEntity] = Format(jsonReads, jsonWrites)

}