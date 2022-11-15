/*
 * Copyright 2022 HM Revenue & Customs
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

import com.google.inject.ImplementedBy
import org.mongodb.scala.MongoException
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.Logging
import uk.gov.hmrc.agentassurance.models.AmlsError._
import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[OverseasAmlsRepositoryImpl])
trait OverseasAmlsRepository {
  def create(amlsEntity: OverseasAmlsEntity): Future[Either[AmlsError, Unit]]
}

@Singleton
class OverseasAmlsRepositoryImpl @Inject()(mongo: MongoComponent)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[OverseasAmlsEntity](
    mongoComponent = mongo,
    collectionName ="overseas-agent-amls",
    domainFormat = OverseasAmlsEntity.format,
    indexes = Seq(
      IndexModel(ascending("arn"), IndexOptions().name("arnIndex").unique(true))
    )
    ) with OverseasAmlsRepository with Logging {

  def create(amlsEntity: OverseasAmlsEntity): Future[Either[AmlsError, Unit]] = {
    collection
      .find(equal("arn", amlsEntity.arn.value))
      .headOption()
      .flatMap{
        case Some(_) => Future successful Left(AmlsRecordExists)
        case _  =>
          collection
          .insertOne(amlsEntity)
            .toFuture()
            .map{
              case insertOneResult if insertOneResult.wasAcknowledged() =>  Right(())
              case e =>
                logger.warn(s"Error inserting overseas AMLS record ${e}")
                Left(AmlsUnexpectedMongoError)
            }
          }.recover {
            case e: MongoException =>
              logger.warn(s"Mongo exception when inserting overseas AMLS record $e")
              Left(AmlsUnexpectedMongoError)
          }
      }
}