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

import play.api.libs.json.JsError
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import play.api.Logging
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.models.dms.DmsNotification
import uk.gov.hmrc.agentassurance.models.dms.SubmissionItemStatus
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton
class DmsNotificationController @Inject() (
    cc: ControllerComponents,
    auth: BackendAuthComponents,
    appConfig: AppConfig
) extends BackendController(cc)
    with Logging {

  private val predicate = Predicate.Permission(
    resource = Resource(
      resourceType = ResourceType(appConfig.appName),
      resourceLocation = ResourceLocation("dms-agent-callback")
    ),
    action = IAAction("WRITE")
  )

  private val authorised = auth.authorizedAction(predicate)

  def dmsCallback: Action[JsValue] = authorised(parse.json) { implicit request =>
    request.body.validate[DmsNotification] match {
      case JsSuccess(notification, _) =>
        if (notification.status == SubmissionItemStatus.Failed) {
          logger.error(
            s"DMS notification error received for ${notification.id} with error: ${notification.failureReason
                .getOrElse("")}"
          )
        } else {
          logger.info(
            s"DMS notification received for ${notification.id} with status ${notification.status}"
          )
        }
        Ok
      case JsError(_) =>
        BadRequest
    }
  }
}
