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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.mocks.MockAppConfig
import uk.gov.hmrc.agentassurance.models.Utr
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport
import uk.gov.hmrc.mongo.CurrentTimestampSupport

class MongoLockServiceSpec
extends PlaySpec
with CleanMongoCollectionSupport
with MockAppConfig {

  val mongoLockRepository = new MongoLockRepository(mongoComponent, new CurrentTimestampSupport)
  implicit val ac: AppConfig = mockAppConfig

  val service = new MongoLockService(mongoLockRepository)

  val utr1 = Utr("1234567")
  val utr2 = Utr("1234567")

  "MongoLockServiceSpec" should {
    "return Some(value) when not locked" in {
      await(service.dailyLock(utr1)(Future.successful(()))) mustBe Some(())
    }
    "return None when locked" in {
      await(service.dailyLock(utr1)(Future.successful(()))) mustBe Some(())
      await(service.dailyLock(utr1)(Future.successful(()))) mustBe None
    }

    "return Some(value) after TTL 1 second when locked" in {
      await(service.dailyLock(utr1)(Future.successful(()))) mustBe Some(())
      Thread.sleep(500)
      await(service.dailyLock(utr1)(Future.successful(()))) mustBe None
      Thread.sleep(600)
      await(service.dailyLock(utr1)(Future.successful(()))) mustBe Some(())
    }

  }

}
