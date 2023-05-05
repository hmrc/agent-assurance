/*
 * Copyright 2023 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentassurance.auth.AuthActions
import uk.gov.hmrc.agentassurance.binders.PaginationParameters
import uk.gov.hmrc.agentassurance.models.pagination.{PaginatedResources, PaginationLinks}
import uk.gov.hmrc.agentassurance.models.{ErrorBody, Value}
import uk.gov.hmrc.agentassurance.repositories.PropertiesRepository
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class R2dwController @Inject()(repository: PropertiesRepository,
                               cc: ControllerComponents,
                               val authConnector: AuthConnector)(implicit ec: ExecutionContext) extends BackendController(cc) with AuthActions {

  val key = "refusal-to-deal-with"

  def createProperty = BasicAuth { implicit request =>
    val newValue = request.body.asJson
      .getOrElse(throw new BadRequestException("could not find or recognise json"))
      .validate[Value].getOrElse(throw new BadRequestException("json failed validation"))

    Utr.isValid(newValue.value) match {
      case true =>
        val propertyConverted = newValue.toProperty(key)
        repository.propertyExists(propertyConverted).flatMap {
          case false => repository.createProperty(propertyConverted).map(_ => Created)
          case true => Future.successful(Conflict(Json.toJson(ErrorBody("PROPERTY_EXISTS", "Property already exists"))))
        }
      case false => Future.successful(BadRequest(Json.toJson(ErrorBody("INVALID_UTR", "You must provide a valid UTR"))))
    }
  }


  def isOnR2dwList(identifier: Utr): Action[AnyContent] = BasicAuth { _ =>
    repository.propertyExists(Value(identifier.value).toProperty(key)).map {
      case true => Forbidden
      case false => Ok
    }
  }

  def getR2dwList(pagination: PaginationParameters) = BasicAuth { implicit request =>
    repository.findProperties(key, pagination.page, pagination.pageSize).map { case (total, properties) =>
      val response = PaginatedResources(
        PaginationLinks.apply(paginationParams = pagination,
          total = total,
          paginatedLinkBuilder = pp => routes.R2dwController.getR2dwList(pp).absoluteURL()),
        pagination.page,
        pagination.pageSize,
        total,
        properties
      )

      Ok(Json.toJson(response))
    }
  }

  def deleteProperty(identifier: Utr) = BasicAuth { _ =>
    val property = Value(identifier.value).toProperty(key)

    repository.propertyExists(property).flatMap {
      case true => repository.deleteProperty(property).map(_ => NoContent)
      case false => Future.successful(NotFound)
    }
  }
}