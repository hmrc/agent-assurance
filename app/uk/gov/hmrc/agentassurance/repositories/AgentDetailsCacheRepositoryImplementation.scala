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

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Success

import play.api.libs.json._
import play.api.Configuration
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentassurance.services.AgentDetailsCacheRepository
import uk.gov.hmrc.mongo.cache.CacheIdType
import uk.gov.hmrc.mongo.cache.EntityCache
import uk.gov.hmrc.mongo.cache.MongoCacheRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

@Singleton
class AgentDetailsCacheRepositoryImplementation @Inject() (
    mongoComponent: MongoComponent,
    timestampSupport: TimestampSupport,
    configuration: Configuration,
    metrics: Metrics
)(implicit ec: ExecutionContext)
    extends EntityCache[String, AgentDetailsDesResponse]
    with AgentDetailsCacheRepository {

  private val cacheExpires = Duration.create(configuration.underlying.getString("agent.cache.expires"))

  override val format: Format[AgentDetailsDesResponse] =
    OFormat(AgentDetailsDesResponse.agentRecordDetailsRead, AgentDetailsDesResponse.agentRecordDetailsWrites)

  override val cacheRepo: MongoCacheRepository[String] = new MongoCacheRepository(
    mongoComponent = mongoComponent,
    collectionName = "agency-details",
    ttl = cacheExpires,
    timestampSupport = timestampSupport,
    cacheIdType = CacheIdType.SimpleCacheId
  )

  private val record = metrics.defaultRegistry

  def apply(cacheId: String)(
      body: => Future[AgentDetailsDesResponse]
  ): Future[AgentDetailsDesResponse] = {

    getFromCache(cacheId).flatMap {
      case Some(v) =>
        record.counter("Count-" + cacheId + "-from-cache")
        Future.successful(v)
      case None =>
        body.andThen {
          case Success(v) =>
            record.counter("Count-" + cacheId + "-from-source")
            putCache(cacheId)(v).map(_ => v)
        }
    }
  }
}
