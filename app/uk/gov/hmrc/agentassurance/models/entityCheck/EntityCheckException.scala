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

package uk.gov.hmrc.agentassurance.models.entitycheck

import play.api.http.Status

sealed trait EmailCheckExceptions {
  def failedChecksText:String
}

sealed trait EntityCheckException extends Product with Serializable


sealed trait DeceasedCheckException extends EntityCheckException

object DeceasedCheckException {

  case class CitizenConnectorRequestFailed(code: Int) extends DeceasedCheckException

  case object EntityDeceasedCheckFailed extends DeceasedCheckException with EmailCheckExceptions {
    override val failedChecksText: String = "Agent is deceased."
  }
}


sealed trait RefusalCheckException extends EntityCheckException

object RefusalCheckException {
  case class ConnectorRequestFailed(code: Status) extends RefusalCheckException

  case object AgentIsOnRefuseToDealList extends RefusalCheckException with EmailCheckExceptions {
    override val failedChecksText: String = "Agent is on the 'Refuse To Deal With' list."
  }
}