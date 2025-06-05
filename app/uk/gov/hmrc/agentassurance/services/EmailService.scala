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

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.connectors.EmailConnector
import uk.gov.hmrc.agentassurance.models.EmailInformation
import uk.gov.hmrc.agentassurance.models.EntityCheckNotification
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class EmailService @Inject() (
  emailConnector: EmailConnector,
  appConfig: AppConfig
) {
  def sendEntityCheckNotification(
    entityCheckNotification: EntityCheckNotification
  )(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Unit] = {
    emailConnector.sendEmail(
      EmailInformation(
        to = Seq(appConfig.agentMaintainerEmail),
        templateId = "entity_check_notification",
        parameters = Map(
          "agencyName" -> entityCheckNotification.agencyName,
          "arn" -> entityCheckNotification.arn.value,
          "utr" -> entityCheckNotification.utr,
          "failedChecks" -> entityCheckNotification.failedChecks,
          "dateTime" -> entityCheckNotification.dateTime
        )
      )
    )
  }

}
