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

package uk.gov.hmrc.agentassurance.connectors.helpers

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.http.RequestId
import uk.gov.hmrc.http.SessionId

class CommonHeadersSpec extends PlaySpec:

  "CommonHeaders" should {
    "propagate request and session identifiers from the header carrier" in {
      given HeaderCarrier = HeaderCarrier(
        requestId = Some(RequestId("request-id")),
        sessionId = Some(SessionId("session-id"))
      )

      CommonHeaders() mustBe Seq(
        HeaderNames.xRequestId -> "request-id",
        HeaderNames.xSessionId -> "session-id"
      )
    }

    "include a request identifier when there is no inbound request" in {
      given HeaderCarrier = HeaderCarrier()

      val headers = CommonHeaders()

      headers.map(_._1) mustBe Seq(HeaderNames.xRequestId)
      headers.head._2 must not be empty
    }
  }

end CommonHeadersSpec
