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

enum EntityCheckException(val exceptionMessage: String):

  case CitizenConnectorRequestFailed(code: Int)
  extends EntityCheckException(s"The request to fetch the citizen details failed with a response status code of $code")
  case EntityDeceasedCheckFailed
  extends EntityCheckException("Agent is deceased")
  case AgentIsOnRefuseToDealList
  extends EntityCheckException("Agent is on the 'Refuse To Deal With' list")

end EntityCheckException
