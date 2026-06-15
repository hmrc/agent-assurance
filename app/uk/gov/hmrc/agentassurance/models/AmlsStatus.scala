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
import play.api.libs.json.JsError
import play.api.libs.json.JsResult
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue

sealed trait AmlsStatus

object AmlsStatus {

  case object NoAmlsDetailsNonUK
  extends AmlsStatus
  case object ValidAmlsNonUK
  extends AmlsStatus

  case object NoAmlsDetailsUK
  extends AmlsStatus
  case object ValidAmlsDetailsUK
  extends AmlsStatus
  case object ExpiredAmlsDetailsUK
  extends AmlsStatus

  case object PendingAmlsDetails
  extends AmlsStatus
  case object PendingAmlsDetailsRejected
  extends AmlsStatus

  implicit val formatAmlsStatus: Format[AmlsStatus] =
    new Format[AmlsStatus] {
      override def reads(json: JsValue): JsResult[AmlsStatus] =
        json match {
          case JsString("NoAmlsDetailsNonUK") => JsSuccess(NoAmlsDetailsNonUK)
          case JsString("ValidAmlsNonUK") => JsSuccess(ValidAmlsNonUK)
          case JsString("NoAmlsDetailsUK") => JsSuccess(NoAmlsDetailsUK)
          case JsString("ValidAmlsDetailsUK") => JsSuccess(ValidAmlsDetailsUK)
          case JsString("ExpiredAmlsDetailsUK") => JsSuccess(ExpiredAmlsDetailsUK)
          case JsString("PendingAmlsDetails") => JsSuccess(PendingAmlsDetails)
          case JsString("PendingAmlsDetailsRejected") => JsSuccess(PendingAmlsDetailsRejected)
          case _ => JsError("error.expected.amlsStatus")
        }

      override def writes(amlsStatus: AmlsStatus): JsValue = JsString(amlsStatus.toString)
    }

}
