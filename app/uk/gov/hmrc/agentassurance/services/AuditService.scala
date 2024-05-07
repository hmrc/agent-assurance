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

import java.time.temporal.ChronoUnit
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.libs.json.Json
import play.api.libs.json.Writes
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.models.audit.AgentCheckAuditEvent
import uk.gov.hmrc.agentassurance.models.audit.AgentCheckFailureNotificationAuditEvent
import uk.gov.hmrc.agentassurance.models.audit.AuditDetail
import uk.gov.hmrc.agentassurance.models.audit.EmailData
import uk.gov.hmrc.agentassurance.models.AgentCheckOutcome
import uk.gov.hmrc.agentassurance.models.EntityCheckNotification
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier

@Singleton
class AuditService @Inject() (auditConnector: AuditConnector)(implicit ec: ExecutionContext, appConfig: AppConfig) {

  def auditEntityChecksPerformed(arn: Arn, utr: Option[Utr], agentCheckOutcomes: Seq[AgentCheckOutcome])(
      implicit hc: HeaderCarrier
  ): Unit = audit(AgentCheckAuditEvent(arn, utr, agentCheckOutcomes))

  def auditEntityCheckFailureNotificationSent(
      entityCheckNotification: EntityCheckNotification
  )(implicit hc: HeaderCarrier): Unit =
    audit(
      AgentCheckFailureNotificationAuditEvent(
        agentReferenceNumber = entityCheckNotification.arn,
        utr = entityCheckNotification.utr,
        email = appConfig.agentMaintainerEmail,
        emailData = EmailData(
          Seq(entityCheckNotification.failedChecks),
          dateChecked = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        )
      )
    )

  private def audit[A <: AuditDetail: Writes](a: A)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    auditConnector
      .sendExtendedEvent(
        ExtendedDataEvent(
          auditSource = auditSource,
          auditType = a.auditType,
          eventId = UUID.randomUUID().toString,
          detail = Json.toJson(a),
          tags = hc.toAuditTags()
        )
      )
  }

  private val auditSource = "agent-assurance"

}
