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
import uk.gov.hmrc.agentassurance.connectors.DesConnector
import uk.gov.hmrc.agentassurance.models.{AgentDetailsDesResponse, AgentDetailsResponse}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AgentServicesController @Inject()(
                                         desConnector: DesConnector,
                                         val authConnector: AuthConnector,
                                         cc: ControllerComponents
                                       )
(implicit val appConfig: AppConfig, ec: ExecutionContext) extends BackendController(cc) with AuthActions{

  private val strideRoles = Seq(appConfig.manuallyAssuredStrideRole)

  def getAgencyDetails(arn: Arn): Action[AnyContent] = withAffinityGroupAgentOrStride(strideRoles) {  implicit request =>
    desConnector
      .getAgentRecord(arn)
      .map {
      case _@AgentDetailsDesResponse(optUtr, Some(agencyDetails), _) => Ok(Json.toJson(AgentDetailsResponse(agencyDetails, optUtr)))
      case _ => NoContent
    }
  }
}
