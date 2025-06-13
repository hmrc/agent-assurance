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

import play.api.libs.json.JsError
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.agentassurance.auth.AuthActions
import uk.gov.hmrc.agentassurance.binders.PaginationParameters
import uk.gov.hmrc.agentassurance.models.ErrorBody
import uk.gov.hmrc.agentassurance.models.Value
import uk.gov.hmrc.agentassurance.models.pagination.PaginatedResources
import uk.gov.hmrc.agentassurance.models.pagination.PaginationLinks
import uk.gov.hmrc.agentassurance.models.utrcheck.BusinessNameByUtr._
import uk.gov.hmrc.agentassurance.models.utrcheck.CollectionName
import uk.gov.hmrc.agentassurance.models.utrcheck.UtrChecksResponse
import uk.gov.hmrc.agentassurance.repositories.PropertiesRepository
import uk.gov.hmrc.agentassurance.services.BusinessNamesService
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class UtrChecksController @Inject() (
  repository: PropertiesRepository,
  businessNamesService: BusinessNamesService,
  override val controllerComponents: ControllerComponents,
  val authConnector: AuthConnector
)(implicit ec: ExecutionContext)
extends BackendController(controllerComponents)
with AuthActions {

  def getUtrList(
    pagination: PaginationParameters,
    key: CollectionName
  ): Action[AnyContent] = BasicAuth {
    implicit request =>
      for {
        (total, utrs) <- repository.findProperties(
          key.toString,
          pagination.page,
          pagination.pageSize
        )
        businessNamesByUtrSet <- businessNamesService.get(utrs)
      } yield {
        val paginatedResources = PaginatedResources(
          _links = PaginationLinks.apply(
            paginationParams = pagination,
            total = total,
            paginatedLinkBuilder = pp => routes.UtrChecksController.getUtrList(pp, key).absoluteURL()
          ),
          page = pagination.page,
          pageSize = pagination.pageSize,
          total = total,
          resources = businessNamesByUtrSet.toSeq
        )
        Ok(Json.toJson(paginatedResources))

      }
  }

  def utrChecks(
    utr: Utr,
    nameRequired: Boolean
  ) = BasicAuth { implicit request =>
    for {
      isManuallyAssured <- repository.propertyExists(Value(utr.value).toProperty(CollectionName.ManuallyAssured.toString))
      isRefusalToDealWith <- repository.propertyExists(Value(utr.value).toProperty(CollectionName.RefusalToDealWith.toString))
      businessName <-
        if (nameRequired)
          businessNamesService.get(utr.value)
        else
          Future.successful(None)
    } yield {

      val utrChecksResponse = UtrChecksResponse(
        isManuallyAssured = isManuallyAssured,
        isRefusalToDealWith = isRefusalToDealWith,
        businessName = businessName
      )
      Ok(Json.toJson(utrChecksResponse))
    }
  }

  def addUtr(key: CollectionName): Action[JsValue] =
    BasicAuth(parse.json) { implicit request =>
      request.body.validate[Value].fold(
        errors => {
          Future.successful(BadRequest(Json.toJson(ErrorBody("INVALID_JSON", JsError.toJson(errors).toString))))
        },
        newValue => {
          Utr.isValid(newValue.value) match {
            case true =>
              val propertyConverted = newValue.toProperty(key.toString)
              repository.propertyExists(propertyConverted).flatMap {
                case false => repository.createProperty(propertyConverted).map(_ => Created)
                case true => Future.successful(Conflict(Json.toJson(ErrorBody("PROPERTY_EXISTS", "Property already exists"))))
              }
            case false => Future.successful(BadRequest(Json.toJson(ErrorBody("INVALID_UTR", "You must provide a valid UTR"))))
          }
        }
      )
    }

  def removeUtr(
    identifier: Utr,
    key: CollectionName
  ): Action[AnyContent] = BasicAuth { _ =>
    val property = Value(identifier.value).toProperty(key.toString)

    repository.propertyExists(property).flatMap {
      case true => repository.deleteProperty(property).map(_ => NoContent)
      case false => Future.successful(NotFound)
    }
  }

}
