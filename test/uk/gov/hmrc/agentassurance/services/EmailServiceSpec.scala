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

import scala.concurrent.ExecutionContext

import org.scalatestplus.play.PlaySpec
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.await
import uk.gov.hmrc.agentassurance.mocks.MockAppConfig
import uk.gov.hmrc.agentassurance.mocks.MockEmailConnector
import uk.gov.hmrc.agentassurance.models.EmailInformation
import uk.gov.hmrc.agentassurance.models.EntityCheckNotification
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier

class EmailServiceSpec extends PlaySpec with DefaultAwaitTimeout with MockEmailConnector with MockAppConfig {

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val service: EmailService         = new EmailService(mockEmailConnector, mockAppConfig)

  "sendEntityCheckEmail" should {
    "be successful when request is valid" in {
      val entityChecks = EntityCheckNotification(
        arn = Arn("238799"),
        utr = "1248798",
        agencyName = "Test Agent",
        failedChecks = "Agent is on the 'Refuse To Deal With' list",
        dateTime = "1 May 2024 1:56pm"
      )

      mockSendEmail(
        EmailInformation(
          to = Seq("test@example.com"),
          templateId = "entity_check_notification",
          parameters = Map(
            "arn"          -> entityChecks.arn.value,
            "utr"          -> entityChecks.utr,
            "agencyName"   -> entityChecks.agencyName,
            "failedChecks" -> entityChecks.failedChecks,
            "dateTime"     -> entityChecks.dateTime
          )
        )
      )

      await(service.sendEntityCheckNotification(entityChecks)) mustBe ()
    }
  }
}
