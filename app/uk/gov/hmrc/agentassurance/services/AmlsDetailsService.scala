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
import play.api.mvc.Request
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentassurance.connectors.DesConnector
import uk.gov.hmrc.agentassurance.models.*
import uk.gov.hmrc.agentassurance.repositories.AmlsRepository
import uk.gov.hmrc.agentassurance.repositories.ArchivedAmlsRepository
import uk.gov.hmrc.agentassurance.repositories.OverseasAmlsRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.UpstreamErrorResponse.Upstream5xxResponse

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AmlsDetailsService @Inject() (
  overseasAmlsRepository: OverseasAmlsRepository,
  amlsRepository: AmlsRepository,
  archivedAmlsRepository: ArchivedAmlsRepository,
  desConnector: DesConnector,
  agencyDetailsService: AgencyDetailsService,
  agentServicesAccountConnector: AgentServicesAccountConnector,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends Logging:

  def getAmlsDetailsByArn(
    arn: Arn
  )(using
    hc: HeaderCarrier,
    request: Request[?]
  ): Future[(AmlsStatus, Option[AmlsDetails])] =
    if appConfig.useAgentServicesAccountAmls then
      getAmlsDetailsFromAgentServicesAccount(arn)
    else
      getLegacyAmlsDetailsByArn(arn)

  private def getLegacyAmlsDetailsByArn(
    arn: Arn
  )(using
    hc: HeaderCarrier,
    request: Request[?]
  ): Future[(AmlsStatus, Option[AmlsDetails])] =
    getLegacyAmlsDetails(arn).map {
      case None => // No AMLS record found
        handleNoAmlsDetails(arn) // Scenarios: #1, #2
      case Some(overseasAmlsDetails: OverseasAmlsDetails) => Future.successful((AmlsStatus.ValidAmlsNonUK, Some(overseasAmlsDetails))) // Scenario #7
      case Some(ukAmlsDetails: UkAmlsDetails) =>
        if ukAmlsDetails.supervisoryBodyIsHmrc then
          ukAmlsDetails.membershipNumber
            .map { membershipNumber =>
              if ukAmlsDetails.hasValidMembershipNumber then
                // this call may have update ASA AMLS expiry date side effect
                processUkHmrcAmlsDetails(
                  arn,
                  membershipNumber,
                  ukAmlsDetails
                ).map { amlsStatus =>
                  (amlsStatus, Some(ukAmlsDetails)) // Scenarios: #5a, #5b, #6a, #6b #8, #9
                }
              else
                Future.successful((AmlsStatus.ValidAmlsDetailsUK, Some(ukAmlsDetails)))
            }
            .getOrElse(Future.successful((AmlsStatus.NoAmlsDetailsUK, None))) // Scenario #10
        else // supervisoryBodyIsNotHmrc
          Future.successful(
            (
              if hasRenewalDateExpired(ukAmlsDetails.membershipExpiresOn) then
                AmlsStatus.ExpiredAmlsDetailsUK // Scenarios: #4a, #4b
              else
                AmlsStatus.ValidAmlsDetailsUK
              , // Scenario #3
              Some(ukAmlsDetails)
            )
          )
    }.flatten

  private def getAmlsDetailsFromAgentServicesAccount(
    arn: Arn
  )(using
    hc: HeaderCarrier,
    request: Request[?]
  ): Future[(AmlsStatus, Option[AmlsDetails])] = agentServicesAccountConnector.getAgentRecord(arn).flatMap { agentRecord =>
    agentRecord.amlsDetails match
      case Some(amlsDetails) =>
        agentRecord.agencyDetails.map(_.hasUkAddress) match
          case Some(true) => deriveStatusFromDetails(arn, toUkAmlsDetails(amlsDetails))
          case Some(false) => deriveStatusFromDetails(arn, toOverseasAmlsDetails(amlsDetails))
          case None =>
            getLegacyAmlsDetails(arn).flatMap:
              case Some(details) => deriveStatusFromDetails(arn, details)
              case None => handleNoAmlsDetails(arn)
      case None =>
        getLegacyAmlsDetails(arn).flatMap:
          case Some(details) => deriveStatusFromDetails(arn, details)
          case None => handleNoAmlsDetails(arn, agentRecord.agencyDetails.map(_.hasUkAddress))
  }

  // if today's date >= renewal date then it has expired
  // if no renewal date or today's date < renewal date then it has not expired
  // TODO make private when we upgrade play and can test private methods
  def hasRenewalDateExpired(optRenewalDate: Option[LocalDate]): Boolean = optRenewalDate.exists(renewalDate =>
    LocalDate.now().isAfter(renewalDate) || LocalDate.now().equals(renewalDate)
  )

  // User has no AMLS record with us, if their agency is based in the UK then we deem them as UK
  private def handleNoAmlsDetails(
    arn: Arn,
    isUkOverride: Option[Boolean] = None
  )(using
    hc: HeaderCarrier,
    request: Request[?]
  ): Future[(AmlsStatus, Option[AmlsDetails])] =
    isUkOverride
      .map(Future.successful)
      .getOrElse(agencyDetailsService.agencyDetailsHasUkAddress(arn))
      .map { isUk =>
        (
          if isUk then
            AmlsStatus.NoAmlsDetailsUK // Scenario #1
          else
            AmlsStatus.NoAmlsDetailsNonUK
          , // Scenario #2
          None
        )
      }

  // TODO - Add test when upgrading play to test private methods
  private def processUkHmrcAmlsDetails(
    arn: Arn,
    membershipNumber: String,
    asaAmlsDetails: UkAmlsDetails
  )(
    using hc: HeaderCarrier
  ): Future[AmlsStatus] =
    desConnector
      .getAmlsSubscriptionStatus(membershipNumber)
      .map { desAmlsRecord =>
        desAmlsRecord.formBundleStatus match
          case "Pending" => AmlsStatus.PendingAmlsDetails
          case "Rejected" => AmlsStatus.PendingAmlsDetailsRejected
          case "Approved" | "ApprovedWithConditions" =>
            // check if ETMP has more recent expiry date
            val amlsExpiryDate = findCorrectExpiryDate(
              arn,
              desAmlsRecord.currentRegYearEndDate,
              asaAmlsDetails.membershipExpiresOn
            )
            if hasRenewalDateExpired(amlsExpiryDate) then
              AmlsStatus.ExpiredAmlsDetailsUK
            else
              AmlsStatus.ValidAmlsDetailsUK
            end if
          case _ =>
            // catch all where we won't use the ETMP record and just use ASA
            if hasRenewalDateExpired(asaAmlsDetails.membershipExpiresOn) then
              AmlsStatus.ExpiredAmlsDetailsUK
            else
              AmlsStatus.ValidAmlsDetailsUK
      }
      .recover:
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

  // we have two potential optional dates to use so this logic will select the correct date
  // TODO make private when we upgrade play and can test private methods
  def findCorrectExpiryDate(
    arn: Arn,
    maybeDesAmlsExpiry: Option[LocalDate],
    maybeAsaAmlsExpiry: Option[LocalDate]
  ): Option[LocalDate] =
    (maybeDesAmlsExpiry, maybeAsaAmlsExpiry) match
      case (Some(desAmlsExpiry), Some(asaAmlsExpiry)) =>
        if desAmlsExpiry.isAfter(asaAmlsExpiry) then
          amlsRepository.updateExpiryDate(arn, desAmlsExpiry)
          Some(desAmlsExpiry)
        else
          Some(asaAmlsExpiry)
      case (Some(desAmlsExpiry), None) =>
        amlsRepository.updateExpiryDate(arn, desAmlsExpiry)
        Some(desAmlsExpiry)
      case (None, Some(asaAmlsExpiry)) => Some(asaAmlsExpiry)
      case (None, None) => None

  private def getLegacyAmlsDetails(arn: Arn): Future[Option[AmlsDetails]] =
    Future
      .sequence(
        Seq(
          amlsRepository.getAmlsDetailsByArn(arn),
          overseasAmlsRepository.getOverseasAmlsDetailsByArn(arn)
        )
      )
      .map(_.flatten)
      .collect:
        case (amlsDetails: UkAmlsDetails) :: Nil => Some(amlsDetails)
        case (overseasAmlsDetails: OverseasAmlsDetails) :: Nil => Some(overseasAmlsDetails)
        case Nil => None
        case _ =>
          throw new InternalServerException(
            "[AmlsDetailsService][getAmlsDetailsByArn] ARN has both Overseas and UK AMLS details"
          )

  private def deriveStatusFromDetails(
    arn: Arn,
    details: AmlsDetails
  )(using hc: HeaderCarrier): Future[(AmlsStatus, Option[AmlsDetails])] =
    details match
      case overseasAmlsDetails: OverseasAmlsDetails => Future.successful((AmlsStatus.ValidAmlsNonUK, Some(overseasAmlsDetails)))
      case ukAmlsDetails: UkAmlsDetails =>
        if ukAmlsDetails.supervisoryBodyIsHmrc then
          ukAmlsDetails.membershipNumber
            .map { membershipNumber =>
              if ukAmlsDetails.hasValidMembershipNumber then
                processUkHmrcAmlsDetails(
                  arn,
                  membershipNumber,
                  ukAmlsDetails
                ).map(amlsStatus => (amlsStatus, Some(ukAmlsDetails)))
              else
                Future.successful((AmlsStatus.ValidAmlsDetailsUK, Some(ukAmlsDetails)))
            }
            .getOrElse(Future.successful((AmlsStatus.NoAmlsDetailsUK, None)))
        else
          Future.successful((AmlsStatus.ValidAmlsDetailsUK, Some(ukAmlsDetails)))

  def storeAmlsRequest(
    arn: Arn,
    amlsRequest: AmlsRequest
  )(
    using
    hc: HeaderCarrier,
    request: Request[?]
  ): Future[Either[AmlsError, AmlsDetails]] =
    if appConfig.useAgentServicesAccountAmls then
      val updateRequest = AgentRecordUpdateRequest(
        amlsDetails = Some(AgentRecordAmlsDetails(
          supervisoryBody = amlsRequest.supervisoryBody,
          membershipNumber = amlsRequest.membershipNumber,
          evidenceObjectReference = amlsRequest.evidenceObjectReference
        ))
      )

      agentServicesAccountConnector
        .updateAmlsDetails(updateRequest)
        .flatMap { _ =>
          deleteLegacyAmlsDetails(arn)
            .recover { case error =>
              logger.warn(s"[AmlsDetailsService][storeAmlsRequest] ASA update succeeded but legacy AMLS cleanup failed: ${error.getMessage}", error)
              ()
            }
            .map(_ => Right(amlsRequest.toAmlsEntity(amlsRequest)))
        }
        .recover { case _ => Left(AmlsError.AmlsUnexpectedMongoError) }
    else

      val newAmlsDetails: AmlsDetails = amlsRequest.toAmlsEntity(amlsRequest)

      val storedAmlsDetails: Future[Either[AmlsError, Option[AmlsEntity]]] =
        newAmlsDetails match
          case ukAmlsDetails: UkAmlsDetails =>
            getOrRetrieveUtr(arn)
              .flatMap(mUtr =>
                amlsRepository.createOrUpdate(
                  arn,
                  UkAmlsEntity(
                    utr = mUtr,
                    amlsDetails = ukAmlsDetails,
                    arn = Some(arn),
                    createdOn = LocalDate.now
                  )
                )
              )
              .map:
                case Right(maybeUkAmlsEntity) => Right(maybeUkAmlsEntity)
                case Left(error) => Left(error)
          case overseasAmlsDetails: OverseasAmlsDetails =>
            overseasAmlsRepository.createOrUpdate(
              OverseasAmlsEntity(
                arn = arn,
                amlsDetails = overseasAmlsDetails,
                createdDate = None
              )
            ).map(maybeOverseasAmlsEntity => Right(maybeOverseasAmlsEntity))

      storedAmlsDetails.flatMap:
        case Right(Some(oldAmlsEntity)) =>
          logger.info(
            s"[AmlsDetailsService][storeNewAmlsRequest] Old AMLS record archived, stored and returned new record"
          )
          archivedAmlsRepository.create(ArchivedAmlsEntity(arn, oldAmlsEntity)).map:
            case Right(_) => Right(newAmlsDetails)
            case Left(error) => Left(error)
        case Right(None) =>
          logger.info(s"[AmlsDetailsService][storeNewAmlsRequest] No old AMLS record found, returning new record")
          Future.successful(Right(newAmlsDetails))
        case Left(error) =>
          logger.warn(s"[AmlsDetailsService][storeNewAmlsRequest] Failed to store AMLS record: $error")
          Future.successful(Left(error))

  private def getOrRetrieveUtr(arn: Arn)(using
    hc: HeaderCarrier,
    request: Request[?]
  ): Future[Option[Utr]] =
    for
      a <- amlsRepository.getUtr(arn)
      b <-
        if a.isEmpty then
          desConnector.getAgentRecord(arn).map(_.uniqueTaxReference)
        else
          Future.successful(a)
    yield b

  private def toUkAmlsDetails(amlsDetails: AgentRecordAmlsDetails): UkAmlsDetails = UkAmlsDetails(
    supervisoryBody = amlsDetails.supervisoryBody,
    membershipNumber = Some(amlsDetails.membershipNumber),
    appliedOn = None,
    membershipExpiresOn = None
  )

  private def toOverseasAmlsDetails(amlsDetails: AgentRecordAmlsDetails): OverseasAmlsDetails = OverseasAmlsDetails(
    supervisoryBody = amlsDetails.supervisoryBody,
    membershipNumber = Some(amlsDetails.membershipNumber)
  )

  private def deleteLegacyAmlsDetails(arn: Arn): Future[Unit] = Future.sequence(
    Seq(
      amlsRepository.deleteByArn(arn),
      overseasAmlsRepository.deleteByArn(arn)
    )
  ).map(_ => ())

end AmlsDetailsService
