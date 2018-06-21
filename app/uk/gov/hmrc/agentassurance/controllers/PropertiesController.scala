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

import play.api.mvc.{Request, Result}
import uk.gov.hmrc.agentassurance.repositories.PropertiesRepository
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.agentassurance.model._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class PropertiesController(repository: PropertiesRepository) extends BaseController {

  def key: String

  protected def getCollectionUtrs(key: String)(implicit request: Request[Any]): Future[Result] = {
    repository.getUtrsForKey(key).map(response => if(response.nonEmpty) Ok(response.mkString(",")) else NoContent)
  }

  protected def baseCreateProperty(value: Value)(implicit request: Request[Any]) = {
      repository.updateProperty(value.toProperty(key)).map(response => if (response) Created else Conflict)
  }

  protected def baseUpdateProperty(key: String, value: Value)(implicit request: Request[Any]): Future[Result] = {
      repository.updateProperty(value.toProperty(key)).map(updated =>
        if (updated) NoContent else InternalServerError)

  }

  protected def baseDeleteIdentifierInProperty(key: String, identifier: String)(implicit request: Request[Any]) = {
    repository.deleteUtr(key, identifier).map(if(_) NoContent else InternalServerError)
  }

  protected def baseGetLimitedUtrs(key: String, pageSize: Int, skipUtrSetsAmount: Int)(implicit request: Request[Any]) = {
    repository.getUtrsPaginationWithTotalUtrAmount(key, pageSize, skipUtrSetsAmount).map {
//      case Some(utrList) => if(utrList.nonEmpty) Ok(utrList.mkString(",")) else NoContent
      case Some(utrList) => if(utrList.utrs.nonEmpty) Ok(utrList.utrs.mkString(",") + utrList.totalUtrs) else NoContent
      case None => NoContent
    }
  }
}