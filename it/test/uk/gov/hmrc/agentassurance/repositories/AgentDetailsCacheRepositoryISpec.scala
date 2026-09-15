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
import uk.gov.hmrc.agentassurance.models.AgencyDetails
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentassurance.models.BusinessAddress
import uk.gov.hmrc.agentassurance.models.SuspensionDetails
import uk.gov.hmrc.agentassurance.models.Utr
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.SymmetricCryptoFactory.aesGcmCrypto
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport
import uk.gov.hmrc.mongo.CurrentTimestampSupport

class AgentDetailsCacheRepositoryISpec
extends AnyWordSpecLike
with Matchers
with GuiceOneServerPerSuite
with CleanMongoCollectionSupport
with Eventually {

  override implicit lazy val app: Application = appBuilder.build()
  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(
    "agent.cache.expires" -> "5 minutes"
  )

  private val config: Configuration = app.injector.instanceOf[Configuration]

  private implicit val crypto: Encrypter & Decrypter = aesGcmCrypto("0xbYzrPV9/GmVEGazywGswm7yRYoWy2BraeJnjOUgcY=")

  private val agencyDetailsCacheRepository: AgencyDetailsCacheRepository =
    new AgencyDetailsCacheRepository(
      config = config,
      mongo = mongoComponent,
      timestampSupport = new CurrentTimestampSupport()
    )

  val businessAddress: BusinessAddress = BusinessAddress(
    addressLine1 = "25",
    addressLine2 = Some("Business Address Line 2"),
    addressLine3 = Some("Business Address Line 3"),
    addressLine4 = Some("Business Address Line 4"),
    postalCode = Some("GL54 1AA"),
    countryCode = "GB"
  )

  val agencyDetails: AgencyDetails = AgencyDetails(
    agencyName = Some("Agent Assurance Agency"),
    agencyEmail = Some("agencyassurance@email.com"),
    agencyTelephone = Some("01483821590"),
    agencyAddress = Some(businessAddress)
  )

  val suspensionDetails: SuspensionDetails = SuspensionDetails(
    suspensionStatus = true,
    regimes = Some(Set("ALL"))
  )

  val agencyDetailsResponse: AgentDetailsDesResponse = AgentDetailsDesResponse(
    uniqueTaxReference = Some(Utr("aa123456789")),
    agencyDetails = Some(agencyDetails),
    suspensionDetails = Some(suspensionDetails),
    isAnIndividual = Some(true)
  )

  "AgencyDetailsCacheRepository" when {
    "key does not exist in cache" should {
      "call the body and cache the result" in {
        val result = await(agencyDetailsCacheRepository("agent-2")(Future.successful(agencyDetailsResponse)))

        result shouldBe agencyDetailsResponse

        eventually {
          await(agencyDetailsCacheRepository.cacheRepo.collection.find().toFuture()).size shouldBe 1
        }

        val cachedResult = await(agencyDetailsCacheRepository.getFromCache("agent-2"))
        cachedResult shouldBe Some(agencyDetailsResponse)

        val cacheItem: CacheItem = await(agencyDetailsCacheRepository.cacheRepo.collection.find().toFuture()).head
        cacheItem.id shouldBe "agent-2"
        cacheItem.data.toString() should not include "aa123456789"
        cacheItem.data.toString() should not include "Agent Assurance Agency"
        cacheItem.data.toString() should not include "agencyassurance@email.com"
      }
    }

    "key exists in cache" should {
      "return the cached data" in {
        await(agencyDetailsCacheRepository.putCache(cacheId = "agent-1")(agencyDetailsResponse))

        val result: AgentDetailsDesResponse = await(
          agencyDetailsCacheRepository("agent-1")(Future.failed(throw new RuntimeException("Should not be called")))
        )

        result shouldBe agencyDetailsResponse

        val cacheItem: CacheItem = await(agencyDetailsCacheRepository.cacheRepo.collection.find().toFuture()).head
        cacheItem.id shouldBe "agent-1"
        cacheItem.data.toString() should not include "aa123456789"
      }
    }
  }

}
