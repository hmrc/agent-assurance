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

package uk.gov.hmrc.agentassurance.models.audit

import java.time.LocalDateTime

import play.api.libs.json.Json
import play.api.libs.json.OWrites
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

case class EmailData(
  failedCheck: Seq[String],
  dateChecked: LocalDateTime
)

object EmailData {
  implicit val writes: OWrites[EmailData] = Json.writes
}
case class AgentCheckFailureNotificationAuditEvent(
  agentReferenceNumber: Arn,
  utr: String,
  email: String,
  emailData: EmailData
)
extends AuditDetail {
  val auditType = "AgentCheckFailureNotificationSent"
}

object AgentCheckFailureNotificationAuditEvent {
  implicit val writes: OWrites[AgentCheckFailureNotificationAuditEvent] = Json.writes
}
