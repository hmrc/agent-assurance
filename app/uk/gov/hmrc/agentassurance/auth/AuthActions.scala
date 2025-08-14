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

package uk.gov.hmrc.agentassurance.auth

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.libs.json.JsResultException
import play.api.mvc._
import play.api.Logger
import uk.gov.hmrc.agentassurance.controllers.ErrorResults.NoPermission
import uk.gov.hmrc.agentassurance.models.Arn
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.affinityGroup
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.allEnrolments
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.credentials
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

trait AuthActions
extends AuthorisedFunctions
with BaseController {
  me: Results =>

  private val logger = Logger(this.getClass)

  override def authConnector: AuthConnector

  private def getEnrolmentInfo(
    enrolment: Set[Enrolment],
    enrolmentKey: String,
    identifier: String
  ): Option[String] = enrolment.find(_.key.equals(enrolmentKey)).flatMap(_.identifiers.find(_.key.equals(identifier)).map(_.value))

  def hasRequiredStrideRole(
    enrolments: Enrolments,
    strideRoles: Seq[String]
  ): Boolean = strideRoles.exists(s => enrolments.enrolments.exists(_.key == s))

  private type AuthorisedRequestWithSaRef = Request[AnyContent] => SaAgentReference => Future[Result]
  private type AuthorisedRequestWithUserId = Request[AnyContent] => String => Future[Result]
  private type AuthorisedRequestWithArn = Request[AnyContent] => Arn => Future[Result]

  def AuthorisedIRSAAgent[A](body: AuthorisedRequestWithSaRef)(implicit ec: ExecutionContext): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    authorised(AuthProviders(GovernmentGateway))
      .retrieve(allEnrolments) { enrol =>
        getEnrolmentInfo(
          enrol.enrolments,
          "IR-SA-AGENT",
          "IRAgentReference"
        ) match {
          case Some(saAgentRef) => body(request)(SaAgentReference(saAgentRef))
          case _ => Future.successful(NoPermission)
        }
      }
      .recoverWith {
        case ex: NoActiveSession =>
          logger.warn("NoActiveSession while trying to access check IR SA endpoint", ex)
          Future.successful(Unauthorized)
      }
  }
  def AuthorisedWithArn[A](body: AuthorisedRequestWithArn)(implicit ec: ExecutionContext): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    authorised(AuthProviders(GovernmentGateway))
      .retrieve(allEnrolments) { enrol =>
        getEnrolmentInfo(
          enrol.enrolments,
          "HMRC-AS-AGENT",
          "AgentReferenceNumber"
        ) match {
          case Some(arn) => body(request)(Arn(arn))
          case _ => Future.successful(NoPermission)
        }
      }
      .recoverWith {
        case ex: NoActiveSession =>
          logger.warn("NoActiveSession", ex)
          Future.successful(Unauthorized)
      }
  }

  def AuthorisedWithUserId[A](body: AuthorisedRequestWithUserId)(implicit ec: ExecutionContext): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    authorised(AuthProviders(GovernmentGateway))
      .retrieve(Retrievals.credentials) {
        case Some(Credentials(providerId, _)) => body(request)(providerId)
        case None => Future.successful(NoPermission)
      }
      .recover {
        case ex: NoActiveSession =>
          logger.warn("NoActiveSession while trying to access check acceptable number of clients endpoint", ex)
          Unauthorized
        case _: JsResultException => NoPermission
      }
  }

  def BasicAuth[A](parser: BodyParser[A])(body: Request[A] => Future[Result])(implicit ec: ExecutionContext): Action[A] =
    Action.async(parser) { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
      authorised() {
        body(request)
      }.recoverWith {
        case ex: NoActiveSession =>
          logger.warn("NoActiveSession while trying to access check activeCesaRelationship endpoint", ex)
          Future.successful(Unauthorized)
      }
    }

  def BasicAuth[A](body: Request[AnyContent] => Future[Result])(implicit ec: ExecutionContext): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    authorised() {
      body(request)
    }.recoverWith {
      case ex: NoActiveSession =>
        logger.warn("NoActiveSession while trying to access check activeCesaRelationship endpoint", ex)
        Future.successful(Unauthorized)
    }
  }

  def withAffinityGroupAgent(
    action: Request[AnyContent] => Future[Result]
  )(implicit ec: ExecutionContext): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    authorised(AuthProviders(GovernmentGateway).and(AffinityGroup.Agent)) {
      action(request)
    }.recover {
      case _: NoActiveSession => Unauthorized
      case _: UnsupportedAffinityGroup =>
        logger.warn("user doesn't belong to Agent affinityGroup")
        Forbidden
    }
  }

  def withAffinityGroupAgentOrStride(strideRoles: Seq[String])(
    action: Request[AnyContent] => Future[Result]
  )(implicit ec: ExecutionContext): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    authorised().retrieve(allEnrolments.and(affinityGroup).and(credentials)) {
      case enrolments ~ affinityGroup ~ optCreds =>
        optCreds
          .collect {
            case creds @ Credentials(_, "GovernmentGateway") if affinityGroup.contains(AffinityGroup.Agent) => creds
            case creds @ Credentials(_, "PrivilegedApplication") if hasRequiredStrideRole(enrolments, strideRoles) => creds
          }
          .map(_ => action(request))
          .getOrElse(Future.successful(Forbidden))
    }
  }

}
