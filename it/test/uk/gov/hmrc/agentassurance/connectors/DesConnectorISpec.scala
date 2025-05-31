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

package uk.gov.hmrc.agentassurance.connectors

import scala.concurrent.ExecutionContext

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.Application
import play.api.Configuration
import test.uk.gov.hmrc.agentassurance.stubs.DataStreamStub
import test.uk.gov.hmrc.agentassurance.stubs.DesStubs
import test.uk.gov.hmrc.agentassurance.support.MetricTestSupport
import test.uk.gov.hmrc.agentassurance.support.UnitSpec
import test.uk.gov.hmrc.agentassurance.support.WireMockSupport
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.models.AgencyDetails
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentassurance.models.BusinessAddress
import uk.gov.hmrc.agentassurance.repositories.AgencyDetailsCacheRepository
import uk.gov.hmrc.agentassurance.repositories.AgencyNameCacheRepository
import uk.gov.hmrc.agentassurance.services.CacheProvider
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.SuspensionDetails
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.crypto.SymmetricCryptoFactory.aesCrypto
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

class DesConnectorISpec
    extends UnitSpec
    with GuiceOneAppPerSuite
    with WireMockSupport
    with DesStubs
    with DataStreamStub
    with CleanMongoCollectionSupport
    with MetricTestSupport {

  private implicit val hc: HeaderCarrier                        = HeaderCarrier()
  private implicit val ec: ExecutionContext                     = ExecutionContext.global
  private implicit val request: Request[AnyContentAsEmpty.type] = FakeRequest()
  private implicit val appConfig: AppConfig                     = app.injector.instanceOf[AppConfig]
  private implicit val config: Config                           = app.injector.instanceOf[Config]
  private implicit lazy val as: ActorSystem                     = ActorSystem()
  private implicit val crypto: Encrypter with Decrypter         = aesCrypto("0xbYzrPV9/GmVEGazywGswm7yRYoWy2BraeJnjOUgcY=")

  private def encryptKey(key: String): String = crypto.encrypt(PlainText(key)).value

  private val agentDataCache =
    new AgencyDetailsCacheRepository(
      app.injector.instanceOf[Configuration],
      mongoComponent,
      new CurrentTimestampSupport,
      app.injector.instanceOf[Metrics]
    )

  private val agentNameCache =
    new AgencyNameCacheRepository(
      app.injector.instanceOf[Configuration],
      mongoComponent,
      new CurrentTimestampSupport,
      app.injector.instanceOf[Metrics]
    )

  implicit override lazy val app: Application = appBuilder
    .build()

  val cacheProvider = new CacheProvider(agentDataCache, agentNameCache, app.injector.instanceOf[Configuration])

  val desConnector = new DesConnectorImpl(
    app.injector.instanceOf[HttpClientV2],
    app.injector.instanceOf[Metrics],
    cacheProvider,
    config,
    as
  )

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.host"                  -> wireMockHost,
        "microservice.services.auth.port"                  -> wireMockPort,
        "microservice.services.des.host"                   -> wireMockHost,
        "microservice.services.des.port"                   -> wireMockPort,
        "microservice.services.des.environment"            -> "test",
        "microservice.services.des.authorization-token"    -> "secret",
        "microservice.services.enrolment-store-proxy.host" -> wireMockHost,
        "microservice.services.enrolment-store-proxy.port" -> wireMockPort,
        "auditing.consumer.baseUri.host"                   -> wireMockHost,
        "auditing.consumer.baseUri.port"                   -> wireMockPort,
        "internal-auth-token-enabled-on-start"             -> false,
        "http-verbs.retries.intervals"                     -> List("1ms"),
        "agent.cache.enabled"                              -> true,
        "agent.cache.expires"                              -> "1 second",
        "auditing.enabled"                                 -> false,
        "rate-limiter.business-names.max-calls-per-second" -> 10,
        "agent.name.cache.enabled"                         -> true,
        "agent.name.cache.expires"                         -> "1 second"
      )
      .bindings(bind[DesConnector].toInstance(desConnector))

  val agentDetailsDesResponse = AgentDetailsDesResponse(
    Some(Utr("0123456789")),
    Some(
      AgencyDetails(
        Some("ABC Accountants"),
        Some("abc@xyz.com"),
        Some("07345678901"),
        Some(
          BusinessAddress(
            "Matheson House",
            Some("Grange Central"),
            Some("Town Centre"),
            Some("Telford"),
            Some("TF3 4ER"),
            "GB"
          )
        )
      )
    ),
    Some(SuspensionDetails(suspensionStatus = false, None)),
    Some(false)
  )

  val agentDetailsDesResponse2 = AgentDetailsDesResponse(
    Some(Utr("0123456788")),
    Some(
      AgencyDetails(
        Some("ABC Accountants"),
        Some("abc@xyz.com"),
        Some("07345678901"),
        Some(
          BusinessAddress(
            "Matheson House",
            Some("Grange Central"),
            Some("Town Centre"),
            Some("Telford"),
            Some("TF3 4ER"),
            "GB"
          )
        )
      )
    ),
    Some(SuspensionDetails(suspensionStatus = false, None)),
    Some(false)
  )

  val arn  = Arn("AARN00012345")
  val arn2 = Arn("AARN00012346")

  val utr                      = Utr("1234567890")
  val utr2                     = Utr("1234567891")
  val individualBusinessName   = "First Name QM Last Name QM"
  val organisationBusinessName = "CT AGENT 165"

  "DesConnector getActiveCesaAgentRelationships with a valid NINO" should {
    behave.like(aCheckEndpoint(Nino("AB123456C")))
  }

  "DesConnector getActiveCesaAgentRelationships with a valid UTR" should {
    behave.like(aCheckEndpoint(Utr("7000000002"))) // 7000000002
  }

  "DesConnector getAgentRecord" should {
    "return agency details for a given ARN" in {

      givenDESGetAgentRecord(Arn(arn.value), Some(Utr("0123456789")))

      await(desConnector.getAgentRecord(arn)) shouldBe agentDetailsDesResponse
    }
  }
  "DesConnector getAgentRecord caching check" should {
    "return agency details cached for a given ARN and save record to cache" in {
      givenDESGetAgentRecord(Arn(arn.value), Some(Utr("0123456789")))

      await(desConnector.getAgentRecord(arn)) shouldBe agentDetailsDesResponse
      Thread.sleep(500)
      await(agentDataCache.getFromCache(cacheId = encryptKey(arn.value))) shouldBe Some(agentDetailsDesResponse)

    }

    "return agency details cached for a given ARN,  second from cache" in {
      givenDESGetAgentRecord(Arn(arn.value), Some(Utr("0123456789")))
      await(desConnector.getAgentRecord(arn)) shouldBe agentDetailsDesResponse
      Thread.sleep(500)
      await(desConnector.getAgentRecord(arn)) shouldBe agentDetailsDesResponse
      verifyDESGetAgentRecord(arn, 1)
    }

    "return agency details cached for a given ARN and save record to cache for two agents" in {
      givenDESGetAgentRecord(Arn(arn.value), Some(Utr("0123456789")))
      givenDESGetAgentRecord(Arn(arn2.value), Some(Utr("0123456788")))

      await(desConnector.getAgentRecord(arn)) shouldBe agentDetailsDesResponse
      await(desConnector.getAgentRecord(arn2)) shouldBe agentDetailsDesResponse2
      Thread.sleep(500)
      await(agentDataCache.getFromCache(cacheId = encryptKey(arn.value))) shouldBe Some(agentDetailsDesResponse)
      await(agentDataCache.getFromCache(cacheId = encryptKey(arn2.value))) shouldBe Some(agentDetailsDesResponse2)
    }

    "return agency details cached for a given ARN,  second from cache for two agents" in {
      givenDESGetAgentRecord(Arn(arn.value), Some(Utr("0123456789")))
      givenDESGetAgentRecord(Arn(arn2.value), Some(Utr("0123456788")))
      await(desConnector.getAgentRecord(arn)) shouldBe agentDetailsDesResponse
      await(desConnector.getAgentRecord(arn2)) shouldBe agentDetailsDesResponse2
      Thread.sleep(500)
      await(desConnector.getAgentRecord(arn)) shouldBe agentDetailsDesResponse
      await(desConnector.getAgentRecord(arn2)) shouldBe agentDetailsDesResponse2
      verifyDESGetAgentRecord(arn, 1)
      verifyDESGetAgentRecord(arn2, 1)
    }

    "must fail when the server returns another 5xx status" in {
      givenDesReturnsServerError()
      an[Exception] should be thrownBy await(desConnector.getAgentRecord(arn))
    }

    "must fail when the server returns agent unknown status" in {
      givenAgentIsUnknown404(Arn(arn.value))
      an[Exception] should be thrownBy await(desConnector.getAgentRecord(arn))
    }

  }

  "DesConnector getBusinessNameRecord" should {
    "return business name for individual for a given UTR" in {

      givenDESRespondsWithRegistrationData(identifier = utr, isIndividual = true)

      await(desConnector.getBusinessName(utr.value)) shouldBe Some(individualBusinessName)
    }

    "return business name for organisation for a given UTR" in {

      givenDESRespondsWithRegistrationData(identifier = utr, isIndividual = false)

      await(desConnector.getBusinessName(utr.value)) shouldBe Some(organisationBusinessName)
    }
  }

  "DesConnector getBusinessName caching check" should {
    "return business name cached for a given UTR and save record to cache" in {
      givenDESRespondsWithRegistrationData(identifier = utr, isIndividual = false)

      await(desConnector.getBusinessName(utr.value)) shouldBe Some(organisationBusinessName)
      Thread.sleep(500)
      await(agentNameCache.getFromCache(cacheId = encryptKey(utr.value))).get shouldBe Some(organisationBusinessName)

    }

    "do not cache and throw an exception if DES business name is not found" in {
      givenDESReturnsErrorForRegistration(identifier = utr, responseCode = NOT_FOUND)

      await(desConnector.getBusinessName(utr.value)) shouldBe None
      Thread.sleep(500)
      await(agentNameCache.getFromCache(cacheId = encryptKey(utr.value))) shouldBe None

    }

    "return business name from cache second time called within timeout " in {
      givenDESRespondsWithRegistrationData(identifier = utr, isIndividual = false)
      await(desConnector.getBusinessName(utr.value)) shouldBe Some(organisationBusinessName)
      Thread.sleep(500)
      await(desConnector.getBusinessName(utr.value)) shouldBe Some(organisationBusinessName)
      verifyDESGetAgentRegistrationData(utr, 1)
    }

    "return business name cached for a given UTR and save record to cache for two agents" in {
      givenDESRespondsWithRegistrationData(identifier = utr, isIndividual = false)
      givenDESRespondsWithRegistrationData(identifier = utr2, isIndividual = true)

      await(desConnector.getBusinessName(utr.value)) shouldBe Some(organisationBusinessName)
      await(desConnector.getBusinessName(utr2.value)) shouldBe Some(individualBusinessName)
      Thread.sleep(500)
      await(agentNameCache.getFromCache(cacheId = encryptKey(utr.value))).get shouldBe Some(organisationBusinessName)
      await(agentNameCache.getFromCache(cacheId = encryptKey(utr2.value))).get shouldBe Some(individualBusinessName)
    }

    "return None and do not thow exception when the DES server returns another 5xx status" in {
      givenDesReturnsServerError()
      await(desConnector.getBusinessName(utr.value)) shouldBe None
    }

  }

  private def aCheckEndpoint(identifier: TaxIdentifier) = {
    "return one Agent when client has a single active agent" in {
      val agentId = SaAgentReference("bar")
      givenClientHasRelationshipWithAgentInCESA(identifier, agentId)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)) shouldBe Seq(agentId)
    }

    "return multiple Agents when client has multiple active agents" in {
      val agentIds = Seq("001", "002", "003", "004", "005", "005", "007").map(SaAgentReference.apply)
      givenClientHasRelationshipWithMultipleAgentsInCESA(identifier, agentIds)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)) should contain theSameElementsAs agentIds
    }

    "return empty seq when client has no active relationship with an agent" in {
      givenClientHasNoActiveRelationshipWithAgentInCESA(identifier)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)) shouldBe Seq.empty[SaAgentReference]
    }

    "return empty seq when client has/had no relationship with any agent" in {
      givenClientHasNoRelationshipWithAnyAgentInCESA(identifier)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)) shouldBe Seq.empty[SaAgentReference]
    }

    "return empty seq when client relationship with agent ceased" in {
      givenClientRelationshipWithAgentCeasedInCESA(identifier, "foo")
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)) shouldBe Seq.empty[SaAgentReference]
    }

    "return empty seq when all client's relationships with agents ceased" in {
      givenAllClientRelationshipsWithAgentsCeasedInCESA(
        identifier,
        Seq("001", "002", "003", "004", "005", "005", "007")
      )
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)) shouldBe Seq.empty[SaAgentReference]
    }

    "fail when client id is invalid" in {
      givenClientIdentifierIsInvalid(identifier)
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(identifier))
    }

    "When NOT_FOUND(404) occurs return None" in {
      givenClientIsUnknown404(identifier)
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier)) shouldBe Seq.empty[SaAgentReference]
    }

    "fail when DES is unavailable" in {
      givenDesReturnsServiceUnavailable()
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(identifier))
    }

    "return 502 when DES returns BadGateway error" in {
      givenDesReturnBadGateway()
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(identifier))
    }

    "fail when DES is throwing errors" in {
      givenDesReturnsServerError()
      givenAuditConnector()
      an[Exception] should be thrownBy await(desConnector.getActiveCesaAgentRelationships(identifier))
    }

    "record metrics for GetStatusAgentRelationship" in {
      givenClientHasRelationshipWithAgentInCESA(identifier, SaAgentReference("bar"))
      givenCleanMetricRegistry()
      givenAuditConnector()
      await(desConnector.getActiveCesaAgentRelationships(identifier))
      timerShouldExistsAndBeenUpdated("ConsumedAPI-DES-GetStatusAgentRelationship-GET")
    }

  }
}
