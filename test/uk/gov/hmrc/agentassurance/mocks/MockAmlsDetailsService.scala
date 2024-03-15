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

import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.agentassurance.models.{AmlsDetails, AmlsStatus, UkAmlsDetails}
import uk.gov.hmrc.agentassurance.services.AmlsDetailsService
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockAmlsDetailsService extends MockFactory {

  val mockAmlsDetailsService = mock[AmlsDetailsService]

  def mockGetAmlsDetailsByArn(arn: Arn)(response: Seq[AmlsDetails]) =
    (mockAmlsDetailsService.getAmlsDetailsByArn(_: Arn))
      .expects(arn)
      .returning(Future.successful(response))

  def mockGetAmlsStatusForHmrcBody(amlsDetails: UkAmlsDetails)(response: AmlsStatus) =
    (mockAmlsDetailsService.getAmlsStatusForHmrcBody(_: UkAmlsDetails)(_: ExecutionContext, _: HeaderCarrier))
      .expects(amlsDetails, *, *)
      .returning(Future.successful(response))

  def mockGetAmlsStatus(arn: Arn)(response: AmlsStatus) =
    (mockAmlsDetailsService.getAmlsStatus(_: Arn)(_: ExecutionContext, _: HeaderCarrier))
      .expects(arn, *, *)
      .returning(Future.successful(response))


}
