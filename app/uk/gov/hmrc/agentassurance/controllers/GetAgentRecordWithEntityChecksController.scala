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

package uk.gov.hmrc.agentassurance.controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.agentassurance.auth.AuthActions
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.services.EntityCheckService
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.internalauth.client.BackendAuthComponents
import uk.gov.hmrc.internalauth.client.IAAction
import uk.gov.hmrc.internalauth.client.Predicate
import uk.gov.hmrc.internalauth.client.Resource
import uk.gov.hmrc.internalauth.client.ResourceLocation
import uk.gov.hmrc.internalauth.client.ResourceType
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton
class GetAgentRecordWithEntityChecksController @Inject() (
    cc: ControllerComponents,
    entityCheckService: EntityCheckService,
    val authConnector: AuthConnector,
    auth: BackendAuthComponents
)(implicit ec: ExecutionContext, appConfig: AppConfig)
    extends BackendController(cc)
    with AuthActions {

  def get: Action[AnyContent] = AuthorisedWithArn { implicit request => arn: Arn =>
    entityCheckService.verifyAgent(arn).map(entityCheckResult => Ok(Json.toJson(entityCheckResult.agentRecord)))
  }

  private val predicate = Predicate.Permission(
    resource = Resource(
      resourceType = ResourceType(appConfig.appName),
      resourceLocation = ResourceLocation("agent-record-with-checks/arn")
    ),
    action = IAAction("WRITE")
  )

  private val internalAuth = auth.authorizedAction(predicate)

  def clientGet(arn: Arn): Action[AnyContent] = internalAuth.async { implicit request =>
    entityCheckService.verifyAgent(arn).map(entityCheckResult => Ok(Json.toJson(entityCheckResult.agentRecord)))
  }
}
