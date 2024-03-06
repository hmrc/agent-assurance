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
import uk.gov.hmrc.agentassurance.models.{AmlsError, OverseasAmlsDetails, OverseasAmlsEntity}
import uk.gov.hmrc.agentassurance.repositories.OverseasAmlsRepository
import uk.gov.hmrc.agentassurance.util.toFuture
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

trait MockOverseasAmlsRepository extends MockFactory {

  val mockOverseasAmlsRepository = mock[OverseasAmlsRepository]

  def mockCreateOverseasAmls(amlsEntity: OverseasAmlsEntity)(response: Either[AmlsError, Unit]) = {
    (mockOverseasAmlsRepository.create(_: OverseasAmlsEntity))
      .expects(amlsEntity)
      .returning(toFuture(response))
  }

  def mockGetOverseasAmlsDetailsByArn(arn: Arn)(response: Option[OverseasAmlsDetails]) = {
    (mockOverseasAmlsRepository.getOverseasAmlsDetailsByArn(_: Arn))
      .expects(arn)
      .returning(toFuture(response))
  }

  def mockCreateOrUpdate(amlsEntity: OverseasAmlsEntity)(response: Option[OverseasAmlsEntity]) = {
    (mockOverseasAmlsRepository.createOrUpdate(_: OverseasAmlsEntity))
      .expects(amlsEntity)
      .returning(toFuture(response))
  }


}
