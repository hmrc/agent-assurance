package uk.gov.hmrc.agentassurance.models

case class AgentCheckOutcome(agentCheckType: AgentCheckType, isSuccessful: Boolean, failureReason: Option[String])
