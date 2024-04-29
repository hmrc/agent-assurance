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

package uk.gov.hmrc.agentassurance.services

import play.api.Configuration
import play.api.libs.json.{Reads, Writes}
import play.api.mvc.Request
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesCheckResponse
import uk.gov.hmrc.agentassurance.repositories.AgencyDetailsCacheRepository
import uk.gov.hmrc.mongo.cache.{DataKey, MongoCacheRepository}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

trait Cache[T] {
  def apply(key: String)(body: => Future[T])
               (implicit request: Request[_], ec: ExecutionContext,reads: Reads[T], writes: Writes[T]): Future[T]
}

class DoNotCache[T] extends Cache[T] {
  def apply(key: String)(body: => Future[T])
               (implicit request: Request[_], ec: ExecutionContext,reads: Reads[T], writes: Writes[T]): Future[T] = body
}

trait CacheSupport[T] extends Cache[T] {

  val cacheRepository: MongoCacheRepository[String]
  val metrics: Metrics
  val cacheId:String

  val record = metrics.defaultRegistry

  def apply(key: String)(body: => Future[T])
                    (implicit request: Request[_], ec: ExecutionContext,reads: Reads[T], writes: Writes[T]): Future[T] = {
    val dataKey = DataKey[T](key)
    cacheRepository.get(cacheId)(dataKey).flatMap {
      case Some(v) =>
        record.counter("Count-" + cacheId + "-from-cache")
        Future.successful(v)
      case None =>
        body.andThen {
          case Success(v) =>
            record.counter("Count-" + cacheId + "-from-source")
            cacheRepository.put(cacheId)(dataKey, v).map(_ => v)
        }
    }
  }

}

@Singleton
class AgencyDetailsCache @Inject()(val cacheRepository: AgencyDetailsCacheRepository,
                                   val metrics: Metrics) extends CacheSupport[AgentDetailsDesCheckResponse] {
  override val cacheId: String = "agentDetails"
}


@Singleton
  class CacheProvider @Inject()(agencyDetailsCache: AgencyDetailsCache,
                                configuration: Configuration) {

  val cacheEnabled = configuration.underlying.getBoolean("agent.cache.enabled")

  val agentDetailsCache: Cache[AgentDetailsDesCheckResponse] =
    if (cacheEnabled) agencyDetailsCache
    else new DoNotCache[AgentDetailsDesCheckResponse]


}
