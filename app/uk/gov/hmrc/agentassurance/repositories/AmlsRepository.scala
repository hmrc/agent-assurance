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

import com.google.inject.ImplementedBy
import com.mongodb.client.model.ReturnDocument
import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{FindOneAndReplaceOptions, IndexModel, IndexOptions, ReplaceOptions}
import play.api.Logging
import uk.gov.hmrc.agentassurance.models.AmlsError.{AmlsUnexpectedMongoError, ArnAlreadySetError, NoExistingAmlsError, UniqueKeyViolationError}
import uk.gov.hmrc.agentassurance.models.{AmlsError, AmlsSource, CreateAmlsRequest, UkAmlsDetails, UkAmlsEntity}
import uk.gov.hmrc.agentassurance.util._
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AmlsRepositoryImpl])
trait AmlsRepository {

  def createOrUpdate(createAmlsRequest: CreateAmlsRequest): Future[Either[AmlsError, Unit]]

  def createOrUpdate(arn: Arn, amlsEntity: UkAmlsEntity): Future[Option[UkAmlsEntity]]

  def updateArn(utr: Utr, arn: Arn): Future[Either[AmlsError, UkAmlsDetails]]

  def getAmlDetails(utr: Utr): Future[Option[UkAmlsDetails]]

  def getAmlsDetailsByArn(arn: Arn): Future[Option[UkAmlsDetails]]
}

@Singleton
class AmlsRepositoryImpl @Inject()(mongo: MongoComponent)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[UkAmlsEntity](
    mongoComponent = mongo,
    collectionName = "agent-amls",
    domainFormat = UkAmlsEntity.amlsEntityFormat,
    indexes = Seq(
      IndexModel(ascending("utr"),
        IndexOptions()
          .unique(true)
          .name("utrIndex")
          .sparse(true)),
      IndexModel(ascending("arn"),
        IndexOptions()
          .unique(true)
          .name("arnIndex")
          .sparse(true))
    ),
    extraCodecs = Seq(
      Codecs.playFormatCodec(CreateAmlsRequest.format)
    ),
    replaceIndexes = true //TODO WG - remove that
  ) with AmlsRepository with Logging {

  override lazy val requiresTtlIndex: Boolean = false


  override def createOrUpdate(createAmlsRequest: CreateAmlsRequest): Future[Either[AmlsError, Unit]] = {

    val utr = createAmlsRequest.utr.value

    collection
      .find(equal("utr", utr))
      .headOption()
      .flatMap {
        case Some(amlsEntity) if amlsEntity.arn.isDefined => Left(ArnAlreadySetError)
        case _ =>
          collection
            .replaceOne(equal("utr", utr),
              UkAmlsEntity(
                utr = Option(Utr(utr)),
                amlsDetails = createAmlsRequest.amlsDetails,
                arn = None,
                createdOn = LocalDate.now(),
                amlsSource = AmlsSource.Subscription),
              ReplaceOptions().upsert(true))
            .toFuture()
            .map {
              case updateResult if updateResult.getModifiedCount < 2L => Right(())
              case _ => Left(AmlsUnexpectedMongoError)
            }
      }
  }

  override def createOrUpdate(arn: Arn, amlsEntity: UkAmlsEntity): Future[Option[UkAmlsEntity]] = {
    collection
      .findOneAndReplace(
        equal("arn", arn.value),
        amlsEntity,
        FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.BEFORE))
      .headOption()
  }

  override def updateArn(utr: Utr, arn: Arn): Future[Either[AmlsError, UkAmlsDetails]] = {
    collection
      .find(equal("utr", utr.value))
      .headOption()
      .flatMap {
        case Some(existingEntity) => existingEntity.arn match {
          case Some(existingArn) =>
            if (existingArn.value == arn.value) Right(existingEntity.amlsDetails)
            else Left(ArnAlreadySetError)
          case None =>
            val toUpdate = existingEntity.copy(arn = Some(arn)).copy(updatedArnOn = Some(LocalDate.now()))
            collection
              .replaceOne(equal("utr", utr.value), toUpdate)
              .toFuture()
              .map {
                case updateResult => if (updateResult.getModifiedCount == 1L) Right(toUpdate.amlsDetails)
                else {
                  logger.warn(s"error updating AMLS record with ARN - acknowledged: " +
                    s"${updateResult.wasAcknowledged()}, modified count: ${updateResult.getModifiedCount}")
                  Left(AmlsUnexpectedMongoError)
                }
              }.recover {
                case e: MongoWriteException if e.getError.getCode == 11000 =>
                  Left(UniqueKeyViolationError)
                case e => {
                  logger.warn(s"unexpected error when updating AMLS record with ARN ${e.getMessage}")
                  Left(AmlsUnexpectedMongoError)
                }
              }
        }
        case None => Left(NoExistingAmlsError)
      }
  }

  override def getAmlDetails(utr: Utr): Future[Option[UkAmlsDetails]] =
    collection
      .find(equal("utr", utr.value))
      .headOption()
      .map(_.map(_.amlsDetails))

  override def getAmlsDetailsByArn(arn: Arn): Future[Option[UkAmlsDetails]] =
    collection
      .find(equal("arn", arn.value))
      .headOption()
      .map(_.map(_.amlsDetails))

}
