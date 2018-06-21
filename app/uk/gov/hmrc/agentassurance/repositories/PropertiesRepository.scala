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

import play.api.libs.json._
import play.api.libs.json.Json.format
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.ImplicitBSONHandlers
import uk.gov.hmrc.mongo.{AtomicUpdate, ReactiveRepository}
import uk.gov.hmrc.agentassurance.model._
import scala.concurrent.{ExecutionContext, Future}

abstract class PropertiesRepository (mongoComponent: ReactiveMongoComponent, collectionName: String)
  extends ReactiveRepository[Property, String](collectionName, mongoComponent.mongoConnector.db, format[Property],
    implicitly[Format[String]]) with AtomicUpdate[Property] {

  import reactivemongo.bson._
  import ImplicitBSONHandlers._

  def getUtrsForKey(key: String)(implicit ec: ExecutionContext): Future[List[String]] = {
    collection.find(Json.obj("key" -> key)).one[UtrsForKey].map{response => response.map(_.values).getOrElse(List.empty)}
  }

  def findProperty(key: String, utr: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val query2 = Json.obj("key" -> key, "values" -> utr)
    collection.find(query2, Json.obj("$exists" -> true)).one[BSONDocument].map{result =>
      result.isDefined}
  }

  //utr validation on frontend, whatever string you pass will work
  def updateProperty(newProperty: Property)(implicit ec: ExecutionContext): Future[Boolean] = {
    collection.update(Json.obj("key" -> newProperty.key), addToSet(BSONDocument("values" -> newProperty.value)), upsert = true)
      .map{response => response.ok}
  }

  def deleteUtr(key: String, utr: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    collection.update(Json.obj("key" -> key), Json.obj("$pull" -> Json.obj("values"-> utr))).map(_.ok)
  }

  def getUtrsPaginationWithTotalUtrAmount(key: String, recordsPerPage: Int, pageNumber: Int = 1)(implicit ec: ExecutionContext): Future[Option[UtrsForKeyUpdated]] = {
    import collection.BatchCommands.AggregationFramework.{Match, Project}
    val validatedPageNumber = if(pageNumber < 1) 1 else pageNumber
    val validatedRecordsPerPage = if(recordsPerPage < 1) 1 else recordsPerPage
    val skipRecords = validatedRecordsPerPage * validatedPageNumber - validatedRecordsPerPage

    collection.aggregate(Match(Json.obj("key" -> key)),
      List(Project(Json.obj("key" -> "$key",
        "values" -> Json.obj("$slice" -> JsArray(Seq(JsString("$values"),
          JsNumber(skipRecords), JsNumber(validatedRecordsPerPage)))), "totalUtrs" -> Json.obj("$size" -> "$values"))))).map(_.firstBatch.headOption match {
      case Some(results) => results.asOpt[UtrsForKeyUpdated]
      case None => None
    })
  }

  //false as we always want to update using the atomicUpdate function
  override def isInsertion(newRecordId: BSONObjectID, oldRecord: Property): Boolean = false

  override def indexes: Seq[Index] = Seq(Index(Seq("key" -> IndexType.Ascending)))
}