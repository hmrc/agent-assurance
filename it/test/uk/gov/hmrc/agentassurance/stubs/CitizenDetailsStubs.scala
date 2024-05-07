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

package uk.gov.hmrc.agentassurance.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.Eventually.eventually
import uk.gov.hmrc.domain.SaUtr

trait CitizenDetailsStubs {
  def givenCitizenReturnDeceasedFlag(saUtr: SaUtr, deceased: Boolean): StubMapping = {
    stubFor(
      get(urlEqualTo(s"/citizen-details/sautr/${saUtr.value}"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              s"""{"deceased": $deceased }"""
            )
        )
    )
  }

  def verifyCitizenDetailsWasCalled(saUtr: SaUtr, times: Int = 1): Unit =
    eventually {
      verify(
        times,
        getRequestedFor(urlMatching(s"/citizen-details/sautr/${saUtr.value}"))
      )
    }

}
