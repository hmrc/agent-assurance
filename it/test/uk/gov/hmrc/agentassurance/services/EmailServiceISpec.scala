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

import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Writes
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.await
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.connectors.EmailConnector
import uk.gov.hmrc.agentassurance.models.EmailInformation
import uk.gov.hmrc.agentassurance.models.EntityCheckNotification
import uk.gov.hmrc.agentassurance.stubs.EmailStub
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier

class EmailServiceISpec(implicit tjs: Writes[EmailInformation])
    extends PlaySpec
    with MockFactory
    with DefaultAwaitTimeout
    with EmailStub {

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val appConfig: AppConfig = mock[AppConfig]
  val mockConnector: EmailConnector = mock[EmailConnector]
  val service: EmailService         = new EmailService(mockConnector, appConfig)

  "sendEntityCheckEmail" should {
    "be successful when request is valid" in {
      val entityChecks = EntityCheckNotification(
        arn = Arn("238799"),
        utr = Some("1248798"),
        agencyName = "Test Agent",
        failedChecks = "Agent is on the 'Refuse To Deal With' list",
        dateTime = "1 May 2024 1:56pm"
      )

      givenEmailSent(
        EmailInformation(
          to = Seq("test@example.com"),
          templateId = "entity_check_notification",
          parameters = Map(
            "arn"          -> entityChecks.arn.value,
            "utr"          -> entityChecks.utr.getOrElse(""),
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
