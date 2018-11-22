/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject._
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess}
import play.api.mvc._
import uk.gov.hmrc.agentassurance.auth.AuthActions
import uk.gov.hmrc.agentassurance.connectors.{DesConnector, EnrolmentStoreProxyConnector}
import uk.gov.hmrc.agentassurance.model.toFuture
import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.agentassurance.repositories.AmlsRepository
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.{Nino, SaAgentReference, TaxIdentifier}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class AgentAssuranceController @Inject()(
                                          @Named("minimumIRPAYEClients") minimumIRPAYEClients: Int,
                                          @Named("minimumIRSAClients") minimumIRSAClients: Int,
                                          override val authConnector: AuthConnector,
                                          val desConnector: DesConnector,
                                          val espConnector: EnrolmentStoreProxyConnector,
                                          val amlsRepository: AmlsRepository) extends BaseController with AuthActions {

  def enrolledForIrSAAgent(): Action[AnyContent] = AuthorisedIRSAAgent { implicit request =>
    implicit saAgentRef =>
      Future successful NoContent
  }

  def activeCesaRelationshipWithUtr(utr: Utr, saAgentReference: SaAgentReference): Action[AnyContent] =
    activeCesaRelationship(utr, saAgentReference)

  def activeCesaRelationshipWithNino(nino: Nino, saAgentReference: SaAgentReference): Action[AnyContent] =
    activeCesaRelationship(nino, saAgentReference)

  private def activeCesaRelationship(identifier: TaxIdentifier, saAgentReference: SaAgentReference): Action[AnyContent] =
    BasicAuth { implicit request =>
      desConnector.getActiveCesaAgentRelationships(identifier).map { agentRefs =>
        if (agentRefs.contains(saAgentReference)) Ok else Forbidden
      }
    }

  def acceptableNumberOfPAYEClients = acceptableNumberOfClients("IR-PAYE", minimumIRPAYEClients)

  def acceptableNumberOfIRSAClients = acceptableNumberOfClients("IR-SA", minimumIRSAClients)

  def acceptableNumberOfClients(service: String, minimumAcceptableNumberOfClients: Int): Action[AnyContent] =
    AuthorisedWithUserId { implicit request =>
      implicit userId =>
        espConnector.getClientCount(service, userId).flatMap { count =>
          if (count >= minimumAcceptableNumberOfClients)
            Future successful NoContent
          else
            Future successful Forbidden
        }
    }

  def storeAmlsDetails: Action[AnyContent] = withAffinityGroupAgent { implicit request =>
    request.body.asJson.map(_.validate[AmlsDetails]) match {
      case Some(JsSuccess(amlsDetails, _)) ⇒
        amlsRepository.createOrUpdate(amlsDetails).map {
          case Right(_) => Created
          case Left(error) =>
            Logger.warn(s"error during creating amls record, error  = $error")
            InternalServerError
        }

      case Some(JsError(_)) ⇒
        BadRequest("Could not parse AmlsDetails JSON in request")
      case None ⇒
        BadRequest("No JSON found in request body")
    }
  }
}
