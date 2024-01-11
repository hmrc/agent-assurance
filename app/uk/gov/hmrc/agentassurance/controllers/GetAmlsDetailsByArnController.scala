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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentassurance.auth.AuthActions
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.models.{OverseasAmlsDetails, UkAmlsDetails}
import uk.gov.hmrc.agentassurance.services.AmlsDetailsService
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class GetAmlsDetailsByArnController @Inject()(amlsDetailsService: AmlsDetailsService,
                                              val authConnector: AuthConnector,
                                              override val controllerComponents: ControllerComponents
                                             )(implicit val appConfig: AppConfig, ec: ExecutionContext) extends BackendController(controllerComponents) with AuthActions {

  private val strideRoles = Seq(appConfig.manuallyAssuredStrideRole)

  def getAmlsDetails(arn: Arn): Action[AnyContent] =
    withAffinityGroupAgentOrStride(strideRoles) {
      request =>
        amlsDetailsService.getAmlsDetailsByArn(arn).map {
          case Nil => NotFound // Should this be NoContent as this isn't an client side issue
          case Seq(amlsDetails@UkAmlsDetails(_, _, _, _, _, _)) => Ok(Json.toJson(amlsDetails))
          case Seq(overseasAmlsDetails@OverseasAmlsDetails(_, _)) => Ok(Json.toJson(overseasAmlsDetails))
          case _ => throw new InternalServerException("[getAmlsDetailsByArnController][getAmlsDetails] ARN has both Overseas and UK AMLS details")
        }

    }

}
