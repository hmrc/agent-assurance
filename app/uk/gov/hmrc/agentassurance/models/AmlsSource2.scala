/*
 * Copyright 2026 HM Revenue & Customs
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

enum AmlsSource2:

  case Subscription, AutomaticUpdate, ManageAccountUpdate

end AmlsSource2

object AmlsSource2:

  implicit val amlsSource2Format: Format[AmlsSource2] =
    new Format[AmlsSource2]:
      override def reads(json: JsValue): JsResult[AmlsSource2] =
        json match
          case JsString(value) =>
            value.toLowerCase match
              case "subscription" => JsSuccess(AmlsSource2.Subscription)
              case "automatic update" => JsSuccess(AmlsSource2.AutomaticUpdate)
              case "manage account update" => JsSuccess(AmlsSource2.ManageAccountUpdate)
              case _ => JsError("Invalid AMLS source string")
          case _ => JsError("Expected JsString")

      override def writes(amlsSource: AmlsSource2): JsValue = JsString(
        amlsSource match
          case AmlsSource2.Subscription => "subscription"
          case AmlsSource2.AutomaticUpdate => "automatic update"
          case AmlsSource2.ManageAccountUpdate => "manage account update"
      )

end AmlsSource2
