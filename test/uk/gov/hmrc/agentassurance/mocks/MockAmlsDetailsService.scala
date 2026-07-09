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

import org.scalamock.handlers.CallHandler3
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import play.api.mvc.Request
import uk.gov.hmrc.agentassurance.models.*
import uk.gov.hmrc.agentassurance.services.AmlsDetailsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockAmlsDetailsService
extends MockFactory { this: TestSuite =>

  val mockAmlsDetailsService: AmlsDetailsService = mock[AmlsDetailsService]

  def mockGetAmlsDetailsByArn(arn: Arn)(
    response: Future[(AmlsStatus, Option[AmlsDetails])]
  ): CallHandler3[
    Arn,
    HeaderCarrier,
    Request[?],
    Future[(AmlsStatus, Option[AmlsDetails])]
  ] =
    (mockAmlsDetailsService
      .getAmlsDetailsByArn(_: Arn)(using _: HeaderCarrier, _: Request[?]))
      .expects(arn, *, *)
      .returning(response)

  def mockStoreAmlsRequest(
    arn: Arn,
    amlsRequest: AmlsRequest
  )(response: Future[Either[AmlsError, AmlsDetails]]): CallHandler4[
    Arn,
    AmlsRequest,
    HeaderCarrier,
    Request[?],
    Future[Either[AmlsError, AmlsDetails]]
  ] =
    (mockAmlsDetailsService
      .storeAmlsRequest(
        _: Arn,
        _: AmlsRequest
      )(using _: HeaderCarrier, _: Request[?]))
      .expects(
        arn,
        amlsRequest,
        *,
        *
      )
      .returning(response)

}
