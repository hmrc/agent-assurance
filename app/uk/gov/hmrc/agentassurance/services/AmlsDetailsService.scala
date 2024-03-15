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
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmlsDetailsService @Inject()(overseasAmlsRepository: OverseasAmlsRepository,
                                   amlsRepository: AmlsRepository,
                                   archivedAmlsRepository: ArchivedAmlsRepository,
                                   desConnector: DesConnector,
                                   agencyDetailsService: AgencyDetailsService)(implicit ec: ExecutionContext) extends Logging {

  def getAmlsStatus(arn: Arn)
                   (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[AmlsStatus] =
    getAmlsDetailsByArnValidated(arn).map {
      case Some(amlsDetails: UkAmlsDetails) =>
        if (amlsDetails.supervisoryBodyIsHmrc)
          if (amlsDetails.isPending) Future.successful(AmlsStatus.NoAmlsDetailsUK)
          else getAmlsStatusForHmrcBody(amlsDetails)
        else
          if (amlsDetails.isExpired) Future.successful(AmlsStatus.ExpiredAmlsDetailsUK)
          else Future.successful(AmlsStatus.ValidAmlsDetailsUK)

      case Some(_: OverseasAmlsDetails) => Future.successful(AmlsStatus.ValidAmlsNonUK)

      case None =>
        agencyDetailsService.isUkAddress().map {
          case true => AmlsStatus.NoAmlsDetailsUK
          case false => AmlsStatus.NoAmlsDetailsNonUK
        }
    }.flatten


  def getAmlsStatusForHmrcBody(amlsDetails: UkAmlsDetails)
                                     (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[AmlsStatus] =

    if (amlsDetails.supervisoryBodyIsHmrc)
      amlsDetails.membershipNumber match {
        case Some(x) => desConnector.getAmlsSubscriptionStatus(x).map(Some(_)).map(_.map { amlsSubscriptionRecord =>
          amlsSubscriptionRecord.formBundleStatus match {
            case "Pending" => AmlsStatus.PendingAmlsDetails
            case "Rejected" => AmlsStatus.PendingAmlsDetailsRejected
            case "Approved" | "ApprovedWithConditions" =>
              val isAmlsSubscriptionRecordNewer = (for {
                amlsRecordEndDate <- amlsSubscriptionRecord.currentRegYearEndDate
                amlsDetailsExpiryDate <- amlsDetails.membershipExpiresOn
              } yield amlsRecordEndDate.isAfter(amlsDetailsExpiryDate))
                .getOrElse(amlsDetails.membershipExpiresOn.isEmpty)

              if (isAmlsSubscriptionRecordNewer) AmlsStatus.ExpiredAmlsDetailsHmrcUK
              else AmlsStatus.ValidAmlsDetailsHmrcUK
            case _ => throw new InternalServerException("[AmlsDetailsService][getAmlsStatusForHmrcBody] Invalid amls subscription status from DES")
          }

        }.getOrElse(AmlsStatus.NoAmlsDetailsHmrcUK))
          .recover(_ => AmlsStatus.NoAmlsDetailsHmrcUK)
        case None => Future.successful(AmlsStatus.NoAmlsDetailsHmrcUK)
      }
    else Future.successful(AmlsStatus.NoAmlsDetailsUK)

  def getAmlsDetailsByArnValidated(arn: Arn): Future[Option[AmlsDetails]] = getAmlsDetailsByArn(arn: Arn).collect {
      case (amlsDetails:UkAmlsDetails)::Nil => Some(amlsDetails)
      case (overseasAmlsDetails:OverseasAmlsDetails)::Nil => Some(overseasAmlsDetails)
      case Nil => None
      case _ =>
        throw new InternalServerException("[AmlsDetailsService][getAmlsDetailsByArnValidated] ARN has both Overseas and UK AMLS details")
  }


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

  def storeAmlsRequest(arn: Arn, amlsRequest: AmlsRequest, amlsSource: AmlsSource = AmlsSource.Subscription): Future[Either[AmlsError, AmlsDetails]] = {

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
