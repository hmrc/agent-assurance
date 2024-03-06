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
import uk.gov.hmrc.agentassurance.connectors.DesConnector
import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.agentassurance.repositories.{AmlsRepository, ArchivedAmlsRepository, OverseasAmlsRepository}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmlsDetailsService @Inject()(overseasAmlsRepository: OverseasAmlsRepository,
                                   amlsRepository: AmlsRepository,
                                   archivedAmlsRepository: ArchivedAmlsRepository,
                                   desConnector: DesConnector
                                  )(implicit ec: ExecutionContext) extends Logging {

  def getAmlsDetailsByArn(arn: Arn): Future[Seq[AmlsDetails]] =
    Future.sequence(
      Seq(
        amlsRepository.getAmlsDetailsByArn(arn),
        overseasAmlsRepository.getOverseasAmlsDetailsByArn(arn)
      )
    ).map(_.flatten)

  def getUpdatedAmlsDetailsForHmrcBody(amlsDetails: UkAmlsDetails)
                                      (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[UkAmlsDetails] = {

    val optMembershipNumber = amlsDetails.membershipNumber
    val isHmrc = amlsDetails.supervisoryBodyIsHmrc

    (optMembershipNumber, isHmrc) match {
      case (Some(membershipNumber), true) =>
        desConnector.getAmlsSubscriptionStatus(membershipNumber).map {
          amlsRecord =>
            amlsRecord.formBundleStatus match {
              case "Approved" | "ApprovedWithConditions" => {
                for {
                  amlsRecordEndDate <- amlsRecord.currentRegYearEndDate
                  amlsDetailsExpiryDate <- amlsDetails.membershipExpiresOn
                  if amlsRecordEndDate.isAfter(amlsDetailsExpiryDate)
                } yield amlsDetails.copy(membershipExpiresOn = Some(amlsRecordEndDate))
              }.getOrElse(amlsDetails)
              case _ =>
                amlsDetails
            }
        }.recover(_ => amlsDetails)
      case _ =>
        Future.successful(amlsDetails)
    }
  }

  def handleStoringNewAmls(arn: Arn, amlsRequest: AmlsRequest): Future[Either[AmlsError, Unit]] = {
      val amlsDetails = amlsRequest.toAmlsEntity(amlsRequest)
    (amlsDetails match {
          case uk: UkAmlsDetails =>
            amlsRepository
              .createOrUpdate(arn, UkAmlsEntity(amlsRequest.utr, uk, arn = Some(arn), LocalDate.now))
          case os: OverseasAmlsDetails =>
            overseasAmlsRepository.createOrUpdate(OverseasAmlsEntity(arn, os))
        }).flatMap {
      case Some(oldAmlsEntity) => archivedAmlsRepository.create(ArchivedAmlsEntity(arn,oldAmlsEntity))
      case None =>
        logger.info(s"no AMLS record existed for ${arn.value} so nothing to archive")
        Future successful Right(())
      }
    }
}
