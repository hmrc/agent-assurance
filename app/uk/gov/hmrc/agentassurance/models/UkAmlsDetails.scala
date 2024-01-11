/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.{Format, Json}

import java.time.LocalDate

case class UkAmlsDetails(supervisoryBody: String,
                         membershipNumber: Option[String],
                         amlsSafeId: Option[String] = None,
                         agentBPRSafeId: Option[String] = None,
                         appliedOn: Option[LocalDate],
                         membershipExpiresOn: Option[LocalDate]) extends AmlsDetails {
  val isPending: Boolean = membershipExpiresOn.isEmpty
  val isRegistered: Boolean = !isPending
}

object UkAmlsDetails {
  implicit val format: Format[UkAmlsDetails] = Json.format[UkAmlsDetails]
}

