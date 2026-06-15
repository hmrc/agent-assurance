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

sealed trait AmlsSource

object AmlsSource {

  case object Subscription
  extends AmlsSource
  case object AutomaticUpdate
  extends AmlsSource
  case object ManageAccountUpdate
  extends AmlsSource

  implicit val formatAmlsSource: Format[AmlsSource] =
    new Format[AmlsSource] {
      override def reads(json: JsValue): JsResult[AmlsSource] =
        json match {
          case JsString("Subscription") => JsSuccess(Subscription)
          case JsString("AutomaticUpdate") => JsSuccess(AutomaticUpdate)
          case JsString("ManageAccountUpdate") => JsSuccess(ManageAccountUpdate)
          case _ => JsError("error.expected.amlsSource")
        }

      override def writes(amlsSource: AmlsSource): JsValue = JsString(amlsSource.toString)
    }

}
