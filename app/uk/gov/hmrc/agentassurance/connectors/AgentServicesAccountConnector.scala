/*
 * Copyright 2026 HM Revenue & Customs
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

import com.google.inject.ImplementedBy
import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import play.api.Logging
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import play.api.mvc.Request
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentassurance.models.AgentRecordUpdateRequest
import uk.gov.hmrc.agentassurance.models.Arn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@ImplementedBy(classOf[AgentServicesAccountConnectorImpl])
trait AgentServicesAccountConnector {

  def getAgentRecord(arn: Arn)(implicit hc: HeaderCarrier): Future[AgentDetailsDesResponse]

  def updateAmlsDetails(request: AgentRecordUpdateRequest)(implicit
    request0: Request[?],
    hc: HeaderCarrier
  ): Future[Unit]

}

@Singleton
class AgentServicesAccountConnectorImpl @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
extends AgentServicesAccountConnector
with BaseConnector
with Logging {

  private val baseUrl = appConfig.agentServicesAccountBaseUrl

  override def getAgentRecord(arn: Arn)(implicit hc: HeaderCarrier): Future[AgentDetailsDesResponse] = httpClient
    .get(url"$baseUrl/agent-services-account/agent-record-with-checks/arn/${arn.value}")
    .setHeader("Authorization" -> appConfig.internalAuthToken)
    .executeAndDeserialise[AgentDetailsDesResponse]

  override def updateAmlsDetails(request: AgentRecordUpdateRequest)(implicit
    request0: Request[?],
    hc: HeaderCarrier
  ): Future[Unit] = httpClient
    .put(url"$baseUrl/agent-services-account/agent-record-update")
    .withBody(Json.toJson(request))
    .executeAndExpect(200)

}
