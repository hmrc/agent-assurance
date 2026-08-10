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

package uk.gov.hmrc.agentassurance.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.client.WireMock.*
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentassurance.models.Arn

trait ASAStubs {

  def givenASAGetAgentRecord(
    arn: Arn,
    response: AgentDetailsDesResponse
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/agent-services-account/agent-record-with-checks/arn/${arn.value}"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(Json.toJson(response).toString)
        )
    )

  def givenASAAgentRecordUpdateSuccess(): StubMapping = stubFor(
    put(urlEqualTo("/agent-services-account/agent-record-update"))
      .withHeader("Content-Type", containing("application/json"))
      .willReturn(
        aResponse()
          .withStatus(OK)
      )
  )

}
