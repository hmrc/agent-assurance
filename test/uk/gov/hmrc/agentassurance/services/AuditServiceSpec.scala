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

import scala.concurrent.ExecutionContext

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testArn
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testUtr
import uk.gov.hmrc.agentassurance.mocks.MockAppConfig
import uk.gov.hmrc.agentassurance.mocks.MockAuditConnector
import uk.gov.hmrc.agentassurance.models.AgentCheckOutcome
import uk.gov.hmrc.agentassurance.models.EntityCheckNotification
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

class AuditServiceSpec extends PlaySpec with MockAppConfig with MockAuditConnector {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier    = new HeaderCarrier()
  val auditService                  = new AuditService(mockAuditConnector)(ec, mockAppConfig)

  "auditEntityCheckFailureNotificationSent" should {
    "send audit event" in {
      val nowTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)

      mockSendExtendedEvent(AuditResult.Success)

      auditService.auditEntityCheckFailureNotificationSent(
        EntityCheckNotification(testArn, testUtr.value, "ABC", "deceased, refusalList", nowTime.toString)
      )
    }
  }

  "auditEntityChecksPerformed" should {
    "send audit event" in {

      mockSendExtendedEvent(AuditResult.Success)

      auditService.auditEntityChecksPerformed(
        testArn,
        Some(testUtr),
        Seq(
          AgentCheckOutcome("deceased", true, Some("is deceased")),
          AgentCheckOutcome("refusal", true, Some("on the list"))
        )
      )
    }
  }
}
