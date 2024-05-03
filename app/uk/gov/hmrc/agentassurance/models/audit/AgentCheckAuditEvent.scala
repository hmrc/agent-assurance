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

import java.time.Instant
import java.util.UUID

import play.api.libs.json.JsString
import uk.gov.hmrc.agentassurance.models.AgentCheckOutcome
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

class AgentCheckAuditEvent(arn: Arn, utr: Option[Utr], agentCheckOutcomes: Seq[AgentCheckOutcome])
    extends ExtendedDataEvent(
      auditSource = "agent-assurance",
      auditType = "Agent Check",
      eventId = UUID.randomUUID().toString,
      tags = Map.empty,
      detail = JsString(""),
      generatedAt = Instant.now()
    )
