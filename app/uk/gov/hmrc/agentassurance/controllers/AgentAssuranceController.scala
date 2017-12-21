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

package uk.gov.hmrc.agentassurance.controllers

import javax.inject._

import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.agentassurance.auth.AuthActions
import uk.gov.hmrc.agentassurance.connectors.{DesConnector, GovernmentGatewayConnector}
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.{Nino, TaxIdentifier}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton
class AgentAssuranceController @Inject()(
 @Named("minimumIRPAYEClients") minimumIRPAYEClients: Int,
 @Named("minimumIRSAClients") minimumIRSAClients: Int,
 override val authConnector: AuthConnector,
 val desConnector: DesConnector,
 val governmentGatewayConnector: GovernmentGatewayConnector) extends BaseController with AuthActions {

  def enrolledForIrSAAgent(): Action[AnyContent] = AuthorisedIRSAAgent { implicit request =>
    implicit saAgentRef =>
      Future successful NoContent
  }

  def activeCesaRelationship(clientType: String, clientId: String): Action[AnyContent] = AuthorisedIRSAAgent { implicit request =>
    implicit saAgentRef =>
      validateClientIdentifier(clientType, clientId) match {
        case Right(identifier) =>
          desConnector.getActiveCesaAgentRelationships(identifier).map { activeAgentIds =>
            if (activeAgentIds.contains(saAgentRef)) Ok else Forbidden
          }
        case Left(ex) =>
          Logger.warn(ex.getMessage)
          Future.successful(Forbidden)

      }
  }

  def acceptableNumberOfPAYEClients = acceptableNumberOfClients("IR-PAYE", minimumIRPAYEClients)

  def acceptableNumberOfIRSAClients = acceptableNumberOfClients("IR-SA", minimumIRSAClients)

  def acceptableNumberOfClients(service: String, minimumAcceptableNumberOfClients: Int): Action[AnyContent] =
    AuthorisedWithAgentCode { implicit request =>
    implicit agentCode =>
      governmentGatewayConnector.getClientCount(service, agentCode).flatMap { count =>
        if (count >= minimumAcceptableNumberOfClients)
          Future successful NoContent
        else
          Future successful Forbidden
      }
  }

  private def validateClientIdentifier(clientType: String, clientId: String): Either[Throwable, TaxIdentifier] = {
    val identifier = clientId.trim
    clientType.trim match {
      case "nino" =>
        Try(Nino(identifier)).map(Right(_)).recover { case ex => Left(ex) }.get
      case "utr" =>
        if (Utr.isValid(identifier))
          Right(Utr(identifier))
        else
          Left(new IllegalArgumentException(s"The utr $identifier is invalid"))
      case _ =>
        Left(new IllegalArgumentException(s"The client type $clientType is invalid"))
    }
  }
}
