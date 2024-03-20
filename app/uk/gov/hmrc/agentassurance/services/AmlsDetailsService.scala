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
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.collection.immutable.{::, Nil}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmlsDetailsService @Inject()(overseasAmlsRepository: OverseasAmlsRepository,
                                   amlsRepository: AmlsRepository,
                                   archivedAmlsRepository: ArchivedAmlsRepository,
                                   desConnector: DesConnector,
                                   agencyDetailsService: AgencyDetailsService
                                  )(implicit ec: ExecutionContext) extends Logging {

  def getAmlsDetailsByArn(arn: Arn)(implicit hc: HeaderCarrier): Future[(AmlsStatus, Option[AmlsDetails])] =
    getAmlsDetails(arn).map {
      case Some(amlsDetails: UkAmlsDetails) if amlsDetails.isExpired =>
        Future.successful((AmlsStatus.ExpiredAmlsDetailsUK, Some(amlsDetails)))
      case Some(amlsDetails: UkAmlsDetails) if amlsDetails.supervisoryBodyIsHmrc && amlsDetails.isPending =>
        Future.successful((AmlsStatus.NoAmlsDetailsUK, Some(amlsDetails)))
      case Some(amlsDetails: UkAmlsDetails) if amlsDetails.supervisoryBodyIsHmrc =>
        getAmlsStatusForHmrcBody(amlsDetails).map { amlsStatus =>
          (
            amlsStatus.getOrElse(if (amlsDetails.isExpired) AmlsStatus.ExpiredAmlsDetailsUK else AmlsStatus.ValidAmlsDetailsUK),
            Some(amlsDetails)
          )
        }
      case Some(amlsDetails: UkAmlsDetails) =>
        Future.successful((AmlsStatus.ValidAmlsDetailsUK, Some(amlsDetails)))
      case Some(amlsDetails: OverseasAmlsDetails) =>
        Future.successful((AmlsStatus.ValidAmlsNonUK, Some(amlsDetails)))
      case None =>
        agencyDetailsService.agencyDetailsHasUkAddress().map { hasUkAddress =>
          (
            if (hasUkAddress) AmlsStatus.NoAmlsDetailsUK else AmlsStatus.NoAmlsDetailsNonUK,
            None
          )
        }
    }.flatten

  private def getAmlsDetails(arn: Arn): Future[Option[AmlsDetails]] =
    Future.sequence(
      Seq(
        amlsRepository.getAmlsDetailsByArn(arn),
        overseasAmlsRepository.getOverseasAmlsDetailsByArn(arn)
      )
    ).map(_.flatten).collect {
      case (amlsDetails: UkAmlsDetails) :: Nil => Some(amlsDetails)
      case (overseasAmlsDetails: OverseasAmlsDetails) :: Nil => Some(overseasAmlsDetails)
      case Nil => None
      case _ =>
        throw new InternalServerException("[AmlsDetailsService][getAmlsDetailsByArn] ARN has both Overseas and UK AMLS details")
    }

  private def getAmlsStatusForHmrcBody(amlsDetails: UkAmlsDetails)(implicit hc: HeaderCarrier): Future[Option[AmlsStatus]] = {

    val optMembershipNumber = amlsDetails.membershipNumber
    val isHmrc = amlsDetails.supervisoryBodyIsHmrc

    (optMembershipNumber, isHmrc) match {
      case (Some(membershipNumber), true) =>
        desConnector.getAmlsSubscriptionStatus(membershipNumber).map {
          amlsRecord =>
            amlsRecord.formBundleStatus match {
              case "Pending" =>
                Some(AmlsStatus.PendingAmlsDetails)
              case "Rejected" =>
                Some(AmlsStatus.PendingAmlsDetailsRejected)
              case "Approved" | "ApprovedWithConditions" =>
                val isExpired = {
                  for {
                    amlsRecordEndDate <- amlsRecord.currentRegYearEndDate
                    amlsDetailsExpiryDate <- amlsDetails.membershipExpiresOn
                  } yield amlsRecordEndDate.isAfter(amlsDetailsExpiryDate)
                }.getOrElse(amlsDetails.membershipExpiresOn.isEmpty)

                if (isExpired) Some(AmlsStatus.ExpiredAmlsDetailsUK) else Some(AmlsStatus.ValidAmlsDetailsUK)
              case _ => None
            }

        }.recover(_ => Some(AmlsStatus.NoAmlsDetailsUK))
      case (_, false) => Future.successful(Some(AmlsStatus.NoAmlsDetailsUK))
      case (None, _) => Future.successful(Some(AmlsStatus.NoAmlsDetailsUK))
    }
  }

  def storeAmlsRequest(arn: Arn,
                       amlsRequest: AmlsRequest,
                       amlsSource: AmlsSource = AmlsSource.Subscription): Future[Either[AmlsError, AmlsDetails]] = {

    val newAmlsDetails: AmlsDetails = amlsRequest.toAmlsEntity(amlsRequest)

    {
      newAmlsDetails match {
        case ukAmlsDetails: UkAmlsDetails =>
          amlsRepository.createOrUpdate( // this method returns old document BEFORE updating it
            arn,
            UkAmlsEntity(
              utr = amlsRequest.utr,
              amlsDetails = ukAmlsDetails,
              arn = Some(arn),
              createdOn = LocalDate.now,
              amlsSource = amlsSource
            )
          )
        case overseasAmlsDetails: OverseasAmlsDetails =>
          overseasAmlsRepository.createOrUpdate( // this method returns old document BEFORE updating it
            OverseasAmlsEntity(
              arn = arn,
              amlsDetails = overseasAmlsDetails,
              createdDate = None
            )
          )
      }
    }.flatMap {
      case Some(oldAmlsEntity) =>
        logger.info(s"[AmlsDetailsService][storeNewAmlsRequest] Old AMLS record archived, stored and returned new record")
        archivedAmlsRepository.create(ArchivedAmlsEntity(arn, oldAmlsEntity)).map {
          case Right(_) => Right(newAmlsDetails)
          case Left(error) => Left(error)
        }
      case None =>
        logger.info(s"[AmlsDetailsService][storeNewAmlsRequest] No old AMLS record found, returning new record")
        Future.successful(Right(newAmlsDetails))
    }
  }

}
