/*
 * Copyright 2023 HM Revenue & Customs
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

import akka.NotUsed
import com.typesafe.config.Config
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.http.Status.ACCEPTED
import play.api.mvc.MultipartFormData
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DmsConnector @Inject()(
  httpClient: HttpClientV2,
  appConfig: AppConfig,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
    extends BaseConnector {

  private def dmsHeaders: (String, String) = HeaderNames.authorisation -> appConfig.internalAuthToken

  def sendPdf(
    body: Source[MultipartFormData.Part[Source[ByteString, NotUsed]], NotUsed]
  )(implicit hc: HeaderCarrier): Future[Unit] =
    retryFor[Unit]("DMS submission")(retryCondition) {
      httpClient
        .post(url"${appConfig.dmsSubmissionUrl}")
        .setHeader(dmsHeaders)
        .withBody(body)
        .executeAndExpect(ACCEPTED)
    }
}
