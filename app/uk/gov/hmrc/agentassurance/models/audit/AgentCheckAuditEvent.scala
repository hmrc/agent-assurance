package uk.gov.hmrc.agentassurance.models.audit

import uk.gov.hmrc.agentassurance.models.AgentCheckOutcome
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import java.util.UUID

case class AgentCheckAuditEvent(arn: Arn,
                                utr: Option[Utr],
                                agentCheckOutcomes: Seq[AgentCheckOutcome]
                               ) extends ExtendedDataEvent(
  auditSource = "agent-assurance",
  auditType = "Agent Check",
  eventId = UUID.randomUUID().toString,
)
