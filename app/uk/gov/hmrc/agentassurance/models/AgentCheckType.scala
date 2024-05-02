package uk.gov.hmrc.agentassurance.models

sealed trait AgentCheckType {
  val value: String
}

case object DeceasedCheck extends AgentCheckType {
  val value = "isDeceased"
}

case object OnRefusalListCheck extends AgentCheckType {
  val value = "onRefusalList"
}

