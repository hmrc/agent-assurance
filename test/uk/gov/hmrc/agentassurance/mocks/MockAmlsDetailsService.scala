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

import scala.concurrent.Future

import org.scalamock.handlers.CallHandler2
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.agentassurance.models.AmlsDetails
import uk.gov.hmrc.agentassurance.models.AmlsStatus
import uk.gov.hmrc.agentassurance.services.AmlsDetailsService
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier

trait MockAmlsDetailsService extends MockFactory { this: TestSuite =>

  val mockAmlsDetailsService: AmlsDetailsService = mock[AmlsDetailsService]

  def mockGetAmlsDetailsByArn(arn: Arn)(
      response: Future[(AmlsStatus, Option[AmlsDetails])]
  ): CallHandler2[Arn, HeaderCarrier, Future[(AmlsStatus, Option[AmlsDetails])]] = {
    (mockAmlsDetailsService
      .getAmlsDetailsByArn(_: Arn)(_: HeaderCarrier))
      .expects(arn, *)
      .returning(response)
  }

}
