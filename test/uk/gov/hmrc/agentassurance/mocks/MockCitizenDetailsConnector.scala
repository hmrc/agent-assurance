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

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.agentassurance.connectors.CitizenDetailsConnector
import uk.gov.hmrc.agentassurance.models.entitycheck.DeceasedCheckException
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier

trait MockCitizenDetailsConnector
extends MockFactory { this: TestSuite =>

  val mockCitizenDetailsConnector = mock[CitizenDetailsConnector]

  def mockGetCitizenDeceasedFlag(saUtr: SaUtr)(response: Option[DeceasedCheckException]) = {
    (mockCitizenDetailsConnector
      .getCitizenDeceasedFlag(_: SaUtr)(_: HeaderCarrier, _: ExecutionContext))
      .expects(saUtr, *, *)
      .returning(Future.successful(response))
  }

}
