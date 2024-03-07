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

import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentassurance.auth.AuthActions
import uk.gov.hmrc.agentassurance.models.AmlsJourney
import uk.gov.hmrc.agentassurance.services.AmlsJourneyService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmlsJourneyController @Inject()(amlsJourneyService: AmlsJourneyService,
                                      val authConnector: AuthConnector,
                                       override val controllerComponents: ControllerComponents)(
  implicit ec: ExecutionContext) extends BackendController(controllerComponents) with AuthActions {


  val key = DataKey[AmlsJourney]("amlsJourney")
  def getAmlsJourneyRecord: Action[AnyContent] = withAffinityGroupAgent { implicit request =>
      amlsJourneyService.get(key).map {
        case Some(journey) => Ok(Json.toJson(journey))
        case _ => NoContent
      }
    }
  def putAmlsJourneyRecord: Action[AnyContent] = withAffinityGroupAgent { implicit request =>

    request.body.asJson.map(_.validate[AmlsJourney]).map {
      case JsSuccess(amlsJourney, _) => amlsJourneyService.put(key, amlsJourney).map(_ => Accepted)
      case JsError(_) => Future successful BadRequest
    }.getOrElse(Future successful BadRequest)
  }

  def deleteAmlsJourneyRecord: Action[AnyContent] = withAffinityGroupAgent{ implicit request =>
    amlsJourneyService.delete(key).map(_ => NoContent)
  }


}
