/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.__
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.json.JsonEncryption.stringEncrypterDecrypter

case class AgentRecordAmlsDetails(
  supervisoryBody: String,
  membershipNumber: String,
  evidenceObjectReference: Option[String] = None
)

object AgentRecordAmlsDetails {

  implicit val format: Format[AgentRecordAmlsDetails] = Json.format[AgentRecordAmlsDetails]

  def databaseFormat(implicit
    crypto: Encrypter
      & Decrypter
  ): Format[AgentRecordAmlsDetails] =
    (__ \ "supervisoryBody")
      .format[String](using stringEncrypterDecrypter)
      .and((__ \ "membershipNumber").format[String](using stringEncrypterDecrypter))
      .and((__ \ "evidenceObjectReference").formatNullable[String](using stringEncrypterDecrypter))(
        AgentRecordAmlsDetails.apply,
        details => (details.supervisoryBody, details.membershipNumber, details.evidenceObjectReference)
      )

}

case class AgentRecordUpdateRequest(
  amlsDetails: Option[AgentRecordAmlsDetails],
  agencyDetails: Option[AgencyDetails] = None
)

object AgentRecordUpdateRequest {
  implicit val format: Format[AgentRecordUpdateRequest] = Json.format[AgentRecordUpdateRequest]
}
