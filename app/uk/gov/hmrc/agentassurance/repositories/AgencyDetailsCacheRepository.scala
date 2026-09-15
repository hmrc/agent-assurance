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

import play.api.Configuration
import play.api.libs.json.Format
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentassurance.services.Cache
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.mongo.cache.CacheIdType
import uk.gov.hmrc.mongo.cache.EntityCache
import uk.gov.hmrc.mongo.cache.MongoCacheRepository

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Success

@Singleton
class AgencyDetailsCacheRepository @Inject() (
  config: Configuration,
  mongo: MongoComponent,
  timestampSupport: TimestampSupport
)(
  implicit
  ec: ExecutionContext,
  @Named("aes") crypto: Encrypter
    & Decrypter
)
extends EntityCache[String, AgentDetailsDesResponse]
with Cache[AgentDetailsDesResponse]:

  val format: Format[AgentDetailsDesResponse] = AgentDetailsDesResponse.agentRecordDatabaseDetailsFormat
  val cacheRepo: MongoCacheRepository[String] =
    new MongoCacheRepository(
      mongoComponent = mongo,
      collectionName = "cache-agent-details",
      ttl = Duration.create(config.underlying.getString("agent.cache.expires")),
      timestampSupport = timestampSupport,
      cacheIdType = CacheIdType.SimpleCacheId,
      replaceIndexes = true
    )

  def apply(
    key: String
  )(body: => Future[AgentDetailsDesResponse])(implicit ec: ExecutionContext): Future[AgentDetailsDesResponse] =
    getFromCache(key).flatMap:
      case Some(v) => Future.successful(v)
      case None =>
        body.andThen:
          case Success(v) => putCache(key)(v).map(_ => v)
  end apply

end AgencyDetailsCacheRepository
