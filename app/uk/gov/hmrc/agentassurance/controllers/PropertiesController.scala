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
import uk.gov.hmrc.agentassurance.repositories.PropertiesRepository
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.agentassurance.model._

import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

@Singleton
class PropertiesController @Inject()(repository: PropertiesRepository) extends BaseController {


  def createProperty = Action.async(parse.json) { implicit request =>
    withJsonBody[Property] { property =>
      repository.findProperty(property.key).flatMap { op =>
        if (op.isEmpty) {
          repository.createProperty(property).map(_ => Created)
        } else {
          Future successful BadRequest(Json.toJson(ErrorBody("PROPERTY_EXISTS", "Property already exists")))
        }
      }
    }
  }

  def updateProperty(key: String) = Action.async(parse.json) { implicit request =>
    withJsonBody[Value] { value =>
      repository.updateProperty(value.toProperty(key)).map { updated =>
        if (updated) NoContent else NotFound
      }
    }
  }

  def getProperty(key: String) = Action.async { implicit request =>
    repository.findProperty(key).map { mayBeProperty =>
      if (mayBeProperty.nonEmpty) Ok(Json.toJson(mayBeProperty.get)) else NotFound
    }
  }

  def deleteProperty(key: String) = Action.async { implicit request =>
    repository.deleteProperty(key).map { deleted =>
      if (deleted) NoContent else NotFound
    }
  }
}
