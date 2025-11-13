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

import play.api.mvc.Request
import uk.gov.hmrc.agentassurance.connectors.CitizenDetailsConnector
import uk.gov.hmrc.agentassurance.connectors.DesConnector
import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.agentassurance.models.entitycheck.DeceasedCheckException.CitizenConnectorRequestFailed
import uk.gov.hmrc.agentassurance.models.entitycheck.DeceasedCheckException.EntityDeceasedCheckFailed
import uk.gov.hmrc.agentassurance.models.entitycheck.EmailCheckExceptions
import uk.gov.hmrc.agentassurance.models.entitycheck.EntityCheckException
import uk.gov.hmrc.agentassurance.models.entitycheck.EntityCheckResult
import uk.gov.hmrc.agentassurance.models.entitycheck.RefusalCheckException
import uk.gov.hmrc.agentassurance.models.entitycheck.RefusalCheckException.AgentIsOnRefuseToDealList
import uk.gov.hmrc.agentassurance.repositories.PropertiesRepository
import uk.gov.hmrc.agentassurance.utils.DateTimeService
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class EntityCheckService @Inject() (
  desConnector: DesConnector,
  citizenConnector: CitizenDetailsConnector,
  mongoLockService: MongoLockService,
  emailService: EmailService,
  repository: PropertiesRepository,
  auditService: AuditService,
  clock: Clock
) {

  def verifyAgent(
    arn: Arn
  )(implicit
    request: Request[_],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[EntityCheckResult] = {

    def deceasedStatusCheck(saUtr: SaUtr): Future[Option[EntityCheckException]] = citizenConnector
      .getCitizenDeceasedFlag(saUtr)

    def refusalToDealCheck(utr: Utr): Future[Option[RefusalCheckException]] = {
      repository.propertyExists(Value(utr.value).toProperty("refusal-to-deal-with")).map {
        case true => Some(AgentIsOnRefuseToDealList)
        case false => None
      }
    }

    def entityChecks(
      arn: Arn,
      utr: Utr,
      isAnIndividual: Option[Boolean]
    ): Future[Seq[EntityCheckException]] = {
      val checksRequired =
        if (isAnIndividual.contains(true)) {
          Seq(deceasedStatusCheck(SaUtr(utr.value)), refusalToDealCheck(utr))
        }
        else
          Seq(refusalToDealCheck(utr))
      mongoLockService
        .dailyLock(utr) {
          Future
            .sequence(checksRequired)
            .map(_.flatten)
        }
        .map {
          case Some(entityCheckExceptions) =>
            val onRefusalListAgentCheckOutcomes: AgentCheckOutcome = entityCheckExceptions
              .collectFirst {
                case AgentIsOnRefuseToDealList =>
                  AgentCheckOutcome(
                    agentCheckType = "onRefusalList",
                    isSuccessful = false,
                    failureReason = Some(AgentIsOnRefuseToDealList.failedChecksText)
                  )
              }
              .getOrElse(AgentCheckOutcome(
                agentCheckType = "onRefusalList",
                isSuccessful = true,
                failureReason = None
              ))

            val isDeceasedAgentCheckOutcome: AgentCheckOutcome = entityCheckExceptions
              .collectFirst {
                case EntityDeceasedCheckFailed =>
                  AgentCheckOutcome(
                    agentCheckType = "isDeceased",
                    isSuccessful = false,
                    failureReason = Some(EntityDeceasedCheckFailed.failedChecksText)
                  )
                case x @ CitizenConnectorRequestFailed(_) =>
                  AgentCheckOutcome(
                    agentCheckType = "isDeceased",
                    isSuccessful = false,
                    failureReason = Some(s"Check failed with error: ${x.code.toString}")
                  )
              }
              .getOrElse(AgentCheckOutcome(
                agentCheckType = "isDeceased",
                isSuccessful = true,
                failureReason = None
              ))

            auditService.auditEntityChecksPerformed(
              arn,
              Some(utr),
              agentCheckOutcomes = Seq(isDeceasedAgentCheckOutcome, onRefusalListAgentCheckOutcomes)
            )
            entityCheckExceptions
          case None => Seq.empty[EntityCheckException]
        }
    }

    def sendEmail(
      agentRecord: AgentDetailsDesResponse,
      entityCheckExceptions: Seq[EntityCheckException]
    ): Future[Unit] = {
      val failedChecks: Seq[String] = entityCheckExceptions.collect {
        case x: EmailCheckExceptions => x.failedChecksText
      }

      (agentRecord.uniqueTaxReference, failedChecks) match {
        case (Some(utr), nonEmptyFailedChecks) if nonEmptyFailedChecks.nonEmpty =>
          val entityCheckNotification = EntityCheckNotification(
            arn = arn,
            utr = utr.value,
            agencyName = agentRecord.agencyDetails.flatMap(_.agencyName).getOrElse(""),
            failedChecks = nonEmptyFailedChecks.mkString("|"),
            dateTime = DateTimeService.nowAtLondonTime(clock)
          )

          mongoLockService
            .emailLock(utr) {
              emailService.sendEntityCheckNotification(entityCheckNotification)
            }
            .map {
              case Some(_) => auditService.auditEntityCheckFailureNotificationSent(entityCheckNotification)
              case None => ()
            }

        case _ => Future.successful(())
      }
    }

    for {
      agentRecord <- desConnector.getAgentRecord(arn)
      entityChecksResult <- agentRecord.uniqueTaxReference
        .map(entityChecks(
          arn,
          _,
          agentRecord.isAnIndividual
        ))
        .getOrElse(Future.successful(Seq.empty[EntityCheckException]))
      _ <- sendEmail(agentRecord, entityChecksResult)
    } yield EntityCheckResult(
      agentRecord,
      entityChecksResult
    )

  }

}
