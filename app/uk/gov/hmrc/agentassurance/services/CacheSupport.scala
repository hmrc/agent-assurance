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

import scala.concurrent.Future

import com.google.inject.ImplementedBy
import play.api.Configuration
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentassurance.repositories.AgentDetailsCacheRepositoryImplementation

@ImplementedBy(classOf[AgentDetailsCacheRepositoryImplementation])
trait AgentDetailsCacheRepository {
  def apply(cacheId: String)(
      body: => Future[AgentDetailsDesResponse]
  ): Future[AgentDetailsDesResponse]
}

class DoNotCache extends AgentDetailsCacheRepository {
  def apply(cacheId: String)(
      body: => Future[AgentDetailsDesResponse]
  ): Future[AgentDetailsDesResponse] = body
}

@Singleton
class CacheProvider @Inject() (
    agencyDetailsCache: AgentDetailsCacheRepository,
    configuration: Configuration
) {

  val cacheEnabled = configuration.underlying.getBoolean("agent.cache.enabled")

  val agentDetailsCache: AgentDetailsCacheRepository =
    if (cacheEnabled) agencyDetailsCache
    else new DoNotCache

}
