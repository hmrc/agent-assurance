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
import scala.concurrent.Future

import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.Result
import uk.gov.hmrc.agentassurance.auth.AuthActions
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.models.entitycheck.VerifyEntityRequest
import uk.gov.hmrc.agentassurance.services.EntityCheckService
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.SuspensionDetails
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton
class EntityCheckController @Inject() (
    cc: ControllerComponents,
    entityCheckService: EntityCheckService,
    val authConnector: AuthConnector,
    auth: BackendAuthComponents
)(implicit ec: ExecutionContext, appConfig: AppConfig)
    extends BackendController(cc)
    with AuthActions {

  private val predicate = Predicate.Permission(
    resource = Resource(
      resourceType = ResourceType(appConfig.appName),
      resourceLocation = ResourceLocation("client/verify-entity")
    ),
    action = IAAction("WRITE")
  )

  val internalAuth = auth.authorizedAction(predicate)

  // only for agents
  def agentVerifyEntity: Action[AnyContent] = AuthorisedWithArn { implicit request => arn: Arn =>
    entityCheckService
      .verifyAgent(arn)
      .map(x => createResponse(x.suspensionDetails))

  }

  // only for clients or stride
  def clientVerifyEntity: Action[JsValue] = internalAuth.async(parse.json) { implicit request =>
    request.body.validate[VerifyEntityRequest] match {
      case JsSuccess(value, _) =>
        entityCheckService
          .verifyAgent(value.identifier)
          .map(x => createResponse(x.suspensionDetails))
      case _ => Future.successful(BadRequest("Invalid Arn"))
    }
  }

  private val createResponse: Option[SuspensionDetails] => Result = {
    case None                                                           => NoContent
    case Some(suspensionDetails) if !suspensionDetails.suspensionStatus => NoContent
    case suspensionDetails                                              => Ok(Json.toJson(suspensionDetails))
  }
}
