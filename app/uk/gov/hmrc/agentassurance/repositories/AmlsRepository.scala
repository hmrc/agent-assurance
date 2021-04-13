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

import java.time.LocalDate

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json._
import play.api.libs.json.{JsString, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.LastError
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json._
import uk.gov.hmrc.agentassurance.util._
import uk.gov.hmrc.agentassurance.models.{AmlsDetails, AmlsEntity, CreateAmlsRequest, AmlsError}
import uk.gov.hmrc.agentassurance.models.AmlsError.{AmlsUnexpectedMongoError, ArnAlreadySetError, NoExistingAmlsError, UniqueKeyViolationError}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AmlsRepositoryImpl])
trait AmlsRepository {

  def createOrUpdate(createAmlsRequest: CreateAmlsRequest)(implicit ec: ExecutionContext): Future[Either[AmlsError, Unit]]

  def updateArn(utr: Utr, arn: Arn)(implicit ec: ExecutionContext): Future[Either[AmlsError, AmlsDetails]]
}

@Singleton
class AmlsRepositoryImpl @Inject()(mongoComponent: ReactiveMongoComponent)
  extends ReactiveRepository[AmlsEntity, BSONObjectID](
    "agent-amls",
    mongoComponent.mongoConnector.db,
    AmlsEntity.amlsEntityFormat,
    ReactiveMongoFormats.objectIdFormats) with AmlsRepository {

  override def indexes: Seq[Index] = Seq(
    Index(key = Seq("utr" -> IndexType.Ascending), name = Some("utrIndex"), unique = true),
    Index(key = Seq("arn" -> IndexType.Ascending), name = Some("arnIndex"), unique = true, partialFilter = Some(BSONDocument("arn" -> BSONDocument("$exists" -> true)))))

  def createOrUpdate(createAmlsRequest: CreateAmlsRequest)(implicit ec: ExecutionContext): Future[Either[AmlsError, Unit]] = {
    val utr = createAmlsRequest.utr.value
    val selector = "utr" -> toJsFieldJsValueWrapper(utr)
    find(selector).map(_.headOption).flatMap {
      case Some(existingEntity) if existingEntity.arn.isDefined => toFuture(Left(ArnAlreadySetError))
      case _ =>
        val selector = Json.obj("utr" -> JsString(utr))
        collection
          .update(false).one(selector, AmlsEntity(Utr(utr), createAmlsRequest.amlsDetails, None, LocalDate.now()), upsert = true)
          .map { updateResult =>
            if (updateResult.writeErrors.isEmpty) {
              Right(())
            } else {
              Left(AmlsUnexpectedMongoError)
            }
          }
    }
  }

  def updateArn(utr: Utr, arn: Arn)(implicit ec: ExecutionContext): Future[Either[AmlsError, AmlsDetails]] = {
    val selector = "utr" -> toJsFieldJsValueWrapper(utr.value)
    find(selector).map(_.headOption).flatMap {
      case Some(existingEntity) =>
        existingEntity.arn match {
          case Some(existingArn) =>
            if (arn.value == existingArn.value) {
              toFuture(Right(existingEntity.amlsDetails))
            } else {
              toFuture(Left(ArnAlreadySetError))
            }
          case None =>
            val selector = Json.obj("utr" -> JsString(utr.value))
            val toUpdate = existingEntity.copy(arn = Some(arn)).copy(updatedArnOn = Some(LocalDate.now()))
            collection
              .update(false).one(selector, toUpdate)
              .map { updateResult =>
                if (updateResult.writeErrors.isEmpty) {
                  Right(toUpdate.amlsDetails)
                } else {
                  Left(AmlsUnexpectedMongoError)
                }
              }.recover[Either[AmlsError, AmlsDetails]] {
              case e: LastError => {
                e.code match {
                  case Some(11000) => logger.warn(s"ARN should be unique for each UTR")
                    Left(UniqueKeyViolationError)
                  case _ =>
                    Left(AmlsUnexpectedMongoError)
                }
              }
            }
        }

      case None => Left(NoExistingAmlsError)
    }
  }
}