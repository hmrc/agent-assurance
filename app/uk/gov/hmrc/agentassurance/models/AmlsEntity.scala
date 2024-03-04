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

import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}

import java.time.LocalDate


case class AmlsEntity(utr: Utr,
                      amlsDetails: UkAmlsDetails,
                      arn: Option[Arn] = None,
                      createdOn: LocalDate,
                      updatedArnOn: Option[LocalDate] = None,
                      amlsSource: AmlsSource)

object AmlsEntity {
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
  import AmlsSources._


//  implicit val amlsEntityFormat: OFormat[AmlsEntity] = {
//
//    @SuppressWarnings(Array("org.wartremover.warts.Any"))
//    implicit val jsonReads: Reads[AmlsEntity] = (
//      (__ \ "utr").read[Utr]  and
//        (__ \ "amlsDetails").read[UkAmlsDetails]  and
//        (__ \ "arn").readNullable[Arn]  and
//        (__ \ "createdOn").read[LocalDate]  and
//        (__ \ "updatedArnOn").readNullable[LocalDate]  and
//        ((__ \ "amlsSource").read[AmlsSource].orElse(Reads.pure(AmlsSources.Subscription)))
//      )(AmlsEntity.apply _)
//
//    Json.format[AmlsEntity]
//  }


   val jsonReads: Reads[AmlsEntity] = (
    (__ \ "utr").read[Utr]  and
      (__ \ "amlsDetails").read[UkAmlsDetails]  and
      (__ \ "arn").readNullable[Arn]  and
      (__ \ "createdOn").read[LocalDate]  and
      (__ \ "updatedArnOn").readNullable[LocalDate]  and
      (__ \ "amlsSource").readWithDefault[AmlsSource](AmlsSources.Subscription)
    )(AmlsEntity.apply _)


   val jsonWrites: OWrites[AmlsEntity] = Json.writes[AmlsEntity]

  implicit val amlsEntityFormat: Format[AmlsEntity] = Format(jsonReads, jsonWrites)

//  implicit val amlsEntityFormat: OFormat[AmlsEntity] = Json.format[AmlsEntity]


}
