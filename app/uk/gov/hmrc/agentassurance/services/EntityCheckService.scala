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

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.mvc.Request
import uk.gov.hmrc.agentassurance.connectors.CitizenDetailsConnector
import uk.gov.hmrc.agentassurance.connectors.DesConnector
import uk.gov.hmrc.agentassurance.models.entitycheck.EmailCheckExceptions
import uk.gov.hmrc.agentassurance.models.entitycheck.EntityCheckException
import uk.gov.hmrc.agentassurance.models.entitycheck.EntityCheckResult
import uk.gov.hmrc.agentassurance.models.entitycheck.RefusalCheckException
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentassurance.models.EntityCheckNotification
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class EntityCheckService @Inject() (
    desConnector: DesConnector,
    citizenConnector: CitizenDetailsConnector,
    mongoLockService: MongoLockService,
    emailService: EmailService,
) {

  def verifyAgent(
      arn: Arn
  )(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[EntityCheckResult] = {

    def deceasedStatusCheck(saUtr: SaUtr): Future[Option[EntityCheckException]] =
      citizenConnector
        .getCitizenDeceasedFlag(saUtr)

    // TODO WG - to be implement
    def refusalToDealCheck(saUtr: SaUtr): Future[Option[RefusalCheckException]] = Future.successful(None)

    def entityChecks(utr: Utr): Future[Seq[EntityCheckException]] = {
      mongoLockService
        .dailyLock(utr) {
          val saUtr = SaUtr(utr.value)
          Future
            .sequence(Seq(deceasedStatusCheck(saUtr), refusalToDealCheck(saUtr)))
            .map(_.flatten)
        }
        .map(_.getOrElse(Seq.empty[EntityCheckException]))
    }

    def sendEmail(agentRecord: AgentDetailsDesResponse, entityCheckExceptions: Seq[EntityCheckException]) = {
      val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy h:mma")

      val failedChecks: Seq[String] = entityCheckExceptions.collect {
        case x: EmailCheckExceptions => x.failedChecksText
      }

      (agentRecord.uniqueTaxReference, failedChecks) match {
        case (Some(utr), nonEmptyFailedChecks) if nonEmptyFailedChecks.nonEmpty =>
          val entityCheckNotification = EntityCheckNotification(
            arn = arn,
            utr = utr.value,
            agencyName = agentRecord.agencyDetails.flatMap(_.agencyName).getOrElse(""),
            failedChecks = nonEmptyFailedChecks.mkString("Checks that failed: ", " ", ""),
            dateTime = formatter.format(LocalDateTime.now())
          )

          mongoLockService
            .emailLock(utr) {
              emailService.sendEntityCheckNotification(entityCheckNotification)
            }
            .map(_.getOrElse(()))

        case _ => Future.successful(())
      }
    }

    for {
      agentRecord <- desConnector.getAgentRecord(arn)
      entityChecksResult <- agentRecord.uniqueTaxReference
        .map(entityChecks(_))
        .getOrElse(Future.successful(Seq.empty[EntityCheckException]))
      _ <- sendEmail(agentRecord, entityChecksResult)
    } yield EntityCheckResult(
      agentRecord.suspensionDetails,
      entityChecksResult
    )

  }

}
