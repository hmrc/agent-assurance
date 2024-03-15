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

import julienrf.json.derived
import play.api.libs.json.Format

sealed trait AmlsStatus
sealed trait AmlsOverseas
sealed trait AmlsUkHmrc
sealed trait AmlsUkNonHmrc

sealed trait NoAmlsDetails
sealed trait ValidAmls
sealed trait ExpiredAmlsDetails
sealed trait PendingAmlsDetails

object AmlsStatus {
  implicit val formatAmlsSource: Format[AmlsStatus] = derived.oformat[AmlsStatus]()

  final case object NoAmlsDetailsNonUK extends AmlsStatus with AmlsOverseas with NoAmlsDetails
  final case object ValidAmlsNonUK extends AmlsStatus with AmlsOverseas with ValidAmls

  final case object NoAmlsDetailsUK extends AmlsStatus with AmlsUkNonHmrc with NoAmlsDetails
  final case object ValidAmlsDetailsUK extends AmlsStatus with AmlsUkNonHmrc
  final case object ExpiredAmlsDetailsUK extends AmlsStatus with AmlsUkNonHmrc with ExpiredAmlsDetails

  final case object NoAmlsDetailsHmrcUK extends AmlsStatus with AmlsUkHmrc with NoAmlsDetails
  final case object ValidAmlsDetailsHmrcUK extends AmlsStatus with AmlsUkHmrc with ValidAmls
  final case object ExpiredAmlsDetailsHmrcUK extends AmlsStatus with AmlsUkHmrc with ExpiredAmlsDetails
  final case object PendingAmlsDetails  extends AmlsStatus with AmlsUkHmrc with PendingAmlsDetails
  final case object PendingAmlsDetailsRejected  extends AmlsStatus with AmlsUkHmrc with PendingAmlsDetails

}

