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

import java.time.LocalDate

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json._
import play.api.libs.json.{JsString, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json._
import uk.gov.hmrc.agentassurance.model._
import uk.gov.hmrc.agentassurance.models.AmlsEntity
import uk.gov.hmrc.agentassurance.repositories.AmlsDBError.{CreateAmlsWithARNNotAllowed, AmlsUnexpectedMongoError, DuplicateAmlsError, NoExistingAmlsError}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

sealed trait AmlsDBError

object AmlsDBError {

  case object DuplicateAmlsError extends AmlsDBError

  case object NoExistingAmlsError extends AmlsDBError

  case object CreateAmlsWithARNNotAllowed extends AmlsDBError

  case object AmlsUnexpectedMongoError extends AmlsDBError

}

@ImplementedBy(classOf[AmlsRepositoryImpl])
trait AmlsRepository {

  def createOrUpdate(amlsEntity: AmlsEntity)(implicit ec: ExecutionContext): Future[Either[AmlsDBError, Unit]]

  def updateArn(utr: Utr, arn: Arn)(implicit ec: ExecutionContext): Future[Either[AmlsDBError, Unit]]
}

@Singleton
class AmlsRepositoryImpl @Inject()(mongoComponent: ReactiveMongoComponent)
  extends ReactiveRepository[AmlsEntity, BSONObjectID](
    "agent-amls",
    mongoComponent.mongoConnector.db,
    AmlsEntity.amlsEntityFormat,
    ReactiveMongoFormats.objectIdFormats) with AmlsRepository {

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq("utr" -> IndexType.Ascending),
      name = Some("utrIndex"),
      unique = true))


  def createOrUpdate(amlsEntity: AmlsEntity)(implicit ec: ExecutionContext): Future[Either[AmlsDBError, Unit]] = {
    val utr = amlsEntity.amlsDetails.utr.value
    val selector = "amlsDetails.utr" -> toJsFieldJsValueWrapper(utr)
    find(selector).map(_.headOption).flatMap {
      case Some(existingEntity) if existingEntity.amlsDetails.arn.isDefined => toFuture(Left(DuplicateAmlsError))
      case _ =>
        amlsEntity.amlsDetails.arn match {
          case Some(_) =>
            Left(CreateAmlsWithARNNotAllowed)
          case None =>
            val selector = Json.obj("amlsDetails.utr" -> JsString(utr))
            collection
              .update(selector, amlsEntity, upsert = true)
              .map { updateResult =>
                if (updateResult.writeErrors.isEmpty) {
                  Right(())
                } else {
                  Left(AmlsUnexpectedMongoError)
                }
              }
        }
    }
  }

  def updateArn(utr: Utr, arn: Arn)(implicit ec: ExecutionContext): Future[Either[AmlsDBError, Unit]] = {
    val selector = "amlsDetails.utr" -> toJsFieldJsValueWrapper(utr.value)
    find(selector).map(_.headOption).flatMap {
      case Some(existingEntity) =>
        existingEntity.amlsDetails.arn match {
          case Some(_) => toFuture(Left(DuplicateAmlsError))
          case None =>
            val selector = Json.obj("amlsDetails.utr" -> JsString(utr.value))
            val toUpdate = existingEntity
              .copy(amlsDetails = existingEntity.amlsDetails.copy(arn = Some(arn)))
              .copy(updatedArnOn = Some(LocalDate.now()))

            collection
              .update(selector, toUpdate)
              .map { updateResult =>
                if (updateResult.writeErrors.isEmpty) {
                  Right(())
                } else {
                  Left(AmlsUnexpectedMongoError)
                }
              }
        }

      case None => Left(NoExistingAmlsError)
    }
  }
}