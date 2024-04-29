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


import play.api.libs.json.{Json, OWrites, Reads}
import uk.gov.hmrc.agentmtdidentifiers.model.{SuspensionDetails, Utr}

case class AgentDetailsDesCheckResponse(
                                         uniqueTaxReference: Option[Utr],
                                         isAnIndividual: Option[Boolean],
                                         suspensionDetails: Option[SuspensionDetails]
)

object AgentDetailsDesCheckResponse {
  implicit val suspensionDetailsRead: Reads[SuspensionDetails] = Json.reads
  implicit val agentDetailsDesCheckResponseRead: Reads[AgentDetailsDesCheckResponse] = Json.reads

  implicit val suspensionDetailsWrites: OWrites[SuspensionDetails] = Json.writes
  implicit val agentDetailsDesCheckResponseWrites: OWrites[AgentDetailsDesCheckResponse] = Json.writes


//  implicit val agentDetailsDesCheckResponseFormat: Format[AgentDetailsDesCheckResponse] = Json.format[AgentDetailsDesCheckResponse]

}
