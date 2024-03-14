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
                                   desConnector: DesConnector)(implicit ec: ExecutionContext) extends Logging {

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
              createdDate = None // TODO - should this be Instant.now() as it is for the UK details?
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
