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
import uk.gov.hmrc.agentassurance.helpers.TestConstants.{testAmlsDetails, testArn, testOverseasAmlsDetails}
import uk.gov.hmrc.agentassurance.mocks.{MockAmlsRepository, MockOverseasAmlsRepository}

import scala.concurrent.ExecutionContext

class AmlsDetailsServiceSpec extends PlaySpec with MockAmlsRepository with MockOverseasAmlsRepository {

  val service = new AmlsDetailsService(mockOverseasAmlsRepository, mockAmlsRepository)(ExecutionContext.global)

  "getAmlsDetailsByArn" should {
    "return Future(Seq(UkAmlsDetails(_,_,_,_,_,_)))" when {
      "only UK AMLS Details are available" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(None)

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe Seq(testAmlsDetails)

      }
    }

    "return Future(Seq(OverseasAmlsDetails(_,_)))" when {
      "only Overseas AMLS Details are available" in {
        mockGetAmlsDetailsByArn(testArn)(None)
        mockGetOverseasAmlsDetailsByArn(testArn)(Some(testOverseasAmlsDetails))

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe Seq(testOverseasAmlsDetails)

      }
    }

    "return Future(Seq(UkAmlsDetails(_,_,_,_,_,_)), OverseasAmlsDetails(_,_)))" when {
      "when both are available (technically should never happen)" in {
        mockGetAmlsDetailsByArn(testArn)(Some(testAmlsDetails))
        mockGetOverseasAmlsDetailsByArn(testArn)(Some(testOverseasAmlsDetails))

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe Seq(testAmlsDetails, testOverseasAmlsDetails)
      }
    }

    "return Future(Seq())" when {
      "when none are available" in {
        mockGetAmlsDetailsByArn(testArn)(None)
        mockGetOverseasAmlsDetailsByArn(testArn)(None)

        val result = await(service.getAmlsDetailsByArn(testArn))

        result mustBe Seq.empty
      }
    }
  }

}
