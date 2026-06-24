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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.libs.json.__
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.json.JsonEncryption.stringEncrypterDecrypter

case class AgentDetailsDesResponse(
  uniqueTaxReference: Option[Utr],
  agencyDetails: Option[AgencyDetails],
  suspensionDetails: Option[SuspensionDetails],
  isAnIndividual: Option[Boolean],
  amlsDetails: Option[AgentRecordAmlsDetails] = None
)

object AgentDetailsDesResponse:

  implicit val agentRecordDetailsFormat: OFormat[AgentDetailsDesResponse] = Json.format[AgentDetailsDesResponse]

  def agentRecordDatabaseDetailsFormat(implicit
    crypto: Encrypter
      & Decrypter
  ): Format[AgentDetailsDesResponse] = (__ \ "uniqueTaxReference")
    .formatNullable[String](using stringEncrypterDecrypter)
    .bimap[Option[Utr]](
      _.map(Utr(_)),
      _.map(_.value)
    )
    .and((__ \ "agencyDetails").formatNullable[AgencyDetails](using AgencyDetails.agencyDetailsDatabaseFormat))
    .and((__ \ "suspensionDetails").formatNullable[SuspensionDetails])
    .and((__ \ "isAnIndividual").formatNullable[Boolean])
    .and((__ \ "amlsDetails").formatNullable[AgentRecordAmlsDetails](using AgentRecordAmlsDetails.databaseFormat))(
      AgentDetailsDesResponse.apply,
      response => (response.uniqueTaxReference, response.agencyDetails, response.suspensionDetails, response.isAnIndividual, response.amlsDetails)
    )

end AgentDetailsDesResponse
