/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.agentassurance.models.pagination

import java.net.URLEncoder

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.agentassurance.binders.PaginationParameters

case class PaginationLinks(
                            self: LinkHref,
                            first: LinkHref,
                            previous: Option[LinkHref],
                            next: Option[LinkHref],
                            last: LinkHref)

object PaginationLinks {
  implicit val format: Format[PaginationLinks] = Json.format[PaginationLinks]

  def apply(
             paginationParams: PaginationParameters,
             total: Int,
             paginatedLinkBuilder: PaginationParameters => String): PaginationLinks =
    PaginationLinks(
      selfLink(paginationParams, paginatedLinkBuilder),
      firstLink(paginationParams, paginatedLinkBuilder),
      previousLink(paginationParams, total, paginatedLinkBuilder),
      nextLink(paginationParams, total, paginatedLinkBuilder),
      lastLink(paginationParams, total, paginatedLinkBuilder)
    )

  private def selfLink(paginationParams: PaginationParameters, f: PaginationParameters => String): LinkHref =
    LinkHref(f(paginationParams))

  private def firstLink(paginationParams: PaginationParameters, f: PaginationParameters => String): LinkHref =
    LinkHref(f(paginationParams.copy(page = 1)))

  private def previousLink(
                            paginationParams: PaginationParameters,
                            total: Int,
                            f: PaginationParameters => String): Option[LinkHref] =
    if (paginationParams.page > 1) {
      Some(LinkHref(f(paginationParams.copy(page = paginationParams.page - 1))))
        .filter(_ => Range.inclusive(2, paginationParams.lastPage(total)).contains(paginationParams.page))
    } else None

  private def nextLink(
                        paginationParams: PaginationParameters,
                        total: Int,
                        f: PaginationParameters => String): Option[LinkHref] =
    Some(LinkHref(f(paginationParams.copy(page = paginationParams.page + 1))))
      .filter(_ => Range(1, paginationParams.lastPage(total)).contains(paginationParams.page))

  private def lastLink(
                        paginationParams: PaginationParameters,
                        total: Int,
                        f: PaginationParameters => String): LinkHref =
    LinkHref(f(paginationParams.copy(page = Math.max(1, ((total - 1) / paginationParams.pageSize) + 1))))

  def makeQueryParamsString(queryParams: Seq[(String, String)]): String = {
    val paramPairs = queryParams.map(Function.tupled((k, v) => s"$k=${URLEncoder.encode(v, "utf-8")}"))
    val params = paramPairs.mkString("&")

    if (params.isEmpty) "" else s"?$params"
  }

}
