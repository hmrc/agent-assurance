/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.agentassurance.auth

import play.api.Logger
import play.api.libs.json.JsResultException
import play.api.mvc._
import uk.gov.hmrc.agentassurance.controllers.ErrorResults.NoPermission
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.v2._
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}

trait AuthActions extends AuthorisedFunctions with BaseController {
  me: Results =>

  override def authConnector: AuthConnector

  implicit val ec: ExecutionContext

  private def getEnrolmentInfo(enrolment: Set[Enrolment], enrolmentKey: String, identifier: String): Option[String] =
    enrolment.find(_.key equals enrolmentKey).flatMap(_.identifiers.find(_.key equals identifier).map(_.value))

  private type AuthorisedRequestWithSaRef = Request[AnyContent] => SaAgentReference => Future[Result]
  private type AuthorisedRequestWithUserId = Request[AnyContent] => String => Future[Result]

  def AuthorisedIRSAAgent[A](body: AuthorisedRequestWithSaRef): Action[AnyContent] = Action.async {
    implicit request =>
      authorised(AuthProviders(GovernmentGateway)).retrieve(allEnrolments) {
        enrol =>
          getEnrolmentInfo(enrol.enrolments, "IR-SA-AGENT", "IRAgentReference") match {
            case Some(saAgentRef) => body(request)(SaAgentReference(saAgentRef))
            case _ => Future successful NoPermission
          }
      } recoverWith {
        case ex: NoActiveSession =>
          Logger.warn("NoActiveSession while trying to access check IR SA endpoint", ex)
          Future.successful(Unauthorized)
      }
  }

  def AuthorisedWithUserId[A](body: AuthorisedRequestWithUserId): Action[AnyContent] = Action.async {
    implicit request =>
      authorised(AuthProviders(GovernmentGateway)).retrieve(Retrievals.credentials) {
        case Some(Credentials(providerId, _)) => body(request)(providerId)
        case None =>
          Logger.warn("no credentials found for user")
          Future successful Unauthorized
      } recover {
        case ex: NoActiveSession =>
          Logger.warn("NoActiveSession while trying to access check acceptable number of clients endpoint", ex)
          Unauthorized
        case _: JsResultException =>
          NoPermission
      }
  }

  def BasicAuth[A](body: Request[AnyContent] => Future[Result]): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      body(request)
    } recoverWith {
      case ex: NoActiveSession =>
        Logger.warn("NoActiveSession while trying to access check activeCesaRelationship endpoint", ex)
        Future.successful(Unauthorized)
    }
  }

  def withAffinityGroupAgent(action: Request[AnyContent] => Future[Result]): Action[AnyContent] = Action.async {
    implicit request =>
      authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent) {
        action(request)
      }.recover {
          case _: NoActiveSession =>
            Unauthorized
          case _: UnsupportedAffinityGroup =>
            Logger.warn("user doesn't belong to Agent affinityGroup")
            Forbidden
        }
  }
}