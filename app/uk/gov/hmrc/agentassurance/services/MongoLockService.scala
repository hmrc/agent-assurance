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

import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.mongo.lock.TimePeriodLockService

@Singleton
class MongoLockService @Inject() (mongoLockRepository: MongoLockRepository)(implicit appConfig: AppConfig) {
  def tryLock[T](arn: Arn)(body: => Future[T])(implicit ec: ExecutionContext): Future[Option[T]] = {
    val lockService = TimePeriodLockService(
      mongoLockRepository,
      lockId = s"verify-arn-${arn.value}",
      ttl = appConfig.entityChecksLockExpires
    )
    lockService.withRenewedLock(body)
  }
}