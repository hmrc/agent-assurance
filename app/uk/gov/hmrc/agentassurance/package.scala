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

package uk.gov.hmrc.agentassurance

import play.api.libs.json.Json

package object model {

  case class Property(key: String, value: String)

  case class Value(value: String) {
    def toProperty(key: String) = Property(key, this.value.replace(" ", ""))
  }

  case class ErrorBody(code: String, message: String)

  implicit val propertyFormat = Json.format[Property]
  implicit val valueFormat = Json.format[Value]
  implicit val errorBodyFormat = Json.format[ErrorBody]
}
