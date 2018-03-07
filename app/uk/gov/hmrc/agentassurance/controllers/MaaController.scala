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

import play.api.mvc.{Action, Request}
import uk.gov.hmrc.agentassurance.model.{Property, Value}
import uk.gov.hmrc.agentassurance.repositories.MaaRepository
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class MaaController @Inject()(repository: MaaRepository) extends PropertiesController(repository) {

  override def key = "manually-assured"

  def createProperty = Action.async(parse.json) { implicit request =>
    withJsonBody[Value] { value =>
      baseCreateProperty(value)
    }
  }

  def updateProperty= Action.async(parse.json) { implicit request =>
    withJsonBody[Value] { value =>
      baseUpdateProperty(key, value)
    }
  }

  def isManuallyAssured(identifier: String) = Action.async { implicit request =>
    repository.findProperty(key).map { mayBeProperty =>
      if (mayBeProperty.isDefined && mayBeProperty.get.value.contains(identifier)) Ok else Forbidden
    }
  }

  def deleteEntireProperty = Action.async { implicit request =>
    baseDeleteEntireProperty(key)
  }

  def deleteIdentifierInProperty(identifier: String) = Action.async { implicit request =>
    baseDeleteIdentifierInProperty(key, identifier)
  }
}
