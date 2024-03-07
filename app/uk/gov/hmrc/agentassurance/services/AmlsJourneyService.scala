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

package uk.gov.hmrc.agentassurance.services

import com.google.inject.ImplementedBy
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.agentassurance.models.AmlsJourney
import uk.gov.hmrc.agentassurance.repositories.AmlsJourneyRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AmlsJourneyServiceImpl])
  trait AmlsJourneyService {
    def get(dataKey: DataKey[AmlsJourney])(implicit reads: Reads[AmlsJourney], hc: HeaderCarrier): Future[Option[AmlsJourney]]
    def put(dataKey: DataKey[AmlsJourney], value: AmlsJourney)(implicit writes: Writes[AmlsJourney], hc: HeaderCarrier, ec: ExecutionContext): Future[CacheItem]
    def delete(dataKey: DataKey[AmlsJourney])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit]

  }


  @Singleton
  class AmlsJourneyServiceImpl @Inject()(amlsJourneyRepository: AmlsJourneyRepository) extends AmlsJourneyService {

    def sessionId(implicit hc: HeaderCarrier): String = hc.sessionId.getOrElse(throw new Exception("No sessionId")).value
    def get(dataKey: DataKey[AmlsJourney])
              (implicit reads: Reads[AmlsJourney], hc: HeaderCarrier): Future[Option[AmlsJourney]] = {
      amlsJourneyRepository.get(sessionId)(dataKey)
    }

    def put(dataKey: DataKey[AmlsJourney], amlsJourney: AmlsJourney)
              (implicit writes: Writes[AmlsJourney], hc: HeaderCarrier, ec: ExecutionContext): Future[CacheItem] = {
      amlsJourneyRepository.put(sessionId)(dataKey, amlsJourney)
    }

    def delete(dataKey: DataKey[AmlsJourney])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
      amlsJourneyRepository.delete(sessionId)(dataKey).map(_ => ())


}
