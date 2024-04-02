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

import org.mongodb.scala.result.UpdateResult
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.agentassurance.models.{AmlsError, CreateAmlsRequest, UkAmlsDetails, UkAmlsEntity}
import uk.gov.hmrc.agentassurance.repositories.AmlsRepository
import uk.gov.hmrc.agentassurance.util.toFuture
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}

import java.time.LocalDate
import scala.concurrent.Future

trait MockAmlsRepository extends MockFactory {

  val mockAmlsRepository = mock[AmlsRepository]

  def mockCreateAmls(createAmlsRequest: CreateAmlsRequest)(response: Either[AmlsError, Unit]) = {
    (mockAmlsRepository.createOrUpdate(_: CreateAmlsRequest))
      .expects(createAmlsRequest)
      .returning(toFuture(response))
  }

  def mockUpdateAmls(utr: Utr, arn: Arn)(response: Either[AmlsError, UkAmlsDetails]) = {
    (mockAmlsRepository.updateArn(_: Utr, _: Arn))
      .expects(utr, arn)
      .returning(toFuture(response))
  }

  def mockGetAmls(utr: Utr)(response: Option[UkAmlsDetails]) = {
    (mockAmlsRepository.getAmlDetails(_: Utr))
      .expects(utr)
      .returning(toFuture(response))
  }

 def mockGetAmlsDetailsByArn(arn: Arn)(response: Option[UkAmlsDetails]) = {
    (mockAmlsRepository.getAmlsDetailsByArn(_: Arn))
      .expects(arn)
      .returning(toFuture(response))
  }

  def mockGetAmlsDetailsByArnFuture(arn: Arn)(response: Future[Option[UkAmlsDetails]]) = {
    (mockAmlsRepository.getAmlsDetailsByArn(_: Arn))
      .expects(arn)
      .returning(response)
  }

  def mockCreateOrUpdate(arn: Arn, ukAmnlsEntity: UkAmlsEntity)(response: Option[UkAmlsEntity]) = {
    (mockAmlsRepository.createOrUpdate(_: Arn, _: UkAmlsEntity))
      .expects(arn, ukAmnlsEntity)
      .returning(toFuture(response))
  }

  def mockGetUtr(arn: Arn)(response: Option[Utr]) = {
    (mockAmlsRepository.getUtr(_: Arn))
      .expects(arn)
      .returning(toFuture(response))
  }

  def mockUpdateExpiryDate(arn: Arn, date: LocalDate)(response: UpdateResult) = {
    (mockAmlsRepository.updateExpiryDate(_: Arn, _: LocalDate))
      .expects(arn, date)
      .returning(toFuture(response))
  }

}
