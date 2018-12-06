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

package uk.gov.hmrc.agentassurance.repositories

import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.libs.json.Json.format
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import uk.gov.hmrc.mongo.{AtomicUpdate, ReactiveRepository}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.agentassurance.models.Property
import uk.gov.hmrc.agentassurance.models.pagination.PaginationResult

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertiesRepository @Inject() (mongoComponent: ReactiveMongoComponent)
  extends ReactiveRepository[Property, String]("agent-assurance", mongoComponent.mongoConnector.db, format[Property],
    implicitly[Format[String]]) with AtomicUpdate[Property] {

  import reactivemongo.bson._
  import collection.BatchCommands.AggregationFramework.{Match, Project, Group, SumValue, PushField}

  def findProperties(key: String, page:Int, pageSize: Int)(implicit ec: ExecutionContext): Future[(Int, Seq[String])] = {

    val skipDuePageNumber = pageSize * (page - 1)
    val sliceForPagination = Json.obj("$slice" -> JsArray(Seq(JsString("$utrs"), JsNumber(skipDuePageNumber), JsNumber(pageSize))))

    val groupUtrsByKey = Group(JsString("$key"))("totalUtrsForKey" -> SumValue(1), "utrs" -> PushField("value"))
    val project = Project(Json.obj("collectionTotalForKey" -> "$totalUtrsForKey",
      "utrsForPage" -> sliceForPagination))

    collection.aggregate(Match(Json.obj("key" -> key)), List(groupUtrsByKey, project)).map(
      response => response.firstBatch.headOption match {
        case Some(jsonResponse) => {
          val paginatedResult = jsonResponse.asOpt[PaginationResult].getOrElse(throw new Exception("bad json"))
          (paginatedResult.collectionTotalForKey, paginatedResult.utrsForPage)
        }
        case None => (0, Seq.empty)
      }
    )
  }

  def propertyExists(property: Property)(implicit ec: ExecutionContext): Future[Boolean] =
    find("key" -> property.key, "value" -> property.value).map(_.headOption.nonEmpty)

  def createProperty(property: Property)(implicit ec: ExecutionContext): Future[Unit] = insert(property).map(_ => ())

  def deleteProperty(property: Property)(implicit ec: ExecutionContext): Future[Unit] =
    remove("key" -> property.key, "value" -> property.value).map(_ => ())

  //false as we always want to update using the atomicUpdate function
  override def isInsertion(newRecordId: BSONObjectID, oldRecord: Property): Boolean = false

  override def indexes: Seq[Index] = Seq(Index(Seq("key" -> IndexType.Ascending)))
}