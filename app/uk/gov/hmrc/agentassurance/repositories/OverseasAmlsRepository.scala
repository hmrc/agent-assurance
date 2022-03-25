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
import play.api.libs.json.Json._
import javax.inject.{Inject, Singleton}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.agentassurance.models.AmlsError._
import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[OverseasAmlsRepositoryImpl])
trait OverseasAmlsRepository {
  def create(amlsEntity: OverseasAmlsEntity)(implicit ec: ExecutionContext): Future[Either[AmlsError, Unit]]
}

@Singleton
class OverseasAmlsRepositoryImpl @Inject()(mongoComponent: ReactiveMongoComponent)
  extends ReactiveRepository[OverseasAmlsEntity, BSONObjectID](
    "overseas-agent-amls",
    mongoComponent.mongoConnector.db,
    OverseasAmlsEntity.format,
    ReactiveMongoFormats.objectIdFormats) with OverseasAmlsRepository {

  override def indexes: Seq[Index] = Seq(
    Index(key = Seq("arn" -> IndexType.Ascending), name = Some("arnIndex"), unique = true))

  def create(amlsEntity: OverseasAmlsEntity)(implicit ec: ExecutionContext): Future[Either[AmlsError, Unit]] = {
    val selector = "arn" -> toJsFieldJsValueWrapper(amlsEntity.arn.value)

    find(selector).map(_.headOption).flatMap {
      case Some(_) => Future.successful(Left(AmlsRecordExists))
      case _ =>  collection.insert(false).one(amlsEntity)
        .map(_ => Right(()))
        .recover { case _ => Left(AmlsUnexpectedMongoError) }
    }
  }
}