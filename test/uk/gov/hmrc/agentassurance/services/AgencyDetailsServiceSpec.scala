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

import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.agentassurance.helpers.TestConstants.{agencyDetailsOverseas, agencyDetailsUk}
import uk.gov.hmrc.agentassurance.mocks.MockAgentClientAuthConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class AgencyDetailsServiceSpec extends PlaySpec with MockAgentClientAuthConnector {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val service: AgencyDetailsService = new AgencyDetailsService(mockAcaConnector)

  "isUkAddress" should {
    "return true if agency address country code is GB" in {
      mockGetAgencyDetails()(Some(agencyDetailsUk))

      val result = await(service.agencyDetailsHasUkAddress())
      result mustBe true
    }

    "return false if agency address country code is not GB" in {
      mockGetAgencyDetails()(Some(agencyDetailsOverseas))

      val result = await(service.agencyDetailsHasUkAddress())
      result mustBe false
    }

    "return false if no agency address" in {
      mockGetAgencyDetails()(None)

      val result = await(service.agencyDetailsHasUkAddress())
      result mustBe false
    }
  }
}
