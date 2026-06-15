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
import play.api.libs.json.__
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import uk.gov.hmrc.crypto.json.JsonEncryption.stringEncrypterDecrypter
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter

case class AgencyDetails(
  agencyName: Option[String],
  agencyEmail: Option[String],
  agencyTelephone: Option[String],
  agencyAddress: Option[BusinessAddress]
) {
  val hasUkAddress: Boolean = agencyAddress.exists(_.countryCode == "GB")
}
object AgencyDetails {

  implicit val agencyDetailsFormat: Format[AgencyDetails] = Json.format[AgencyDetails]

  def agencyDetailsDatabaseFormat(implicit
    crypto: Encrypter & Decrypter
  ): Format[AgencyDetails] =
    (__ \ "agencyName")
      .formatNullable[String](using stringEncrypterDecrypter)
      .and((__ \ "agencyEmail").formatNullable[String](using stringEncrypterDecrypter))
      .and((__ \ "agencyTelephone").formatNullable[String](using stringEncrypterDecrypter))
      .and((__ \ "agencyAddress").formatNullable[BusinessAddress](using BusinessAddress.businessAddressDatabaseFormat))(
        AgencyDetails.apply,
        details => (details.agencyName, details.agencyEmail, details.agencyTelephone, details.agencyAddress)
      )

}

case class BusinessAddress(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String] = None,
  addressLine4: Option[String] = None,
  postalCode: Option[String],
  countryCode: String
)

object BusinessAddress {

  implicit val format: OFormat[BusinessAddress] = Json.format

  def businessAddressDatabaseFormat(implicit
    crypto: Encrypter & Decrypter
  ): Format[BusinessAddress] = {
    (__ \ "addressLine1")
      .format[String](using stringEncrypterDecrypter)
      .and((__ \ "addressLine2").formatNullable[String](using stringEncrypterDecrypter))
      .and((__ \ "addressLine3").formatNullable[String](using stringEncrypterDecrypter))
      .and((__ \ "addressLine4").formatNullable[String](using stringEncrypterDecrypter))
      .and((__ \ "postalCode").formatNullable[String](using stringEncrypterDecrypter))
      .and((__ \ "countryCode").format[String](using stringEncrypterDecrypter))(
        BusinessAddress.apply,
        address => (address.addressLine1, address.addressLine2, address.addressLine3, address.addressLine4, address.postalCode, address.countryCode)
      )
  }

}
