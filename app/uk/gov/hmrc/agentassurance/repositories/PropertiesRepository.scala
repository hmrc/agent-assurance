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

import play.api.libs.json.Format
import play.api.libs.json.Json.format
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.mongo.{AtomicUpdate, ReactiveRepository}
import uk.gov.hmrc.agentassurance.model._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertiesRepository @Inject()(mongoComponent: ReactiveMongoComponent)
  extends ReactiveRepository[Property, String]("agent-assurance", mongoComponent.mongoConnector.db, format[Property],
    implicitly[Format[String]]) with AtomicUpdate[Property] {

  import reactivemongo.bson._

  def findProperty(key: String)(implicit ec: ExecutionContext): Future[Option[Property]] =
    find("key" -> key).map(_.headOption)

  def updateProperty(newProperty: Property)(implicit ec: ExecutionContext): Future[Boolean] = {
    findProperty(newProperty.key).flatMap { currentPropertyOption =>
      if (currentPropertyOption.isDefined) {
        atomicUpdate(
          BSONDocument("key" -> newProperty.key),
          BSONDocument("$set" -> BSONDocument("value" -> newProperty.value))).map(_ => true)
      } else Future successful (false)
    }
  }

  def createProperty(property: Property)(implicit ec: ExecutionContext): Future[Unit] = insert(property).map(_ => ())

  def deleteProperty(key: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    find("key" -> key).flatMap { p =>
      if (p.headOption.isDefined) {
        remove("key" -> key).map(_ => true)
      } else Future successful (false)
    }
  }

  //false as we always want to update using the atomicUpdate function
  override def isInsertion(newRecordId: BSONObjectID, oldRecord: Property): Boolean = false

}
