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

import com.google.inject.ImplementedBy
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.Logging
import play.api.libs.json.Format
import uk.gov.hmrc.agentassurance.models.AmlsError.AmlsUnexpectedMongoError
import uk.gov.hmrc.agentassurance.models.{AmlsError, ArchivedAmlsEntity}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[ArchivedAmlsRepositoryImpl])
trait ArchivedAmlsRepository {
  def create(archivedAmlsEntity: ArchivedAmlsEntity): Future[Either[AmlsError, Unit]]
}

@Singleton
class ArchivedAmlsRepositoryImpl @Inject()(mongoComponent: MongoComponent)(
  implicit ec: ExecutionContext) extends PlayMongoRepository[ArchivedAmlsEntity](
  mongoComponent = mongoComponent,
  collectionName = "archived-amls",
  domainFormat = ArchivedAmlsEntity.format,
  indexes = Seq(
    IndexModel(ascending("arn"),
      IndexOptions()
        .name("arnIndex"))
  ),
  extraCodecs = Seq(
    Codecs.playFormatCodec(Format(Arn.arnReads, Arn.arnWrites))
  )
) with ArchivedAmlsRepository with Logging {


override def create(archivedAmlsEntity: ArchivedAmlsEntity): Future[Either[AmlsError, Unit]] = {
  collection
    .insertOne(archivedAmlsEntity)
    .toFuture()
    .map(
      result =>
        if(result.wasAcknowledged()) Right(())
          else
            Left(AmlsUnexpectedMongoError)
          )
    }
}
