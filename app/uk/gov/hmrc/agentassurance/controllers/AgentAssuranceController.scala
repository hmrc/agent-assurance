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

import javax.inject._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.libs.json.JsError
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import play.api.mvc._
import play.api.Logger
import uk.gov.hmrc.agentassurance.auth.AuthActions
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.connectors.DesConnector
import uk.gov.hmrc.agentassurance.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.agentassurance.models.AmlsError._
import uk.gov.hmrc.agentassurance.repositories.AmlsRepository
import uk.gov.hmrc.agentassurance.repositories.OverseasAmlsRepository
import uk.gov.hmrc.agentassurance.util.toFuture
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton
class AgentAssuranceController @Inject() (
  override val authConnector: AuthConnector,
  val desConnector: DesConnector,
  val espConnector: EnrolmentStoreProxyConnector,
  val overseasAmlsRepository: OverseasAmlsRepository,
  cc: ControllerComponents,
  val amlsRepository: AmlsRepository
)(implicit
  val appConfig: AppConfig,
  ec: ExecutionContext
)
extends BackendController(cc)
with AuthActions {

  private val minimumIRPAYEClients = appConfig.minimumIRPAYEClients
  private val minimumIRSAClients = appConfig.minimumIRSAClients
  private val minimumVatDecOrgClients = appConfig.minimumVatDecOrgClients
  private val minimumIRCTClients = appConfig.minimumIRCTClients
  private val strideRoles = Seq(appConfig.manuallyAssuredStrideRole)

  private val logger = Logger(this.getClass)

  def enrolledForIrSAAgent(): Action[AnyContent] = AuthorisedIRSAAgent { _ => _ => Future.successful(NoContent) }

  def activeCesaRelationshipWithUtr(
    utr: Utr,
    saAgentReference: SaAgentReference
  ): Action[AnyContent] = activeCesaRelationship(utr, saAgentReference)

  def activeCesaRelationshipWithNino(
    nino: Nino,
    saAgentReference: SaAgentReference
  ): Action[AnyContent] = activeCesaRelationship(nino, saAgentReference)

  private def activeCesaRelationship(
    identifier: TaxIdentifier,
    saAgentReference: SaAgentReference
  ): Action[AnyContent] = BasicAuth { implicit request =>
    desConnector.getActiveCesaAgentRelationships(identifier).map {
      case agentRefs if agentRefs.contains(saAgentReference) => Ok
      case _ => Forbidden
    }
  }

  def acceptableNumberOfPAYEClients = acceptableNumberOfClients("IR-PAYE", minimumIRPAYEClients)

  def acceptableNumberOfIRSAClients = acceptableNumberOfClients("IR-SA", minimumIRSAClients)

  def acceptableNumberOfVatDecOrgClients: Action[AnyContent] = acceptableNumberOfClients("HMCE-VATDEC-ORG", minimumVatDecOrgClients)

  def acceptableNumberOfIRCTClients: Action[AnyContent] = acceptableNumberOfClients("IR-CT", minimumIRCTClients)

  def acceptableNumberOfClients(
    service: String,
    minimumAcceptableNumberOfClients: Int
  ): Action[AnyContent] = AuthorisedWithUserId { implicit request => implicit userId =>
    espConnector.getClientCount(service, userId).flatMap { count =>
      if (count >= minimumAcceptableNumberOfClients)
        Future.successful(NoContent)
      else
        Future.successful(Forbidden)
    }
  }

  def storeAmlsDetails: Action[AnyContent] =
    withAffinityGroupAgentOrStride(strideRoles) { implicit request =>
      request.body.asJson.map(_.validate[CreateAmlsRequest]) match {
        case Some(JsSuccess(createAmlsRequest, _)) =>
          if (Utr.isValid(createAmlsRequest.utr.value)) {
            amlsRepository.createOrUpdate(createAmlsRequest).map {
              case Right(_) => Created
              case Left(error) =>
                error match {
                  case ArnAlreadySetError => Forbidden
                  case _ => InternalServerError
                }
            }
          }
          else {
            BadRequest("utr is not valid")
          }
        case Some(JsError(_)) => BadRequest("Could not parse AmlsDetails JSON in request")
        case None => BadRequest("No JSON found in request body")
      }
    }

  def storeOverseasAmlsDetails: Action[AnyContent] = withAffinityGroupAgent { implicit request =>
    request.body.asJson.map(_.validate[OverseasAmlsEntity]) match {
      case Some(JsSuccess(amlsEntity, _)) =>
        if (Arn.isValid(amlsEntity.arn.value)) {
          overseasAmlsRepository.create(amlsEntity).map {
            case Right(_) => Created
            case Left(AmlsRecordExists) => Conflict
            case Left(error) =>
              logger.warn(s"Creating overseas amls details failed with error: $error")
              InternalServerError
          }
        }
        else {
          BadRequest("Invalid Arn")
        }
      case Some(JsError(_)) => BadRequest("Could not parse JSON in request")
      case None => BadRequest("No JSON found in request body")
    }
  }

  private def is5xx(u: UpstreamErrorResponse): Boolean = u.statusCode >= 500 && u.statusCode < 600

  def getAmlsSubscription(amlsRegistrationNumber: String): Action[AnyContent] = Action.async { implicit request =>
    desConnector.getAmlsSubscriptionStatus(amlsRegistrationNumber).map(amls => Ok(Json.toJson(amls))).recover {
      case e: UpstreamErrorResponse if e.statusCode == 404 => NotFound
      case e: UpstreamErrorResponse if is5xx(e) => {
        logger.warn(s"DES return status ${e.statusCode} ${e.message}")
        InternalServerError
      }
    }
  }

  def getAmlsDetails(utr: Utr): Action[AnyContent] =
    withAffinityGroupAgentOrStride(strideRoles) { _ =>
      amlsRepository
        .getAmlDetails(utr)
        .map {
          case Some(details) => Ok(Json.toJson(details))
          case _ => NotFound
        }
    }

  def updateAmlsDetails(utr: Utr): Action[AnyContent] = withAffinityGroupAgent { implicit request =>
    request.body.asJson.map(_.validate[Arn]) match {
      case Some(JsSuccess(arn, _)) =>
        if (Arn.isValid(arn.value)) {
          amlsRepository.updateArn(utr, arn).map {
            case Right(updated) => Ok(Json.toJson(updated))
            case Left(error) =>
              error match {
                case ArnAlreadySetError => Forbidden
                case NoExistingAmlsError => NotFound
                case UniqueKeyViolationError => BadRequest
                case _ => InternalServerError
              }
          }
        }
        else {
          BadRequest("invalid Arn value")
        }

      case Some(JsError(_)) => BadRequest("Could not parse Arn JSON in request")
      case None => BadRequest("No JSON found in request body")
    }
  }

}
