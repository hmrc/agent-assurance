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

import play.api.libs.json.{Format, Json, OFormat}

import java.time.LocalDate

sealed trait AmlsDetails

case class UkAmlsDetails(supervisoryBody: String,
                         membershipNumber: Option[String],
                         amlsSafeId: Option[String] = None,
                         agentBPRSafeId: Option[String] = None,
                         appliedOn: Option[LocalDate],
                         membershipExpiresOn: Option[LocalDate]) extends AmlsDetails {
  val isPending: Boolean = membershipExpiresOn.isEmpty
  val isRegistered: Boolean = !isPending
  val supervisoryBodyIsHmrc: Boolean = supervisoryBody equals "HM Revenue and Customs (HMRC)"
  val isExpired:Boolean = membershipExpiresOn.map(LocalDate.now().isAfter(_)).getOrElse(true)
}

object UkAmlsDetails {
  implicit val format: Format[UkAmlsDetails] = Json.format[UkAmlsDetails]
}

case class OverseasAmlsDetails(supervisoryBody: String, membershipNumber: Option[String] = None) extends AmlsDetails

object OverseasAmlsDetails {
  implicit val format: OFormat[OverseasAmlsDetails] = Json.format[OverseasAmlsDetails]
}

