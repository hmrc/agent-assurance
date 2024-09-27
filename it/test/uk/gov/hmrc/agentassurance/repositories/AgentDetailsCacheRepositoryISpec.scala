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
import com.codahale.metrics.MetricRegistry
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.test.Helpers.await
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.Application
import play.api.Configuration
import uk.gov.hmrc.agentassurance.models.AgencyDetails
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentassurance.models.BusinessAddress
import uk.gov.hmrc.agentmtdidentifiers.model.SuspensionDetails
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.crypto.Crypted
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.crypto.SymmetricCryptoFactory.aesCrypto
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

class AgentDetailsCacheRepositoryISpec
    extends AnyWordSpecLike
    with Matchers
    with GuiceOneServerPerSuite
    with CleanMongoCollectionSupport
    with Eventually {

  implicit override lazy val app: Application = appBuilder.build()
  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder().configure(
      "agent.cache.expires" -> "5 minutes",
    )

  private val config: Configuration = app.injector.instanceOf[Configuration]
  private val metrics: Metrics = new Metrics {
    override def defaultRegistry: MetricRegistry = new MetricRegistry
  }

  private implicit val crypto: Encrypter with Decrypter = aesCrypto("0xbYzrPV9/GmVEGazywGswm7yRYoWy2BraeJnjOUgcY=")
  private def encryptKey(key: String): String           = crypto.encrypt(PlainText(key)).value
  private def decryptKey(field: String): String         = crypto.decrypt(Crypted(field)).value

  private val agencyDetailsCacheRepository: AgencyDetailsCacheRepository = new AgencyDetailsCacheRepository(
    config = config,
    mongo = mongoComponent,
    timestampSupport = new CurrentTimestampSupport(),
    metrics = metrics,
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
    regimes = Some(Set("ALL")),
  )

  val agencyDetailsResponse: AgentDetailsDesResponse = AgentDetailsDesResponse(
    uniqueTaxReference = Some(Utr("aa123456789")),
    agencyDetails = Some(agencyDetails),
    suspensionDetails = Some(suspensionDetails),
    isAnIndividual = Some(true)
  )

  private val agentDetailsEncryptedJson: JsValue =
    Json.parse("""
                 |{
                 |  "dataKey": {
                 |    "uniqueTaxReference": "dYxKUcQi7brfw2LV/jTF6Q==",
                 |    "agencyDetails": {
                 |      "agencyName": "Wdx+pMEDlSl5CvlEZi6MpDrLGPNjnx0XLGviZ3VCXKk=",
                 |      "agencyEmail": "HsqaxGDAUITyI08IQPDg0RxkRDlJHhe7tYcQPq70wz8=",
                 |      "agencyTelephone": "vxQ+sLG39lJBSyQcAmLLEg==",
                 |      "agencyAddress": {
                 |        "addressLine1": "dSa/QR2l10hGIhYZBl0nRg==",
                 |        "addressLine2": "vCy7qMC2hQ6E1M+TAkldhkif+6omWtQ7ge93+XqTuUQ=",
                 |        "addressLine3": "vCy7qMC2hQ6E1M+TAkldhkzV5LrzZyjqwSUyFZ6PcPQ=",
                 |        "addressLine4": "vCy7qMC2hQ6E1M+TAkldhhD6UE6fqfY3hml1fQwr/OU=",
                 |        "postalCode": "tnJ/JZ8+U+FQ2CWid13eHw==",
                 |        "countryCode": "h87BRos/A2asGKgzRou3Sg=="
                 |      }
                 |    },
                 |    "suspensionDetails": {
                 |      "suspensionStatus": true,
                 |      "regimes": [
                 |        "ALL"
                 |      ]
                 |    },
                 |    "isAnIndividual": true
                 |  }
                 |}
                 |""".stripMargin)

  private def encryptedCacheId(key: String): String = encryptKey(key)

  "AgencyDetailsCacheRepository" when {
    "key does not exist in cache" should {
      "call the body and cache the result" in {
        val result = await(agencyDetailsCacheRepository("agent-2")(Future.successful(agencyDetailsResponse)))

        result shouldBe agencyDetailsResponse
        
        eventually {
          await(agencyDetailsCacheRepository.cacheRepo.collection.find().toFuture()).size shouldBe 1
        }

        val cachedResult = await(agencyDetailsCacheRepository.getFromCache(encryptedCacheId("agent-2")))
        cachedResult shouldBe Some(agencyDetailsResponse)

        val cacheItem: CacheItem = await(agencyDetailsCacheRepository.cacheRepo.collection.find().toFuture()).head
        cacheItem.data shouldBe agentDetailsEncryptedJson
        cacheItem.id shouldBe encryptedCacheId("agent-2")
        decryptKey(cacheItem.id) shouldBe "agent-2"
      }
    }

    "key exists in cache" should {
      "return the cached data" in {
        await(agencyDetailsCacheRepository.putCache(cacheId = encryptedCacheId("agent-1"))(agencyDetailsResponse))

        val result: AgentDetailsDesResponse = await(
          agencyDetailsCacheRepository("agent-1")(Future.failed(throw new RuntimeException("Should not be called")))
        )

        result shouldBe agencyDetailsResponse

        val cacheItem: CacheItem = await(agencyDetailsCacheRepository.cacheRepo.collection.find().toFuture()).head
        cacheItem.data shouldBe agentDetailsEncryptedJson
        cacheItem.id shouldBe encryptedCacheId("agent-1")
      }
    }
  }
}
