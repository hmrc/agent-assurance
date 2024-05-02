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

import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.ImplementedBy
import com.mongodb.client.model.ReturnDocument
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.FindOneAndReplaceOptions
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.MongoException
import play.api.Logging
import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.agentassurance.models.AmlsError._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.MongoComponent

@ImplementedBy(classOf[OverseasAmlsRepositoryImpl])
trait OverseasAmlsRepository {
  def create(amlsEntity: OverseasAmlsEntity): Future[Either[AmlsError, Unit]]

  def getOverseasAmlsDetailsByArn(arn: Arn): Future[Option[OverseasAmlsDetails]]

  def createOrUpdate(amlsEntity: OverseasAmlsEntity): Future[Option[OverseasAmlsEntity]]

}

@Singleton
class OverseasAmlsRepositoryImpl @Inject() (mongo: MongoComponent)(implicit ec: ExecutionContext, clock: Clock)
    extends PlayMongoRepository[OverseasAmlsEntity](
      mongoComponent = mongo,
      collectionName = "overseas-agent-amls",
      domainFormat = OverseasAmlsEntity.format,
      indexes = Seq(
        IndexModel(ascending("arn"), IndexOptions().name("arnIndex").unique(true))
      )
    )
    with OverseasAmlsRepository
    with Logging {

  override lazy val requiresTtlIndex: Boolean = false

  def create(amlsEntity: OverseasAmlsEntity): Future[Either[AmlsError, Unit]] = {
    collection
      .find(equal("arn", amlsEntity.arn.value))
      .headOption()
      .flatMap {
        case Some(_) => Future.successful(Left(AmlsRecordExists))
        case _ =>
          collection
            .insertOne(amlsEntity.withDefaultCreatedDate)
            .toFuture()
            .map {
              case insertOneResult if insertOneResult.wasAcknowledged() => Right(())
              case e =>
                logger.warn(s"Error inserting overseas AMLS record ${e}")
                Left(AmlsUnexpectedMongoError)
            }
      }
      .recover {
        case e: MongoException =>
          logger.warn(s"Mongo exception when inserting overseas AMLS record $e")
          Left(AmlsUnexpectedMongoError)
      }
  }

  override def getOverseasAmlsDetailsByArn(arn: Arn): Future[Option[OverseasAmlsDetails]] =
    collection
      .find(equal("arn", arn.value))
      .headOption()
      .map(_.map(_.amlsDetails))

  override def createOrUpdate(amlsEntity: OverseasAmlsEntity): Future[Option[OverseasAmlsEntity]] = {
    collection
      .findOneAndReplace(
        equal("arn", amlsEntity.arn.value),
        amlsEntity.withDefaultCreatedDate,
        FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.BEFORE)
      )
      .headOption()
  }

}
