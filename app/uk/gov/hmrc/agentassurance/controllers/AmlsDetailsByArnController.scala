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
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.models.{AmlsRequest, OverseasAmlsDetails, OverseasAmlsDetailsResponse, UkAmlsDetails, UkAmlsDetailsResponse}
import uk.gov.hmrc.agentassurance.services.AmlsDetailsService
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmlsDetailsByArnController @Inject()(amlsDetailsService: AmlsDetailsService,
                                           val authConnector: AuthConnector,
                                           override val controllerComponents: ControllerComponents
                                          )(implicit val appConfig: AppConfig, ec: ExecutionContext) extends BackendController(controllerComponents) with AuthActions {

  private val strideRoles = Seq(appConfig.manuallyAssuredStrideRole)

  def getAmlsDetails(arn: Arn): Action[AnyContent] =
    withAffinityGroupAgentOrStride(strideRoles) { implicit request =>
      amlsDetailsService.getAmlsDetailsByArn(arn).map {
        case (amlsStatus, None) =>
          Ok(Json.toJson(UkAmlsDetailsResponse(amlsStatus.toString)))
        case (amlsStatus, Some(amlsDetails: UkAmlsDetails)) =>
          Ok(Json.toJson(UkAmlsDetailsResponse(amlsStatus.toString, Some(amlsDetails))))
        case (amlsStatus, Some(overseasAmlsDetails: OverseasAmlsDetails)) =>
          Ok(Json.toJson(OverseasAmlsDetailsResponse(amlsStatus.toString, Some(overseasAmlsDetails))))
      }
    }

  def postAmlsDetails(arn: Arn): Action[AnyContent] = withAffinityGroupAgent {
    implicit request =>
      request.body.asJson.map {
        _.validate[AmlsRequest] match {
          case JsSuccess(amlsRequest, _) =>
            amlsDetailsService.storeAmlsRequest(arn, amlsRequest).map {
              case Right(_) => Created
              case Left(error) =>
                throw new InternalServerException(s"[AmlsDetailsByArnController][postAmlsDetails] failed to store new AMLS details. Error - ${error.toString}")
            }
          case JsError(errors) =>
            Future.successful(BadRequest(s"[AmlsDetailsByArnController][postAmlsDetails] Could not parse JSON body: $errors"))
        }
      }.getOrElse(Future.successful(BadRequest("[AmlsDetailsByArnController][postAmlsDetails] No JSON found in request")))
  }

}
