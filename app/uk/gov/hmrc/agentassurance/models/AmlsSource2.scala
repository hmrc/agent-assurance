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
import play.api.libs.json.JsObject
import play.api.libs.json.JsPath
import play.api.libs.json.JsResult
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.libs.json.OWrites
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import uk.gov.hmrc.agentassurance.models

enum AmlsSource2:

  case Subscription, AutomaticUpdate, ManageAccountUpdate

end AmlsSource2

object AmlsSource2:

//  implicit val AmlsSource2Format: Format[AmlsSource2] =
//    new Format[AmlsSource2] {
//
//      def reads(json: JsValue): JsResult[AmlsSource2] =
//
//        json match
//          case JsString(s) =>
//
//            s.toLowerCase() match
//              case "subscription" => JsSuccess(AmlsSource2.Subscription)
//              case "automatic update" => JsSuccess(AmlsSource2.AutomaticUpdate)
//              case "manage account update" => JsSuccess(AmlsSource2.ManageAccountUpdate)
//              case _ => JsError("Invalid AMLS source string")
//            end match
//
//          case _ => JsError("Expected JsString")
//        end match
//
//      end reads
//
//      def writes(amlsSource: AmlsSource2): JsValue = JsString(amlsSource.toString.toLowerCase())
//
//    }

  implicit val subscriptionFormat: OFormat[Subscription.type] = OFormat[Subscription.type](
    Reads[Subscription.type] {
      case JsString(s) =>

        s.toLowerCase() match
          case "subscription" => JsSuccess(AmlsSource2.Subscription)
          case _ => JsError("Invalid AMLS source string")
        end match

      case _ => JsError("Expected JsString")
    },
    OWrites[Subscription.type] {
      amlsSource => Json.obj("amlsSource" -> "subscription")
    }
  )

  implicit val automaticUpdateFormat: OFormat[AutomaticUpdate.type] = OFormat[AutomaticUpdate.type](
    Reads[AutomaticUpdate.type] {
      case JsString(s) =>

        s.toLowerCase() match
          case "automatic update" => JsSuccess(AmlsSource2.AutomaticUpdate)
          case _ => JsError("Invalid AMLS source string")
        end match

      case _ => JsError("Expected JsString")
    },
    OWrites[AutomaticUpdate.type] {
      amlsSource => Json.obj("amlsSource" -> "automatic update")
    }
  )

  implicit val manageAccountUpdateFormat: OFormat[ManageAccountUpdate.type] = OFormat[ManageAccountUpdate.type](
    Reads[ManageAccountUpdate.type] {
      case JsString(s) =>

        s.toLowerCase() match
          case "manage account update" => JsSuccess(AmlsSource2.ManageAccountUpdate)
          case _ => JsError("Invalid AMLS source string")
        end match

      case _ => JsError("Expected JsString")
    },
    OWrites[ManageAccountUpdate.type] {
      amlsSource => Json.obj("amlsSource" -> "manage account update")
    }
  )

  implicit val amlsSource2Format: OFormat[AmlsSource2] = Json.format[AmlsSource2]

  //  implicit val amlsSource2Writes: Writes[AmlsSource2] = Writes {
//    case Subscription => Json.obj("amlsSource" -> "subscription")
//    case AutomaticUpdate => Json.obj("amlsSource" -> "automatic update")
//    case ManageAccountUpdate => Json.obj("amlsSource" -> "manage account update")
//  }
//
//  implicit val amlsSource2Reads: Reads[AmlsSource2] = Reads {
//    case JsString(value) =>
//
//      value.toLowerCase() match
//        case "subscription" => JsSuccess(AmlsSource2.Subscription)
//        case "automatic update" => JsSuccess(AmlsSource2.AutomaticUpdate)
//        case "manage account update" => JsSuccess(AmlsSource2.ManageAccountUpdate)
//        case _ => JsError("Invalid AMLS source string")
//      end match
//
//    case _ => JsError("Expected JsString")
//  }

end AmlsSource2
