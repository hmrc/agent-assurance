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

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

import scala.collection.immutable.::
import scala.collection.immutable.Nil
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.Logging
import uk.gov.hmrc.agentassurance.connectors.DesConnector
import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.agentassurance.repositories.AmlsRepository
import uk.gov.hmrc.agentassurance.repositories.ArchivedAmlsRepository
import uk.gov.hmrc.agentassurance.repositories.OverseasAmlsRepository
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.UpstreamErrorResponse.Upstream5xxResponse

@Singleton
class AmlsDetailsService @Inject() (
    overseasAmlsRepository: OverseasAmlsRepository,
    amlsRepository: AmlsRepository,
    archivedAmlsRepository: ArchivedAmlsRepository,
    desConnector: DesConnector,
    agencyDetailsService: AgencyDetailsService
)(implicit ec: ExecutionContext)
    extends Logging {

  def getAmlsDetailsByArn(arn: Arn)(implicit hc: HeaderCarrier): Future[(AmlsStatus, Option[AmlsDetails])] = {
    getAmlsDetails(arn).map {
      case None => // No AMLS record found
        handleNoAmlsDetails(arn) // Scenarios: #1, #2
      case Some(overseasAmlsDetails: OverseasAmlsDetails) =>
        Future.successful((AmlsStatus.ValidAmlsNonUK, Some(overseasAmlsDetails))) // Scenario #7
      case Some(ukAmlsDetails: UkAmlsDetails) =>
        if (ukAmlsDetails.supervisoryBodyIsHmrc) {
          ukAmlsDetails.membershipNumber
            .map { membershipNumber =>
              // this call may have update ASA AMLS expiry date side-effect
              processUkHmrcAmlsDetails(arn, membershipNumber, ukAmlsDetails).map { amlsStatus =>
                (amlsStatus, Some(ukAmlsDetails)) // Scenarios: #5a, #5b, #6a, #6b #8, #9
              }
            }
            .getOrElse(Future.successful((AmlsStatus.NoAmlsDetailsUK, None))) // Scenario #10
        } else {                                                              // supervisoryBodyIsNotHmrc
          Future.successful(
            (
              if (hasRenewalDateExpired(ukAmlsDetails.membershipExpiresOn))
                AmlsStatus.ExpiredAmlsDetailsUK   // Scenarios: #4a, #4b
              else AmlsStatus.ValidAmlsDetailsUK, // Scenario #3
              Some(ukAmlsDetails)
            )
          )
        }
    }.flatten
  }

  // if today's date >= renewal date then it has expired
  // if no renewal date or today's date < renewal date then it has not expired
  // TODO make private when we upgrade play and can test private methods
  def hasRenewalDateExpired(optRenewalDate: Option[LocalDate]): Boolean =
    optRenewalDate.exists(renewalDate => LocalDate.now().isAfter(renewalDate) || LocalDate.now().equals(renewalDate))

  // User has no AMLS record with us, if their agency is based in the UK then we deem them as UK
  private def handleNoAmlsDetails(arn: Arn)(implicit hc: HeaderCarrier): Future[(AmlsStatus, Option[AmlsDetails])] = {
    agencyDetailsService.agencyDetailsHasUkAddress(arn).map { isUk =>
      (
        if (isUk) AmlsStatus.NoAmlsDetailsUK // Scenario #1
        else AmlsStatus.NoAmlsDetailsNonUK,  // Scenario #2
        None
      )
    }
  }

  // TODO - Add test when upgrading play to test private methods
  private def processUkHmrcAmlsDetails(arn: Arn, membershipNumber: String, asaAmlsDetails: UkAmlsDetails)(
      implicit hc: HeaderCarrier
  ): Future[AmlsStatus] = {
    desConnector
      .getAmlsSubscriptionStatus(membershipNumber)
      .map { desAmlsRecord =>
        (asaAmlsDetails.isPending, desAmlsRecord.formBundleStatus) match {
          case (true, "Pending") => // Scenario #8
            AmlsStatus.PendingAmlsDetails
          case (true, "Rejected") => // Scenario #9
            AmlsStatus.PendingAmlsDetailsRejected
          case (_, "Approved" | "ApprovedWithConditions") =>
            // check if ETMP has more recent expiry date
            val amlsExpiryDate =
              findCorrectExpiryDate(arn, desAmlsRecord.currentRegYearEndDate, asaAmlsDetails.membershipExpiresOn)
            if (hasRenewalDateExpired(amlsExpiryDate)) AmlsStatus.ExpiredAmlsDetailsUK
            else AmlsStatus.ValidAmlsDetailsUK
          case (_, _) =>
            // catch all where we won't use the ETMP record and just use ASA
            if (hasRenewalDateExpired(asaAmlsDetails.membershipExpiresOn)) AmlsStatus.ExpiredAmlsDetailsUK
            else AmlsStatus.ValidAmlsDetailsUK
        }
      }
      .recover {
        case error: UpstreamErrorResponse if UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
          logger.warn(
            s"DES API#1028 returned the following response - status: ${error.statusCode}, message: ${error.message}"
          )
          AmlsStatus.ValidAmlsDetailsUK // temp fix for initial release todo - create a new status to handle errors
        case Upstream5xxResponse(error)
            if error.statusCode == 503 && (error.message.contains("REGIME") | error.message.contains("Technical")) =>
          logger.warn(
            s"DES API#1028 returned the following response - status: ${error.statusCode}, message: ${error.message}"
          )
          AmlsStatus.ValidAmlsDetailsUK // temp fix for initial release todo - create a new status to handle errors
      }
  }

  // we have two potential optional dates to use so this logic will select the correct date
  // TODO make private when we upgrade play and can test private methods
  def findCorrectExpiryDate(
      arn: Arn,
      maybeDesAmlsExpiry: Option[LocalDate],
      maybeAsaAmlsExpiry: Option[LocalDate]
  ): Option[LocalDate] = {
    (maybeDesAmlsExpiry, maybeAsaAmlsExpiry) match {
      case (Some(desAmlsExpiry), Some(asaAmlsExpiry)) =>
        if (desAmlsExpiry.isAfter(asaAmlsExpiry)) {
          amlsRepository.updateExpiryDate(arn, desAmlsExpiry)
          Some(desAmlsExpiry)
        } else {
          Some(asaAmlsExpiry)
        }
      case (Some(desAmlsExpiry), None) =>
        amlsRepository.updateExpiryDate(arn, desAmlsExpiry)
        Some(desAmlsExpiry)
      case (None, Some(asaAmlsExpiry)) =>
        Some(asaAmlsExpiry)
      case (None, None) =>
        None
    }
  }

  private def getAmlsDetails(arn: Arn): Future[Option[AmlsDetails]] = {
    Future
      .sequence(
        Seq(
          amlsRepository.getAmlsDetailsByArn(arn),
          overseasAmlsRepository.getOverseasAmlsDetailsByArn(arn)
        )
      )
      .map(_.flatten)
      .collect {
        case (amlsDetails: UkAmlsDetails) :: Nil               => Some(amlsDetails)
        case (overseasAmlsDetails: OverseasAmlsDetails) :: Nil => Some(overseasAmlsDetails)
        case Nil                                               => None
        case _ =>
          throw new InternalServerException(
            "[AmlsDetailsService][getAmlsDetailsByArn] ARN has both Overseas and UK AMLS details"
          )
      }
  }

  def storeAmlsRequest(arn: Arn, amlsRequest: AmlsRequest, amlsSource: AmlsSource = AmlsSource.Subscription)(
      implicit hc: HeaderCarrier
  ): Future[Either[AmlsError, AmlsDetails]] = {

    val newAmlsDetails: AmlsDetails = amlsRequest.toAmlsEntity(amlsRequest)

    {
      newAmlsDetails match {
        case ukAmlsDetails: UkAmlsDetails =>
          getOrRetrieveUtr(arn)
            .flatMap(mUtr =>
              amlsRepository.createOrUpdate( // this method returns old document BEFORE updating it
                arn,
                UkAmlsEntity(
                  utr = mUtr,
                  amlsDetails = ukAmlsDetails,
                  arn = Some(arn),
                  createdOn = LocalDate.now,
                  amlsSource = amlsSource
                )
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
        logger.info(
          s"[AmlsDetailsService][storeNewAmlsRequest] Old AMLS record archived, stored and returned new record"
        )
        archivedAmlsRepository.create(ArchivedAmlsEntity(arn, oldAmlsEntity)).map {
          case Right(_)    => Right(newAmlsDetails)
          case Left(error) => Left(error)
        }
      case None =>
        logger.info(s"[AmlsDetailsService][storeNewAmlsRequest] No old AMLS record found, returning new record")
        Future.successful(Right(newAmlsDetails))
    }
  }

  private def getOrRetrieveUtr(arn: Arn)(implicit hc: HeaderCarrier): Future[Option[Utr]] =
    for {
      a <- amlsRepository.getUtr(arn)
      b <- if (a.isEmpty) desConnector.getAgentRecord(arn).map(_.uniqueTaxReference) else Future.successful(a)
    } yield b

}
