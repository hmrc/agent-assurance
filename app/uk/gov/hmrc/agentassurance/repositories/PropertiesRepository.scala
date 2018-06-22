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
import play.api.libs.json.{Format, Json}
import play.api.libs.json.Json.format
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.ReadPreference
import reactivemongo.api.indexes.{Index, IndexType}
import uk.gov.hmrc.mongo.{AtomicUpdate, ReactiveRepository}
import uk.gov.hmrc.agentassurance.model._
import reactivemongo.play.json.ImplicitBSONHandlers._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertiesRepository @Inject() (mongoComponent: ReactiveMongoComponent)
  extends ReactiveRepository[Property, String]("agent-assurance", mongoComponent.mongoConnector.db, format[Property],
    implicitly[Format[String]]) with AtomicUpdate[Property] {

  import reactivemongo.bson._

  def findProperties(key: String, page:Int, pageSize: Int)(implicit ec: ExecutionContext): Future[(Int, Seq[Property])] = {

    for {
      total <- collection.count(Some(Json.obj("key" ->  key)))
      properties <- collection.find(Json.obj("key" ->  key))
        .skip(pageSize * (page - 1))
        .cursor[Property](ReadPreference.primaryPreferred)
        .collect[Seq](pageSize)
    } yield (total, properties)
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
