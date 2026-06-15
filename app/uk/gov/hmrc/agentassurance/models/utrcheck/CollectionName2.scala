/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentassurance.models.utrcheck

enum CollectionName2(val textValue: String):

  case ManuallyAssured
  extends CollectionName2("manually-assured")
  case RefusalToDealWith
  extends CollectionName2("refusal-to-deal-with")

end CollectionName2

object CollectionName2:

  def fromString(textValue: String): Option[CollectionName2] =
    textValue.toLowerCase match {
      case "refusal-to-deal-with" => Some(RefusalToDealWith)
      case "manually-assured" => Some(ManuallyAssured)
      case _ => None
    }

end CollectionName2
