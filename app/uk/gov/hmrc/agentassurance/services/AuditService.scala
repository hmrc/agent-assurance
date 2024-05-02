package uk.gov.hmrc.agentassurance.services

import uk.gov.hmrc.agentassurance.models.audit.AgentCheckAuditEvent
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{Inject, Singleton}

@Singleton
class AuditService @Inject()(auditConnector: AuditConnector) {

  def sendAgentCheckAuditEvent(agentCheckAuditEvent: AgentCheckAuditEvent) = auditConnector.sendEvent()

  def sendAgentCheckFailureNotification() = ???


}
