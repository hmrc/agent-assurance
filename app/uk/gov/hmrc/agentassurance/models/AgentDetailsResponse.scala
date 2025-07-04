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

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Writes
import uk.gov.hmrc.agentmtdidentifiers.model.Utr

case class AgentDetailsResponse(
  agencyDetails: AgencyDetails,
  optUtr: Option[Utr]
)

object AgentDetailsResponse {
  implicit val agentDetailsResponseWrites: Writes[AgentDetailsResponse] =
    new Writes[AgentDetailsResponse] {
      override def writes(agentDetailsResponse: AgentDetailsResponse): JsValue =
        if (agentDetailsResponse.optUtr.isDefined)
          Json.obj("agencyDetails" -> agentDetailsResponse.agencyDetails, "utr" -> agentDetailsResponse.optUtr)
        else
          Json.obj("agencyDetails" -> agentDetailsResponse.agencyDetails)
    }

}
