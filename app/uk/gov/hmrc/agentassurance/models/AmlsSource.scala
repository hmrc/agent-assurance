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

import play.api.libs.json.*

enum AmlsSource:
  case Subscription, AutomaticUpdate, ManageAccountUpdate

object AmlsSource:

  given Format[AmlsSource] = Format(
    Reads {
      case JsString(value) =>
        try JsSuccess(AmlsSource.valueOf(value))
        catch case _: IllegalArgumentException => JsError(s"Unknown AmlsSource value: $value")
      case _ => JsError("Can only parse String")
    },
    Writes(source => JsString(source.toString))
  )

  override def toString: String =

    this match
      case Subscription => "Subscription"
      case AutomaticUpdate => "AutomaticUpdate"
      case ManageAccountUpdate => "ManageAccountUpdate"
    end match

  end toString

end AmlsSource
