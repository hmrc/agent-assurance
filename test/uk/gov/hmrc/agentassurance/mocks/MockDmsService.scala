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

package uk.gov.hmrc.agentassurance.mocks

import java.time.Instant

import scala.concurrent.Future

import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.agentassurance.models.DmsResponse
import uk.gov.hmrc.agentassurance.models.DmsSubmissionReference
import uk.gov.hmrc.agentassurance.services.DmsService
import uk.gov.hmrc.http.HeaderCarrier

trait MockDmsService
extends MockFactory { this: TestSuite =>

  val mockDmsService = mock[DmsService]

  def mockSubmitToDmsSuccess =
    (mockDmsService
      .submitToDms(
        _: Option[String],
        _: Instant,
        _: DmsSubmissionReference
      )(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(DmsResponse(Instant.now(), "")))

}
