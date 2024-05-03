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

package uk.gov.hmrc.agentassurance.connectors

import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.ImplementedBy
import play.api.Logging
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.models.EmailInformation
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpErrorFunctions
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

@ImplementedBy(classOf[EmailConnectorImpl])
trait EmailConnector {
  def sendEmail(emailInformation: EmailInformation)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit]
}

class EmailConnectorImpl @Inject() (
    appConfig: AppConfig,
    httpClient: HttpClient,
    metrics: Metrics
) extends EmailConnector
    with HttpErrorFunctions
    with Logging {

  def sendEmail(emailInformation: EmailInformation)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val timer = metrics.defaultRegistry.timer(s"Send-Email-${emailInformation.templateId}")
    timer.time()

    httpClient
      .POST[EmailInformation, HttpResponse](s"${appConfig.emailBaseUrl}/hmrc/email", emailInformation)
      .map { response =>
        {
          timer.time().stop()
          response.status match {
            case status if is2xx(status) => ()
            case other =>
              logger.warn(s"unexpected status from email service, status: $other")
              ()
          }
        }
      }
  }

}
