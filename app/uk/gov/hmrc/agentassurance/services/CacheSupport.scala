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

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.Configuration
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentassurance.repositories.AgencyDetailsCacheRepository
import uk.gov.hmrc.agentassurance.repositories.AgencyNameCacheRepository

trait Cache[T] {
  def apply(key: String)(
    body: => Future[T]
  )(implicit ec: ExecutionContext): Future[T]
}

class DoNotCache[T]
extends Cache[T] {
  def apply(key: String)(
    body: => Future[T]
  )(implicit ec: ExecutionContext): Future[T] = body
}

@Singleton
class CacheProvider @Inject() (
  agencyDetailsCache: AgencyDetailsCacheRepository,
  agencyNameCache: AgencyNameCacheRepository,
  configuration: Configuration
) {

  val cacheEnabled = configuration.underlying.getBoolean("agent.cache.enabled")
  val cacheNameEnabled = configuration.underlying.getBoolean("agent.name.cache.enabled")

  val agentDetailsCache: Cache[AgentDetailsDesResponse] =
    if (cacheEnabled)
      agencyDetailsCache
    else
      new DoNotCache[AgentDetailsDesResponse]

  val agentNameCache: Cache[Option[String]] =
    if (cacheNameEnabled)
      agencyNameCache
    else
      new DoNotCache[Option[String]]

}
