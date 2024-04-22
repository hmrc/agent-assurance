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

package uk.gov.hmrc.agentassurance.services

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testAgentDetailsDesAddressUtrResponse
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testAgentDetailsDesOverseas
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testAgentDetailsDesResponse
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testArn
import uk.gov.hmrc.agentassurance.mocks.MockDesConnector
import uk.gov.hmrc.http.HeaderCarrier

class AgencyDetailsServiceSpec extends PlaySpec with MockDesConnector {

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  val service: AgencyDetailsService = new AgencyDetailsService(mockDesConnector)

  "isUkAddress" should {
    "return true if agency address country code is GB" in {
      mockGetAgentRecord(testArn)(testAgentDetailsDesAddressUtrResponse)

      val result = await(service.agencyDetailsHasUkAddress(testArn))
      result mustBe true
    }

    "return false if agency address country code is not GB" in {
      mockGetAgentRecord(testArn)(testAgentDetailsDesOverseas)

      val result = await(service.agencyDetailsHasUkAddress(testArn))
      result mustBe false
    }

    "return false if no agency address" in {
      mockGetAgentRecord(testArn)(testAgentDetailsDesResponse)

      val result = await(service.agencyDetailsHasUkAddress(testArn))
      result mustBe false
    }
  }
}
