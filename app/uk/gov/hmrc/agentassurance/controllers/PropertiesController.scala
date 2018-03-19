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


import play.api.libs.json.Json
import play.api.mvc.Request
import uk.gov.hmrc.agentassurance.repositories.PropertiesRepository
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.agentassurance.model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class PropertiesController (repository: PropertiesRepository) extends BaseController {

  def key: String

  protected def baseCreateProperty(value: Value)(implicit request: Request[Any]) = {
    repository.findProperty(key).flatMap { op =>
      if (op.isEmpty) {
        repository.createProperty(value.toProperty(key)).map(_ => Created)
      } else {
        Future successful Conflict(Json.toJson(ErrorBody("PROPERTY_EXISTS", "Property already exists")))
      }
    }
  }

  protected def baseUpdateProperty(key: String, value: Value)(implicit request: Request[Any]) = {
    //fetch property and append new value to avoid overriding
    repository.findProperty(key).flatMap{ maybeProperty =>
      if(maybeProperty.isDefined){
        repository.updateProperty(Value(s"${maybeProperty.get.value},${value.value.replace(" ", "")}").toProperty(key)).map ( updated =>
          if (updated) NoContent else InternalServerError
        )
      }
      else{
        Future successful NotFound
      }
    }
  }

  protected def baseDeleteEntireProperty(key: String)(implicit request: Request[Any]) = {
    repository.deleteProperty(key).map { deleted =>
      if (deleted) NoContent else NotFound
    }
  }

  protected def baseDeleteIdentifierInProperty(key: String, identifier: String)(implicit request: Request[Any]) = {
    repository.findProperty(key).flatMap{ maybeProperty =>
      if(maybeProperty.isDefined){
        val modifiedIdentifiers = maybeProperty.get.value.split(",").filterNot(_.equals(identifier.replace(" ", "")))
        repository.updateProperty(Property(key, modifiedIdentifiers.mkString(","))).map ( updated =>
          if (updated) NoContent else InternalServerError
        )
      }
      else{
        Future successful NotFound
      }
    }
  }
}
