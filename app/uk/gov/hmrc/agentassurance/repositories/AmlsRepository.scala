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

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json._
import play.api.libs.json.{JsString, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json._
import uk.gov.hmrc.agentassurance.model
import uk.gov.hmrc.agentassurance.models.AmlsDetails
import uk.gov.hmrc.agentassurance.repositories.AmlsDBError.{AmlsUnexpectedMongoError, DuplicateAmlsError}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

sealed trait AmlsDBError

object AmlsDBError {

  case object DuplicateAmlsError extends AmlsDBError

  case object AmlsUnexpectedMongoError extends AmlsDBError

}

@ImplementedBy(classOf[AmlsRepositoryImpl])
trait AmlsRepository {
  def createOrUpdate(amlsDetails: AmlsDetails)(implicit ec: ExecutionContext): Future[Either[AmlsDBError, Unit]]
}

@Singleton
class AmlsRepositoryImpl @Inject()(mongoComponent: ReactiveMongoComponent)
  extends ReactiveRepository[AmlsDetails, BSONObjectID](
    "agent-amls",
    mongoComponent.mongoConnector.db,
    AmlsDetails.amlsDetailsFormat,
    ReactiveMongoFormats.objectIdFormats) with AmlsRepository {

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq("utr" -> IndexType.Ascending),
      name = Some("utrIndex"),
      unique = true))


  def createOrUpdate(newAmlsDetails: AmlsDetails)(implicit ec: ExecutionContext): Future[Either[AmlsDBError, Unit]] = {
    val selector = "utr" -> toJsFieldJsValueWrapper(newAmlsDetails.utr.value)
    find(selector).map(_.headOption).flatMap {
      case Some(existingDetails) if existingDetails.arn.isDefined => model.toFuture(Left(DuplicateAmlsError))
      case _ =>
        val selector = Json.obj("utr" -> JsString(newAmlsDetails.utr.value))
        collection
          .update(selector, newAmlsDetails, upsert = true)
          .map { updateResult =>
            if (updateResult.writeErrors.isEmpty) {
              Right(())
            } else {
              Logger.warn(s"error during creating amls record, error=${updateResult.writeErrors.mkString(",")}")
              Left(AmlsUnexpectedMongoError)
            }
          }
    }
  }
}