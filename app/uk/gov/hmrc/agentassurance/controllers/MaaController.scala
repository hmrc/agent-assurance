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

import javax.inject.{Inject, Singleton}

import play.api.libs.json.Json
import play.api.mvc.Action
import uk.gov.hmrc.agentassurance.auth.AuthActions
import uk.gov.hmrc.agentassurance.binders.PaginationParameters
import uk.gov.hmrc.agentassurance.model.{ErrorBody, Value}
import uk.gov.hmrc.agentassurance.models.pagination.{PaginatedResources, PaginationLinks}
import uk.gov.hmrc.agentassurance.repositories.PropertiesRepository
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class MaaController @Inject()(repository: PropertiesRepository,
                              val authConnector: AuthConnector) extends BaseController with AuthActions {

  val key = "manually-assured"

  def createProperty = BasicAuth { implicit request =>
    val newValue = request.body.asJson
      .getOrElse(throw new BadRequestException("could not find or recognise json"))
      .validate[Value].getOrElse(throw new BadRequestException("json failed validation"))

    Utr.isValid(newValue.value) match {
      case true => {
        val propertyConverted = newValue.toProperty(key)
        repository.propertyExists(propertyConverted).flatMap {
          case false => repository.createProperty(propertyConverted).map(_ => Created)
          case true => Future.successful(Conflict(Json.toJson(ErrorBody("PROPERTY_EXISTS", "Property already exists"))))
        }
      }
      case false => Future.successful(BadRequest(Json.toJson(ErrorBody("INVALID_UTR", "You must provide a valid UTR"))))
    }
  }


  def isManuallyAssured(identifier: Utr) = BasicAuth { implicit request =>
    repository.propertyExists(Value(identifier.value).toProperty(key)).map {
      case true => Ok
      case false => Forbidden
    }
  }

  def getMaaList(pagination: PaginationParameters) = BasicAuth { implicit request =>
    repository.findProperties(key, pagination.page, pagination.pageSize).map { case (total, properties) =>
      val response = PaginatedResources(
        PaginationLinks.apply(paginationParams = pagination,
          total = total,
          paginatedLinkBuilder = pp => routes.MaaController.getMaaList(pp).absoluteURL()),
        pagination.page,
        pagination.pageSize,
        total,
        properties.map(_.value)
      )

      Ok(Json.toJson(response))
    }
  }

  def deleteProperty(identifier: Utr) = Action.async { implicit request =>
    val newProperty = Value(identifier.value).toProperty(key)
    repository.propertyExists(newProperty).flatMap {
      case true => repository.deleteProperty(newProperty).map(_ => NoContent)
      case false => Future.successful(NotFound)
    }
  }
}