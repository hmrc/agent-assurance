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

import com.github.tomakehurst.wiremock.client.WireMock.*

import scala.concurrent.ExecutionContext
import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Application
import play.api.Configuration
import uk.gov.hmrc.agentassurance.stubs.DataStreamStub
import uk.gov.hmrc.agentassurance.stubs.HipStubs
import uk.gov.hmrc.agentassurance.support.UnitSpec
import uk.gov.hmrc.agentassurance.support.WireMockSupport
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.models.Utr
import uk.gov.hmrc.agentassurance.repositories.AgencyDetailsCacheRepository
import uk.gov.hmrc.agentassurance.repositories.AgencyNameCacheRepository
import uk.gov.hmrc.agentassurance.services.CacheProvider
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.SymmetricCryptoFactory.aesGcmCrypto
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.http.RequestId
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport
import uk.gov.hmrc.mongo.CurrentTimestampSupport

class HipConnectorISpec
extends UnitSpec
with GuiceOneAppPerSuite
with WireMockSupport
with HipStubs
with DataStreamStub
with CleanMongoCollectionSupport {

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val ec: ExecutionContext = ExecutionContext.global
  private implicit val request: Request[AnyContentAsEmpty.type] = FakeRequest()
  private implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  private implicit val config: Config = app.injector.instanceOf[Config]
  private implicit lazy val as: ActorSystem = ActorSystem()
  private implicit val crypto: Encrypter & Decrypter = aesGcmCrypto("0xbYzrPV9/GmVEGazywGswm7yRYoWy2BraeJnjOUgcY=")

  private val agentDataCache =
    new AgencyDetailsCacheRepository(
      app.injector.instanceOf[Configuration],
      mongoComponent,
      new CurrentTimestampSupport
    )

  private val agentNameCache =
    new AgencyNameCacheRepository(
      app.injector.instanceOf[Configuration],
      mongoComponent,
      new CurrentTimestampSupport
    )

  override implicit lazy val app: Application = appBuilder
    .build()

  val cacheProvider =
    new CacheProvider(
      agentDataCache,
      agentNameCache,
      app.injector.instanceOf[Configuration]
    )

  val hipConnector =
    new HipConnector(
      appConfig,
      app.injector.instanceOf[HttpClientV2],
      cacheProvider,
      config,
      as
    )

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.host" -> wireMockHost,
      "microservice.services.auth.port" -> wireMockPort,
      "microservice.services.hip.host" -> wireMockHost,
      "microservice.services.hip.port" -> wireMockPort,
      "microservice.services.hip.authorization-token" -> "secret",
      "microservice.services.enrolment-store-proxy.host" -> wireMockHost,
      "microservice.services.enrolment-store-proxy.port" -> wireMockPort,
      "auditing.consumer.baseUri.host" -> wireMockHost,
      "auditing.consumer.baseUri.port" -> wireMockPort,
      "internal-auth-token-enabled-on-start" -> false,
      "http-verbs.retries.intervals" -> List("1ms"),
      "agent.cache.enabled" -> true,
      "agent.cache.expires" -> "1 second",
      "auditing.enabled" -> false,
      "rate-limiter.business-names.max-calls-per-second" -> 10,
      "agent.name.cache.enabled" -> true,
      "agent.name.cache.expires" -> "1 second"
    )
    .bindings(bind[HipConnector].toInstance(hipConnector))

  val utr = Utr("1234567890")
  val utr2 = Utr("1234567891")
  val individualBusinessName = "First Name QM Last Name QM"
  val organisationBusinessName = "CT AGENT 165"

  "HipConnector" should {
    "send correct headers to HIP" in {
      val identifier = Utr("1234567890")

      implicit val hcWithIds: HeaderCarrier = hc.copy(
        requestId = Some(RequestId("request-id")),
        sessionId = Some(SessionId("session-id"))
      )

      stubFor(
        post(urlEqualTo(s"/RESTAdapter/registration/utr/${identifier.value}"))
          .withHeader("Authorization", equalTo("Basic secret"))
          .withHeader("correlationid", matching(".+"))
          .withHeader("X-Originating-System", equalTo("MDTP"))
          .withHeader("X-Receipt-Date", matching(".+"))
          .withHeader("X-Transmitting-System", equalTo("HIP"))
          .withHeader(HeaderNames.xRequestId, equalTo("request-id"))
          .withHeader(HeaderNames.xSessionId, equalTo("session-id"))
          .willReturn(aResponse().withStatus(OK).withBody(Json.obj().toString()))
      )

      await(hipConnector.getBusinessName(identifier.value)) shouldBe None
    }
  }

  "HipConnector getBusinessName success" should {
    "return business name for individual for a given UTR" in {
      givenHIPRespondsWithRegistrationData(identifier = utr, isIndividual = true)

      await(hipConnector.getBusinessName(utr.value)) shouldBe Some(individualBusinessName)
    }

    "return business name for organisation for a given UTR" in {
      givenHIPRespondsWithRegistrationData(identifier = utr, isIndividual = false)

      await(hipConnector.getBusinessName(utr.value)) shouldBe Some(organisationBusinessName)
    }
  }

  "HipConnector getBusinessName error handling" should {

    val noMatchFound422ErrorBody =
      """
        |{
        |  "errors": {
        |    "code": "002",
        |    "processingDate": "2022-01-31T09:26:17Z",
        |    "text": "No match found"
        |  }
        |}
      """.stripMargin

    val other422ErrorBody =
      """
        |{
        |  "errors": {
        |    "code": "003",
        |    "processingDate": "2022-01-31T09:26:17Z",
        |    "text": "Some other error"
        |  }
        |}
      """.stripMargin

    "return None when HIP responds with a 422 and error code 002 (no match found)" in {
      val utrNoMatch = Utr("3330000001")
      givenHIPReturnsErrorForRegistration(
        identifier = utrNoMatch,
        responseCode = UNPROCESSABLE_ENTITY,
        errorMessage = noMatchFound422ErrorBody
      )

      await(hipConnector.getBusinessName(utrNoMatch.value)) shouldBe None
    }

    "return \"Error retrieving name\" when HIP responds with a 422 and a different error code" in {
      val utrOtherCode = Utr("3330000002")
      givenHIPReturnsErrorForRegistration(
        identifier = utrOtherCode,
        responseCode = UNPROCESSABLE_ENTITY,
        errorMessage = other422ErrorBody
      )

      await(hipConnector.getBusinessName(utrOtherCode.value)) shouldBe Some("Error retrieving name")
    }

    "return \"Error retrieving name\" when HIP responds with a 422 and a non-JSON body" in {
      val utrMalformed = Utr("3330000003")
      givenHIPReturnsErrorForRegistration(
        identifier = utrMalformed,
        responseCode = UNPROCESSABLE_ENTITY,
        errorMessage = "this is not valid json"
      )

      await(hipConnector.getBusinessName(utrMalformed.value)) shouldBe Some("Error retrieving name")
    }

    "return \"Error retrieving name\" when HIP responds with a non-422 error response" in {
      val utrBadRequest = Utr("3330000004")
      givenHIPReturnsErrorForRegistration(
        identifier = utrBadRequest,
        responseCode = BAD_REQUEST,
        errorMessage = "another error"
      )

      await(hipConnector.getBusinessName(utrBadRequest.value)) shouldBe Some("Error retrieving name")
    }

    "not cache the result when HIP responds with a 422 and error code 002 (no match found)" in {
      val utrNoMatchCache = Utr("3330000005")
      givenHIPReturnsErrorForRegistration(
        identifier = utrNoMatchCache,
        responseCode = UNPROCESSABLE_ENTITY,
        errorMessage = noMatchFound422ErrorBody
      )

      await(hipConnector.getBusinessName(utrNoMatchCache.value)) shouldBe None
      Thread.sleep(500)
      await(agentNameCache.getFromCache(cacheId = utrNoMatchCache.value)) shouldBe None
    }
  }

  "HipConnector getBusinessName caching check" should {
    "return business name cached for a given UTR and save record to cache" in {
      givenHIPRespondsWithRegistrationData(identifier = utr, isIndividual = false)

      await(hipConnector.getBusinessName(utr.value)) shouldBe Some(organisationBusinessName)
      Thread.sleep(500)
      await(agentNameCache.getFromCache(cacheId = utr.value)).get shouldBe Some(organisationBusinessName)

    }

    "return business name from cache second time called within timeout " in {
      givenHIPRespondsWithRegistrationData(identifier = utr, isIndividual = false)
      await(hipConnector.getBusinessName(utr.value)) shouldBe Some(organisationBusinessName)
      Thread.sleep(500)
      await(hipConnector.getBusinessName(utr.value)) shouldBe Some(organisationBusinessName)
      verifyHIPGetAgentRegistrationData(utr)
    }

    "return business name cached for a given UTR and save record to cache for two agents" in {
      givenHIPRespondsWithRegistrationData(identifier = utr, isIndividual = false)
      givenHIPRespondsWithRegistrationData(identifier = utr2, isIndividual = true)

      await(hipConnector.getBusinessName(utr.value)) shouldBe Some(organisationBusinessName)
      await(hipConnector.getBusinessName(utr2.value)) shouldBe Some(individualBusinessName)
      Thread.sleep(500)
      await(agentNameCache.getFromCache(cacheId = utr.value)).get shouldBe Some(organisationBusinessName)
      await(agentNameCache.getFromCache(cacheId = utr2.value)).get shouldBe Some(individualBusinessName)
    }
  }

}
