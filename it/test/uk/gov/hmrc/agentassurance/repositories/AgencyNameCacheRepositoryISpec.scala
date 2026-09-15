/*
 * Copyright 2026 HM Revenue & Customs
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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.mongodb.scala.ObservableFuture
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.await
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.Application
import play.api.Configuration
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.SymmetricCryptoFactory.aesGcmCrypto
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport
import uk.gov.hmrc.mongo.CurrentTimestampSupport

class AgencyNameCacheRepositoryISpec
extends AnyWordSpecLike
with Matchers
with GuiceOneServerPerSuite
with CleanMongoCollectionSupport
with Eventually {

  override implicit lazy val app: Application = appBuilder.build()
  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(
    "agent.name.cache.expires" -> "5 minutes"
  )

  private val config: Configuration = app.injector.instanceOf[Configuration]
  private implicit val crypto: Encrypter & Decrypter = aesGcmCrypto("0xbYzrPV9/GmVEGazywGswm7yRYoWy2BraeJnjOUgcY=")

  private val agencyNameCacheRepository: AgencyNameCacheRepository =
    new AgencyNameCacheRepository(
      config = config,
      mongo = mongoComponent,
      timestampSupport = new CurrentTimestampSupport()
    )

  "AgencyNameCacheRepository" when {
    "key does not exist in cache" should {
      "call the body and cache an encrypted result against the plain key" in {
        val result = await(agencyNameCacheRepository("1234567890")(Future.successful(Some("Agent Assurance Agency"))))

        result shouldBe Some("Agent Assurance Agency")

        eventually {
          await(agencyNameCacheRepository.cacheRepo.collection.find().toFuture()).size shouldBe 1
        }

        await(agencyNameCacheRepository.getFromCache("1234567890")) shouldBe Some(Some("Agent Assurance Agency"))

        val cacheItem: CacheItem = await(agencyNameCacheRepository.cacheRepo.collection.find().toFuture()).head
        cacheItem.id shouldBe "1234567890"
        cacheItem.data.toString() should not include "Agent Assurance Agency"
      }

      "not cache an empty result" in {
        val result = await(agencyNameCacheRepository("1234567891")(Future.successful(None)))

        result shouldBe None
        await(agencyNameCacheRepository.getFromCache("1234567891")) shouldBe None
        await(agencyNameCacheRepository.cacheRepo.collection.find().toFuture()) shouldBe empty
      }
    }

    "key exists in cache" should {
      "return the decrypted cached data without calling the body" in {
        await(agencyNameCacheRepository.putCache(cacheId = "1234567892")(Some("Cached Agency")))

        val result = await(
          agencyNameCacheRepository("1234567892")(Future.failed(throw new RuntimeException("Should not be called")))
        )

        result shouldBe Some("Cached Agency")

        val cacheItem: CacheItem = await(agencyNameCacheRepository.cacheRepo.collection.find().toFuture()).head
        cacheItem.id shouldBe "1234567892"
        cacheItem.data.toString() should not include "Cached Agency"
      }
    }
  }

}
