/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.agentkyc.controllers

import javax.inject._

import play.api.mvc._
import uk.gov.hmrc.agentkyc.auth.AuthActions
import uk.gov.hmrc.agentkyc.connectors.DesConnector
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future

@Singleton
class KycController @Inject()(override val authConnector: AuthConnector, val desConnector: DesConnector) extends BaseController with AuthActions {
  def enrolledForIrSAAgent(): Action[AnyContent] = AuthorisedIRSAAgent { implicit request =>
    implicit saAgentRef =>
      Future successful NoContent
  }

  def activeCesaRelationship(nino: Nino): Action[AnyContent] = AuthorisedIRSAAgent { implicit request =>
    implicit saAgentRef =>
      desConnector.getActiveCesaAgentRelationships(nino).flatMap { activeAgentIds =>
        if (activeAgentIds.contains(saAgentRef)) Future successful NoContent else Future successful Forbidden
      }
  }
}
