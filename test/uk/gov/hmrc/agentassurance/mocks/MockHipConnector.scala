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

package uk.gov.hmrc.agentassurance.mocks

import scala.concurrent.Future
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq as equal
import org.mockito.Mockito.when
import org.scalatest.TestSuite
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.agentassurance.connectors.HipConnector
import uk.gov.hmrc.http.HeaderCarrier

trait MockHipConnector
extends MockitoSugar { this: TestSuite =>

  val mockHipConnector: HipConnector = mock[HipConnector]

  def mockHipGetBusinessNameRecord(
    utr: String
  )(response: Option[String]): Unit =
    when(
      mockHipConnector
        .getBusinessName(equal(utr))(using any[HeaderCarrier])
    ).thenReturn(Future.successful(response))

}
