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

package uk.gov.hmrc.agentassurance.binders

import play.api.mvc.QueryStringBindable

import scala.util.Try

case class PaginationParameters(page: Int, pageSize: Int) {
  require(pageSize > 0, "The pageSize should be greater than zero")
  require(page > 0, s"The page should be greater than zero")

  def lastPage(total: Int): Int = 1 + ((total - 1) / pageSize)
}

object PaginationParameters {

  implicit def queryStringBinder(implicit intBinder: QueryStringBindable[Int]) = new QueryStringBindable[PaginationParameters] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, PaginationParameters]] = {
      Try(for {
        page <- intBinder.bind("page", params)
        pageSize <- intBinder.bind("pageSize", params)
      } yield {
        (page, pageSize) match {
          case (Right(page), Right(size)) => Right(PaginationParameters(page, size))
          case _ => Left("Unable to bind Pagination Parameters")
        }
      }).recover {
        case _ => Some(Left("Invalid pagination parameters"))
      }.get
    }
    override def unbind(key: String, parameters: PaginationParameters): String = {
      intBinder.unbind("page", parameters.page) + "&" + intBinder.unbind("pageSize", parameters.pageSize)
    }
  }
}