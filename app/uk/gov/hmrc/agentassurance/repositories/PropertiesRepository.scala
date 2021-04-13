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

package uk.gov.hmrc.agentassurance.repositories

import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.Cursor
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.agentassurance.models.Property
import uk.gov.hmrc.agentassurance.models.pagination.PaginationResult
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertiesRepository @Inject() (mongoComponent: ReactiveMongoComponent)
  extends ReactiveRepository[Property, String]("agent-assurance", mongoComponent.mongoConnector.db, Property.propertyFormat,
    implicitly[Format[String]]) {

  import collection.BatchCommands.AggregationFramework.{Group, Match, Project, PushField, SumAll}

  def findProperties(key: String, page:Int, pageSize: Int)(implicit ec: ExecutionContext): Future[(Int, Seq[String])] = {

    val skipDuePageNumber = pageSize * (page - 1)
    val sliceForPagination = Json.obj("$slice" -> JsArray(Seq(JsString("$utrs"), JsNumber(skipDuePageNumber), JsNumber(pageSize))))

    val groupUtrsByKey = Group(JsString("$key"))("totalUtrsForKey" -> SumAll, "utrs" -> PushField("value"))
    val project = Project(Json.obj("collectionTotalForKey" -> "$totalUtrsForKey",
      "utrsForPage" -> sliceForPagination))

    val pipeline = (Match(Json.obj("key" -> key)), List(groupUtrsByKey, project))
    val results: Cursor[PaginationResult] = collection.aggregateWith[PaginationResult]()(_ => pipeline)

    results.headOption.map {
      case Some(paginatedResult) => (paginatedResult.collectionTotalForKey, paginatedResult.utrsForPage)
      case None => (0, Seq.empty)
    }
  }

  def propertyExists(property: Property)(implicit ec: ExecutionContext): Future[Boolean] =
    find("key" -> property.key, "value" -> property.value).map(_.headOption.nonEmpty)

  def createProperty(property: Property)(implicit ec: ExecutionContext): Future[Unit] = insert(property).map(_ => ())

  def deleteProperty(property: Property)(implicit ec: ExecutionContext): Future[Unit] =
    remove("key" -> property.key, "value" -> property.value).map(_ => ())

  override def indexes: Seq[Index] = Seq(Index(Seq("key" -> IndexType.Ascending)))
}