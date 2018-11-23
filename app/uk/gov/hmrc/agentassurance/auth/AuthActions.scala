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

package uk.gov.hmrc.agentassurance.auth

import play.api.Logger
import play.api.libs.json.{JsResultException, JsValue}
import play.api.mvc._
import play.api.mvc.BodyParsers.parse
import uk.gov.hmrc.agentassurance.controllers.ErrorResults.NoPermission
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.HeaderCarrierConverter.fromHeadersAndSession
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.{ExecutionContext, Future}

trait AuthActions extends AuthorisedFunctions {
  me: Results =>

  override def authConnector: AuthConnector

  private def getEnrolmentInfo(enrolment: Set[Enrolment], enrolmentKey: String, identifier: String): Option[String] =
    enrolment.find(_.key equals enrolmentKey).flatMap(_.identifiers.find(_.key equals identifier).map(_.value))

  private type AuthorisedRequestWithSaRef = Request[AnyContent] => SaAgentReference => Future[Result]
  private type AuthorisedRequestWithUserId = Request[AnyContent] => String => Future[Result]

  def AuthorisedIRSAAgent[A](body: AuthorisedRequestWithSaRef): Action[AnyContent] = Action.async {
    implicit request =>
      implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)
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
      implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)
      authorised(AuthProviders(GovernmentGateway)).retrieve(Retrievals.credentials) {
        case Credentials(providerId, _) => body(request)(providerId)
      } recover {
        case ex: NoActiveSession =>
          Logger.warn("NoActiveSession while trying to access check acceptable number of clients endpoint", ex)
          Unauthorized
        case _: JsResultException =>
          NoPermission
      }
  }

  def BasicAuth[A](body: Request[AnyContent] => Future[Result]): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)
    authorised() {
      body(request)
    } recoverWith {
      case ex: NoActiveSession =>
        Logger.warn("NoActiveSession while trying to access check activeCesaRelationship endpoint", ex)
        Future.successful(Unauthorized)
    }
  }

  def withAffinityGroupAgent(action: Request[AnyContent] => Future[Result]) = Action.async {
    implicit request =>
      implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)
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