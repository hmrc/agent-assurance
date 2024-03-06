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

import play.api.Logging
import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.agentassurance.repositories.{AmlsRepository, ArchivedAmlsRepository, OverseasAmlsRepository}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmlsDetailsService @Inject()(overseasAmlsRepository: OverseasAmlsRepository,
                                   amlsRepository: AmlsRepository,
                                   archivedAmlsRepository: ArchivedAmlsRepository
                                  )(implicit ec: ExecutionContext) extends Logging {

  def getAmlsDetailsByArn(arn: Arn): Future[Seq[AmlsDetails]] =
    Future.sequence(
      Seq(
        amlsRepository.getAmlsDetailsByArn(arn),
        overseasAmlsRepository.getOverseasAmlsDetailsByArn(arn)
      )
    ).map(_.flatten)

  def handleStoringNewAmls(arn: Arn, amlsRequest: AmlsRequest): Future[Either[AmlsError, Unit]] = {
      val amlsDetails = amlsRequest.toAmlsEntity(amlsRequest)
    (amlsDetails match {
          case uk: UkAmlsDetails =>
            val ukAmlsEntity = UkAmlsEntity(utr = amlsRequest.utr, amlsDetails = uk, arn = Some(arn), createdOn = LocalDate.now,
              amlsSource = AmlsSource.Subscription)

            amlsRepository
              .createOrUpdate(arn, ukAmlsEntity )
          case os: OverseasAmlsDetails =>
            overseasAmlsRepository.createOrUpdate(OverseasAmlsEntity(arn = arn, amlsDetails = os, createdDate = None))
        }).flatMap {
      case Some(oldAmlsEntity) => archivedAmlsRepository.create(ArchivedAmlsEntity(arn,oldAmlsEntity))
      case None =>
        logger.info(s"no AMLS record existed for ${arn.value} so nothing to archive")
        Future successful Right(())
      }
    }
}
