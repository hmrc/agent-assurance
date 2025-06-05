/*
 * Copyright 2024 HM Revenue & Customs
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

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.ImplementedBy
import org.mongodb.scala._
import org.mongodb.scala.model.Aggregates.filter
import org.mongodb.scala.model.Aggregates.limit
import org.mongodb.scala.model.Aggregates.skip
import org.mongodb.scala.model.Filters.and
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.Indexes.ascending
import uk.gov.hmrc.agentassurance.models.Property
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.MongoComponent

@ImplementedBy(classOf[PropertiesRepositoryImpl])
trait PropertiesRepository {

  def findProperties(
    key: String,
    page: Int,
    pageSize: Int
  ): Future[(Int, Seq[String])]

  def propertyExists(property: Property): Future[Boolean]

  def createProperty(property: Property): Future[Unit]

  def deleteProperty(property: Property): Future[Unit]

}

@Singleton
class PropertiesRepositoryImpl @Inject() (mongo: MongoComponent)(implicit ec: ExecutionContext)
extends PlayMongoRepository[Property](
  mongoComponent = mongo,
  collectionName = "agent-assurance",
  domainFormat = Property.propertyFormat,
  indexes = Seq(
    IndexModel(ascending("key"))
  )
)
with PropertiesRepository {

  override lazy val requiresTtlIndex: Boolean = false

  override def findProperties(
    key: String,
    page: Int,
    pageSize: Int
  ): Future[(Int, Seq[String])] = {

    val skipDuePageNumber = pageSize * (page - 1)

    val collectionSize = collection.find(equal("key", key)).toFuture().map(_.size)
    // TODO improve this query e.g. by using a facet to get total before a projection, skip & limit.
    val utrsForPage = collection
      .aggregate(
        Seq(
          filter(equal("key", key)),
          skip(skipDuePageNumber),
          limit(pageSize)
        )
      )
      .toFuture()
      .map(_.map(_.value))

    for {
      size <- collectionSize
      utrs <- utrsForPage
    } yield (size, utrs)
  }

  override def propertyExists(property: Property): Future[Boolean] = {
    collection
      .find(and(equal("key", property.key), equal("value", property.value)))
      .headOption()
      .map(_.isDefined)
  }

  override def createProperty(property: Property): Future[Unit] = {
    collection.insertOne(property).toFuture().map(_ => ())
  }

  override def deleteProperty(property: Property): Future[Unit] = {
    collection
      .deleteOne(and(equal("key", property.key), equal("value", property.value)))
      .toFuture()
      .map(_ => ())
  }

}
