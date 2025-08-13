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

import java.time.temporal.ChronoUnit
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.Logging
import uk.gov.hmrc.agentassurance.auth.AuthActions
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.connectors.DesConnector
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentassurance.models.AgentDetailsResponse
import uk.gov.hmrc.agentassurance.models.DmsSubmissionReference
import uk.gov.hmrc.agentassurance.services.DmsService
import uk.gov.hmrc.agentassurance.models.Arn
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton
class AgentServicesController @Inject() (
  desConnector: DesConnector,
  val authConnector: AuthConnector,
  dmsService: DmsService,
  cc: ControllerComponents
)(implicit
  val appConfig: AppConfig,
  ec: ExecutionContext
)
extends BackendController(cc)
with AuthActions
with Logging {

  private val strideRoles = Seq(appConfig.manuallyAssuredStrideRole)

  def getAgencyDetails(arn: Arn): Action[AnyContent] =
    withAffinityGroupAgentOrStride(strideRoles) { implicit request =>
      desConnector
        .getAgentRecord(arn)
        .map {
          case _ @AgentDetailsDesResponse(
                optUtr,
                Some(agencyDetails),
                _,
                _
              ) =>
            Ok(Json.toJson(AgentDetailsResponse(agencyDetails, optUtr)))
          case _ => NoContent
        }
    }

  def postAgencyDetails(arn: Arn): Action[AnyContent] =
    withAffinityGroupAgentOrStride(strideRoles) {
      implicit request =>
        for {
          dmsResponse <- dmsService.submitToDms(
            request.body.asText,
            Instant.now().truncatedTo(ChronoUnit.SECONDS),
            DmsSubmissionReference.create
          )
        } yield {
          logger.info(
            s"Dms Submission successful for ${arn.value}: ${dmsResponse.reference} at ${dmsResponse.processingDate}"
          )
          Created
        }
    }

}
