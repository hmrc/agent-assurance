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

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import uk.gov.hmrc.crypto.json.JsonEncryption.stringEncrypterDecrypter
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter

case class EncryptedValue(value: String)

object EncryptedValue {
  def encrypDecryptValue(implicit crypto: Encrypter with Decrypter): Format[EncryptedValue] =
    stringEncrypterDecrypter.bimap(EncryptedValue.apply, _.value)

  implicit def getValue: Format[EncryptedValue] = implicitly[Format[String]].bimap(EncryptedValue.apply, _.value)
}

case class AgencyDetails(
    agencyName: Option[EncryptedValue],
    agencyEmail: Option[String],
    agencyTelephone: Option[String],
    agencyAddress: Option[BusinessAddress]
) {
  val hasUkAddress: Boolean = agencyAddress.exists(_.countryCode == "GB")
}
object AgencyDetails {
  val agencyDetailsFormat: OFormat[AgencyDetails] = Json.format[AgencyDetails]
  def agencyDetailsDatabaseFormat(implicit crypto: Encrypter with Decrypter): OFormat[AgencyDetails] = {
    implicit val encryptedValueFormats: Format[EncryptedValue] = EncryptedValue.encrypDecryptValue
    Json.format[AgencyDetails]
  }
}

case class BusinessAddress(
    addressLine1: String,
    addressLine2: Option[EncryptedValue],
    addressLine3: Option[String] = None,
    addressLine4: Option[String] = None,
    postalCode: Option[String],
    countryCode: String
)

object BusinessAddress {
  implicit val format: OFormat[BusinessAddress] = Json.format
}
