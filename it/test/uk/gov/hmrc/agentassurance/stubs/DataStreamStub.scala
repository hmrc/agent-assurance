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

package test.uk.gov.hmrc.agentassurance.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.Eventually

trait DataStreamStub extends Eventually {

  def givenAuditConnector() = {
    stubFor(post(urlPathEqualTo(auditUrl)).willReturn(aResponse().withStatus(200)))
  }

  private def auditUrl = "/write/audit"
}
