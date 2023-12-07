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

package uk.gov.hmrc.agentassurance.repositories

import org.mongodb.scala._
import org.mongodb.scala.model.Aggregates.{filter, limit, skip}
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.Indexes.ascending
import uk.gov.hmrc.agentassurance.models.Property
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class PropertiesRepository @Inject() (mongo: MongoComponent)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[Property](
    mongoComponent = mongo,
    collectionName = "agent-assurance",
    domainFormat = Property.propertyFormat,
    indexes = Seq(
      IndexModel(ascending("key"))
    )
    ) {

  override lazy val requiresTtlIndex: Boolean = false

  def findProperties(key: String, page:Int, pageSize: Int): Future[(Int, Seq[String])] = {

    val skipDuePageNumber = pageSize * (page - 1)

    val collectionSize = collection.find(equal("key", key)).toFuture().map(_.size)
    // TODO improve this query e.g. by using a facet to get total before a projection, skip & limit.
    val utrsForPage = collection.aggregate(Seq(
      filter(equal("key", key)),
      skip(skipDuePageNumber),
      limit(pageSize)
    )).toFuture()
      .map(_.map(_.value))

    for {
      size <- collectionSize
      utrs <- utrsForPage
    } yield (size, utrs)
  }

  def propertyExists(property: Property): Future[Boolean] = {
    collection.find(and(equal("key", property.key), equal("value", property.value)))
      .headOption().map(_.isDefined)
  }

  def createProperty(property: Property): Future[Unit] = {
    collection.insertOne(property).toFuture().map(_ => ())
  }

  def deleteProperty(property: Property): Future[Unit] = {
    collection.deleteOne(and(equal("key", property.key), equal("value", property.value)))
      .toFuture().map(_ => ())
  }
}