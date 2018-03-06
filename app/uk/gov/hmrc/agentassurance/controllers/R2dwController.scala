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
import uk.gov.hmrc.agentassurance.model.{ErrorBody, Property, Value}
import uk.gov.hmrc.agentassurance.repositories.R2dwRepository
import uk.gov.hmrc.domain.TaxIdentifier

import scala.concurrent.Future

@Singleton
class R2dwController @Inject()(repository: R2dwRepository) extends PropertiesController(repository) {

  def createProperty = Action.async(parse.json) { implicit request =>
    withJsonBody[Property] { property =>
      baseCreateProperty(property)
    }
  }

  def updateProperty(key: String) = Action.async(parse.json) { implicit request =>
    withJsonBody[Value] { value =>
      baseUpdateProperty(key, value)
    }
  }

  def propertyExists(key: String, identifier: String) = Action.async { implicit request =>
    basePropertyExists(key, identifier)
  }

  def deleteEntireProperty(key: String) = Action.async { implicit request =>
    baseDeleteEntireProperty(key)
  }

  def deleteIdentifierInProperty(key: String, identifier: String) = Action.async { implicit request =>
    baseDeleteIdentifierInProperty(key, identifier)
  }
}
