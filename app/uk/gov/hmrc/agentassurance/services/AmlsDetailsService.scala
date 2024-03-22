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

  def getAmlsDetailsByArn(arn: Arn)(implicit hc: HeaderCarrier): Future[(AmlsStatus, Option[AmlsDetails])] = {
    getAmlsDetails(arn).map {
      case None => // No AMLS record found
        handleNoAmlsDetails // Scenarios #1 and #2
      case Some(overseasAmlsDetails: OverseasAmlsDetails) =>
        Future.successful((AmlsStatus.ValidAmlsNonUK, Some(overseasAmlsDetails))) // Scenario #7
      case Some(ukAmlsDetails: UkAmlsDetails) =>
        if (ukAmlsDetails.supervisoryBodyIsHmrc) {
          ukAmlsDetails.membershipNumber.map { membershipNumber =>
            processUkHmrcAmlsDetails(membershipNumber, ukAmlsDetails.membershipExpiresOn).map { amlsStatus =>
              (amlsStatus, Some(ukAmlsDetails)) // Scenarios #5A, #5B, #6A, #6B #8, #9
            }
          }.getOrElse(Future.successful((AmlsStatus.NoAmlsDetailsUK, Some(ukAmlsDetails)))) // Scenario #10
        } else { // supervisoryBodyIsNotHmrc
          Future.successful((
            if (isDateInThePast(ukAmlsDetails.membershipExpiresOn)) AmlsStatus.ExpiredAmlsDetailsUK // Scenarios #4a and #4b
            else AmlsStatus.ValidAmlsDetailsUK, // Scenario #3
            Some(ukAmlsDetails)
          ))
        }
    }.flatten
  }

  // either today's date is after the membership expiry date or there is no date so it hasn't expired
  private def isDateInThePast(renewalDate: Option[LocalDate]): Boolean =
    renewalDate.exists(LocalDate.now().isAfter(_))

  // User has no AMLS record with us, if their agency is based in the UK then we deem them as UK
  private def handleNoAmlsDetails(implicit hc: HeaderCarrier): Future[(AmlsStatus, Option[AmlsDetails])] = {
    agencyDetailsService.agencyDetailsHasUkAddress().map { isUk =>
      (
        if (isUk) AmlsStatus.NoAmlsDetailsUK // Scenario #1
        else AmlsStatus.NoAmlsDetailsNonUK, // Scenario #2
        None
      )
    }
  }

  private def processUkHmrcAmlsDetails(membershipNumber: String, amlsMembershipExpiresOn: Option[LocalDate])(implicit hc: HeaderCarrier): Future[AmlsStatus] = {
    desConnector.getAmlsSubscriptionStatus(membershipNumber).map {
      amlsSubscriptionRecord =>
        amlsSubscriptionRecord.formBundleStatus match {
          case "Pending" =>
            AmlsStatus.PendingAmlsDetails // Scenario #8
          case "Rejected" =>
            AmlsStatus.PendingAmlsDetailsRejected // Scenario #9
          case _ =>
            val amlsExpiryDate: Option[LocalDate] = findCorrectExpiryDate(amlsSubscriptionRecord.currentRegYearEndDate, amlsMembershipExpiresOn)
            // now we have the correct expiry date we can check if it has expired, which covers multiple scenarios
            if (isDateInThePast(amlsExpiryDate)) AmlsStatus.ExpiredAmlsDetailsUK else AmlsStatus.ValidAmlsDetailsUK
        }
    }
  }

  // we have two potential optional dates to use so this logic will select the correct date
  private def findCorrectExpiryDate(maybeAmlsSubscriptionExpiry: Option[LocalDate], maybeAmlsDetailsExpiry: Option[LocalDate]) = {
    (maybeAmlsSubscriptionExpiry, maybeAmlsDetailsExpiry) match {
      case (Some(amlsSubscriptionExpiry), Some(amlsDetailsExpiry)) =>
        if (amlsSubscriptionExpiry.isAfter(amlsDetailsExpiry)) {
          //TODO update amlsDetails expiry date
          Some(amlsSubscriptionExpiry) // Scenario #5A
        } else {
          Some(amlsDetailsExpiry) //TODO what scenario is this?
        }
      case (Some(amlsSubscriptionExpiry), None) =>
        //TODO update amlsDetails expiry date
        Some(amlsSubscriptionExpiry) // Scenario #5B
      case (None, Some(amlsDetailsExpiry)) => Some(amlsDetailsExpiry) // Scenario #6B
      case (None, None) => None //TODO what scenario is this?
    }
  }

  private def getAmlsDetails(arn: Arn): Future[Option[AmlsDetails]] = {
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
