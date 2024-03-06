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
import uk.gov.hmrc.agentassurance.helpers.TestConstants._
import uk.gov.hmrc.agentassurance.mocks.{MockAmlsRepository, MockArchivedAmlsRepository, MockOverseasAmlsRepository}
import uk.gov.hmrc.agentassurance.models.AmlsError.AmlsUnexpectedMongoError
import uk.gov.hmrc.agentassurance.models.ArchivedAmlsEntity

import scala.concurrent.ExecutionContext

class AmlsDetailsServiceSpec extends PlaySpec with MockAmlsRepository with MockOverseasAmlsRepository with MockArchivedAmlsRepository{

  val service = new AmlsDetailsService(mockOverseasAmlsRepository, mockAmlsRepository, mockArchivedAmlsRepository)(ExecutionContext.global)

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

  "handleStoringAmls" should {
    "return Right() when storing a UK AMLS record and there was no existing record" in {
      mockCreateOrUpdate(testArn, testUKAmlsEntity)(None)

      val result = await(service.handleStoringNewAmls(testArn, testUKAmlsRequest))

      result mustBe Right(())
    }

    "return Right() when UK AMLS and there is an existing AMLS record" in {
      mockCreateOrUpdate(testArn, testUKAmlsEntity)(Some(testUKAmlsEntity))
      mockCreate(ArchivedAmlsEntity(testArn, testUKAmlsEntity))(Right(()))

      val result = await(service.handleStoringNewAmls(testArn, testUKAmlsRequest))

      result mustBe Right(())
    }

    "return Right() when Overseas AMLS and there is no existing AMLS record" in {
      mockCreateOrUpdate(testOverseasAmlsEntity)(None)

      val result = await(service.handleStoringNewAmls(testArn, testOverseasAmlsRequest))

      result mustBe Right(())
    }

    "return Right() when Overseas AMLS and there is an existing AMLS record" in {
      mockCreateOrUpdate(testOverseasAmlsEntity)(Some(testOverseasAmlsEntity))
      mockCreate(ArchivedAmlsEntity(testArn, testOverseasAmlsEntity))(Right(()))

      val result = await(service.handleStoringNewAmls(testArn, testOverseasAmlsRequest))

      result mustBe Right(())
    }

    "return Left() when there was a problem with storing new AMLS record" in {
      mockCreateOrUpdate(testOverseasAmlsEntity)(Some(testOverseasAmlsEntity))
      mockCreate(ArchivedAmlsEntity(testArn, testOverseasAmlsEntity))(Left(AmlsUnexpectedMongoError))

      val result = await(service.handleStoringNewAmls(testArn, testOverseasAmlsRequest))

      result mustBe Left(AmlsUnexpectedMongoError)
    }
  }

}
