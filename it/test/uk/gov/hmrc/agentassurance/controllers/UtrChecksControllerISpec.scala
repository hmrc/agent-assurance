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

package uk.gov.hmrc.agentassurance.controllers

import com.google.inject.AbstractModule
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.NOT_FOUND
import test.uk.gov.hmrc.agentassurance.stubs.DesStubs
import test.uk.gov.hmrc.agentassurance.support.AgentAuthStubs
import test.uk.gov.hmrc.agentassurance.support.UnitSpec
import test.uk.gov.hmrc.agentassurance.support.WireMockSupport
import uk.gov.hmrc.agentassurance.models.Property
import uk.gov.hmrc.agentassurance.models.utrcheck.UtrChecksResponse
import uk.gov.hmrc.agentassurance.repositories.PropertiesRepository
import uk.gov.hmrc.agentassurance.repositories.PropertiesRepositoryImpl
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.Clock
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt
import scala.concurrent.Await
import scala.concurrent.Future
import scala.language.postfixOps

class UtrChecksControllerISpec
extends UnitSpec
with GuiceOneServerPerSuite
with BeforeAndAfterEach
with AgentAuthStubs
with DesStubs
with WireMockSupport
with DefaultPlayMongoRepositorySupport[Property] {

  override lazy val repository = new PropertiesRepositoryImpl(mongoComponent)

  val moduleWithOverrides: AbstractModule =
    new AbstractModule() {
      override def configure(): Unit = {
        bind(classOf[PropertiesRepository]).toInstance(repository)
        bind(classOf[Clock]).toInstance(clock)
      }
    }

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
    .configure(
      "auditing.enabled" -> false,
      "microservice.services.auth.host" -> wireMockHost,
      "microservice.services.auth.port" -> wireMockPort,
      "microservice.services.des.host" -> wireMockHost,
      "microservice.services.des.port" -> wireMockPort,
      "microservice.services.des.environment" -> "test",
      "microservice.services.des.authorization-token" -> "secret",
      "microservice.services.enrolment-store-proxy.host" -> wireMockHost,
      "microservice.services.enrolment-store-proxy.port" -> wireMockPort,
      "auditing.consumer.baseUri.host" -> wireMockHost,
      "auditing.consumer.baseUri.port" -> wireMockPort,
      "internal-auth-token-enabled-on-start" -> false,
      "agent.name.cache.enabled" -> false
    )
    .overrides(moduleWithOverrides)

  override implicit lazy val app: Application = appBuilder.build()

  implicit val defaultTimeout: Duration = 5 seconds

  def await[A](future: Future[A])(implicit timeout: Duration) = Await.result(future, timeout)

  val url = s"http://localhost:$port/agent-assurance/restricted-collection-check"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  def getUtrCheck(
    utr: String,
    nameRequired: Boolean
  ): Future[WSResponse] = wsClient.url(s"$url/utr/$utr?nameRequired=$nameRequired").withHttpHeaders("Authorization" -> "Bearer XYZ").get()

  "a identifier exists in  getProperty endpoint for refusal-to-deal-with" should {
    behave.like(checkUtr())
  }

  def checkUtr() = {

    "return 200 OK and correct payload when utr on manually-assured and not refusal-to-deal-with" in {
      isLoggedInWithoutUserId
      givenDESRespondsWithRegistrationData(identifier = Utr("4000000009"), isIndividual = true)

      await(repository.collection.insertOne(Property(key = "manually-assured", value = "4000000009")).toFuture())

      val response = Await.result(getUtrCheck(utr = "4000000009", nameRequired = true), 10 seconds)
      response.status shouldBe OK

      response.json.as[UtrChecksResponse] shouldBe UtrChecksResponse(
        isManuallyAssured = true,
        isRefusalToDealWith = false,
        businessName = Some("First Name QM Last Name QM")
      )
    }

    "return 200 OK and correct payload when utr on manually-assured and on refusal-to-deal-with" in {
      isLoggedInWithoutUserId
      givenDESRespondsWithRegistrationData(identifier = Utr("4000000009"), isIndividual = true)

      await(repository.collection.insertOne(Property(key = "manually-assured", value = "4000000009")).toFuture())
      await(repository.collection.insertOne(Property(key = "refusal-to-deal-with", value = "4000000009")).toFuture())

      val response = Await.result(getUtrCheck(utr = "4000000009", nameRequired = true), 10 seconds)
      response.status shouldBe OK

      response.json.as[UtrChecksResponse] shouldBe UtrChecksResponse(
        isManuallyAssured = true,
        isRefusalToDealWith = true,
        businessName = Some("First Name QM Last Name QM")
      )
    }

    "return 200 OK and correct payload when utr not on  manually-assured and on refusal-to-deal-with" in {
      isLoggedInWithoutUserId
      givenDESRespondsWithRegistrationData(identifier = Utr("4000000009"), isIndividual = true)

      await(repository.collection.insertOne(Property(key = "refusal-to-deal-with", value = "4000000009")).toFuture())

      val response = Await.result(getUtrCheck(utr = "4000000009", nameRequired = true), 10 seconds)
      response.status shouldBe OK

      response.json.as[UtrChecksResponse] shouldBe UtrChecksResponse(
        isManuallyAssured = false,
        isRefusalToDealWith = true,
        businessName = Some("First Name QM Last Name QM")
      )
    }

    "return 200 OK and correct payload when utr not on  manually-assured and not on refusal-to-deal-with" in {
      isLoggedInWithoutUserId
      givenDESRespondsWithRegistrationData(identifier = Utr("4000000009"), isIndividual = true)

      val response = Await.result(getUtrCheck(utr = "4000000009", nameRequired = true), 10 seconds)
      response.status shouldBe OK

      response.json.as[UtrChecksResponse] shouldBe UtrChecksResponse(
        isManuallyAssured = false,
        isRefusalToDealWith = false,
        businessName = Some("First Name QM Last Name QM")
      )

    }

    "return 200 OK and correct payload when utr on manually-assured and not refusal-to-deal-with and no name" in {
      isLoggedInWithoutUserId
      givenDESReturnsErrorForRegistration(identifier = Utr("4000000009"), responseCode = NOT_FOUND)

      await(repository.collection.insertOne(Property(key = "manually-assured", value = "4000000009")).toFuture())

      val response = Await.result(getUtrCheck(utr = "4000000009", nameRequired = true), 10 seconds)
      response.status shouldBe OK

      response.json.as[UtrChecksResponse] shouldBe UtrChecksResponse(
        isManuallyAssured = true,
        isRefusalToDealWith = false,
        businessName = None
      )
    }

    "return 200 OK and correct payload when utr on manually-assured and on refusal-to-deal-with and no name" in {
      isLoggedInWithoutUserId
      givenDESReturnsErrorForRegistration(identifier = Utr("4000000009"), responseCode = NOT_FOUND)

      await(repository.collection.insertOne(Property(key = "manually-assured", value = "4000000009")).toFuture())
      await(repository.collection.insertOne(Property(key = "refusal-to-deal-with", value = "4000000009")).toFuture())

      val response = Await.result(getUtrCheck(utr = "4000000009", nameRequired = true), 10 seconds)
      response.status shouldBe OK

      response.json.as[UtrChecksResponse] shouldBe UtrChecksResponse(
        isManuallyAssured = true,
        isRefusalToDealWith = true,
        businessName = None
      )
    }

    "return 200 OK and correct payload when utr not on  manually-assured and on refusal-to-deal-with and no name" in {
      isLoggedInWithoutUserId
      givenDESReturnsErrorForRegistration(identifier = Utr("4000000009"), responseCode = NOT_FOUND)

      await(repository.collection.insertOne(Property(key = "refusal-to-deal-with", value = "4000000009")).toFuture())

      val response = Await.result(getUtrCheck(utr = "4000000009", nameRequired = true), 10 seconds)
      response.status shouldBe OK

      response.json.as[UtrChecksResponse] shouldBe UtrChecksResponse(
        isManuallyAssured = false,
        isRefusalToDealWith = true,
        businessName = None
      )
    }

    "return 200 OK and correct payload when utr not on  manually-assured and not on refusal-to-deal-with and no name" in {
      isLoggedInWithoutUserId
      givenDESReturnsErrorForRegistration(identifier = Utr("4000000009"), responseCode = NOT_FOUND)

      val response = Await.result(getUtrCheck(utr = "4000000009", nameRequired = true), 10 seconds)
      response.status shouldBe OK

      response.json.as[UtrChecksResponse] shouldBe UtrChecksResponse(
        isManuallyAssured = false,
        isRefusalToDealWith = false,
        businessName = None
      )
    }

    "return 200 OK and correct payload when utr on manually-assured and not refusal-to-deal-with and no name is not required" in {
      isLoggedInWithoutUserId
      givenDESRespondsWithRegistrationData(identifier = Utr("4000000009"), isIndividual = true)
      await(repository.collection.insertOne(Property(key = "manually-assured", value = "4000000009")).toFuture())

      val response = Await.result(getUtrCheck(utr = "4000000009", nameRequired = false), 10 seconds)
      response.status shouldBe OK

      response.json.as[UtrChecksResponse] shouldBe UtrChecksResponse(
        isManuallyAssured = true,
        isRefusalToDealWith = false,
        businessName = None
      )
    }

  }

}
