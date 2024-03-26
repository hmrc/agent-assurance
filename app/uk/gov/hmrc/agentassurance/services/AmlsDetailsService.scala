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
        handleNoAmlsDetails // Scenarios: #1, #2
      case Some(overseasAmlsDetails: OverseasAmlsDetails) =>
        Future.successful((AmlsStatus.ValidAmlsNonUK, Some(overseasAmlsDetails))) // Scenario #7
      case Some(ukAmlsDetails: UkAmlsDetails) =>
        if (ukAmlsDetails.supervisoryBodyIsHmrc) {
          ukAmlsDetails.membershipNumber.map { membershipNumber =>
            processUkHmrcAmlsDetails(membershipNumber, ukAmlsDetails).map { amlsStatus =>
              (amlsStatus, Some(ukAmlsDetails)) // Scenarios: #5a, #5b, #6a, #6b #8, #9
            }
          }.getOrElse(Future.successful((AmlsStatus.NoAmlsDetailsUK, Some(ukAmlsDetails)))) // Scenario #10
        } else { // supervisoryBodyIsNotHmrc
          Future.successful((
            if (dateIsInThePast(ukAmlsDetails.membershipExpiresOn)) AmlsStatus.ExpiredAmlsDetailsUK // Scenarios: #4a, #4b
            else AmlsStatus.ValidAmlsDetailsUK, // Scenario #3
            Some(ukAmlsDetails)
          ))
        }
    }.flatten
  }

  // either today's date is after the membership expiry date or there is no date so it hasn't expired
  //TODO make private when we upgrade play and can test private methods
  def dateIsInThePast(renewalDate: Option[LocalDate]): Boolean =
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

  private def processUkHmrcAmlsDetails(membershipNumber: String, asaAmlsDetails: UkAmlsDetails)(implicit hc: HeaderCarrier): Future[AmlsStatus] = {
    desConnector.getAmlsSubscriptionStatus(membershipNumber).map {
      desAmlsRecord =>
        (asaAmlsDetails.isPending, desAmlsRecord.formBundleStatus) match {
          case (true, "Pending") => // Scenario #8
            AmlsStatus.PendingAmlsDetails
          case (true, "Rejected") => // Scenario #9
            AmlsStatus.PendingAmlsDetailsRejected
          case (_, "Approved" | "ApprovedWithConditions") =>
            // check if ETMP has more recent expiry date
            val amlsExpiryDate = findCorrectExpiryDate(desAmlsRecord.currentRegYearEndDate, asaAmlsDetails.membershipExpiresOn)
            if (dateIsInThePast(amlsExpiryDate)) AmlsStatus.ExpiredAmlsDetailsUK else AmlsStatus.ValidAmlsDetailsUK
          case (_, _) =>
            // catch all where we won't use the ETMP record and just use ASA
            if (dateIsInThePast(asaAmlsDetails.membershipExpiresOn)) AmlsStatus.ExpiredAmlsDetailsUK else AmlsStatus.ValidAmlsDetailsUK
        }
    }
  }

  // we have two potential optional dates to use so this logic will select the correct date
  //TODO make private when we upgrade play and can test private methods
  def findCorrectExpiryDate(maybeDesAmlsExpiry: Option[LocalDate], maybeAsaAmlsExpiry: Option[LocalDate]): Option[LocalDate] = {
    (maybeDesAmlsExpiry, maybeAsaAmlsExpiry) match {
      case (Some(desAmlsExpiry), Some(asaAmlsExpiry)) =>
        if (desAmlsExpiry.isAfter(asaAmlsExpiry)) {
          Some(desAmlsExpiry)
        } else {
          Some(asaAmlsExpiry)
        }
      case (Some(desAmlsExpiry), None) => Some(desAmlsExpiry)
      case (None, Some(asaAmlsExpiry)) => Some(asaAmlsExpiry)
      case (None, None) => None
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
