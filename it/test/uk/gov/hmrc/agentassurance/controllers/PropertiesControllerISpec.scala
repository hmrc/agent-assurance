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

package test.uk.gov.hmrc.agentassurance.controllers

import java.time.Clock

import scala.concurrent.duration.DurationInt
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

import com.google.inject.AbstractModule
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSResponse
import play.api.Application
import test.uk.gov.hmrc.agentassurance.support.AgentAuthStubs
import test.uk.gov.hmrc.agentassurance.support.UnitSpec
import test.uk.gov.hmrc.agentassurance.support.WireMockSupport
import uk.gov.hmrc.agentassurance.models.Property
import uk.gov.hmrc.agentassurance.repositories.PropertiesRepository
import uk.gov.hmrc.agentassurance.repositories.PropertiesRepositoryImpl
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

class PropertiesControllerISpec
extends UnitSpec
with GuiceOneServerPerSuite
with BeforeAndAfterEach
with AgentAuthStubs
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
      "internal-auth-token-enabled-on-start" -> false
    )
    .overrides(moduleWithOverrides)

  override implicit lazy val app: Application = appBuilder.build()

  val url = s"http://localhost:$port/agent-assurance"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  def createProperty(
    key: String,
    value: String
  ) = wsClient
    .url(s"$url/$key")
    .withHttpHeaders("Authorization" -> "Bearer XYZ")
    .post(Json.obj("value" -> value))

  def isAssured(
    key: String,
    identifier: String
  ): Future[WSResponse] = wsClient.url(s"$url/$key/utr/$identifier").withHttpHeaders("Authorization" -> "Bearer XYZ").get()

  def getEntirePaginatedList(
    key: String,
    page: Int,
    pageSize: Int
  ): Future[WSResponse] = {
    wsClient.url(s"$url/$key?page=$page&pageSize=$pageSize").withHttpHeaders("Authorization" -> "Bearer XYZ").get()
  }

  def deleteProperty(
    key: String,
    identifier: String
  ): Future[WSResponse] = wsClient.url(s"$url/$key/utr/$identifier").withHttpHeaders("Authorization" -> "Bearer XYZ").delete()

  "a getProperty entire List for refusal-to-deal-with" should {
    behave.like(getPropertyList("refusal-to-deal-with"))
  }

  "a getProperty entire List for manually-assured" should {
    behave.like(getPropertyList("manually-assured"))
  }

  "a createProperty endpoint for refusal-to-deal-with" should {
    behave.like(createPropertyTests("refusal-to-deal-with"))
  }

  "a createProperty endpoint for manually-assured" should {
    behave.like(createPropertyTests("manually-assured"))
  }

  "a identifier exists in  getProperty endpoint for refusal-to-deal-with" should {
    behave.like(identifierExistsTests("refusal-to-deal-with"))
  }

  "a identifier exists in getProperty endpoint for manually-assured" should {
    behave.like(identifierExistsTests("manually-assured", false))
  }

  "a deleteIdentifierInProperty endpoint for refusal-to-deal-with" should {
    behave.like(deletePropertyTests("refusal-to-deal-with"))
  }

  "a deleteIdentifierInProperty endpoint for manually-assured" should {
    behave.like(deletePropertyTests("manually-assured", false))
  }

  def createPropertyTests(key: String) = {

    "return 201 when property is not already present" in {
      isLoggedInWithoutUserId

      val response: WSResponse = Await.result(createProperty(key, "4000000009"), 10 seconds)
      response.status shouldBe CREATED
    }

    "return 409 (with appropriate reason) when property is already present" in {
      isLoggedInWithoutUserId

      val response: WSResponse = Await.result(createProperty(key, "4000000009"), 10 seconds)
      response.status shouldBe CREATED

      val response2: WSResponse = Await.result(createProperty(key, "4000000009"), 10 seconds)
      response2.status shouldBe CONFLICT
      (Json.parse(response2.body) \ "message").as[String] shouldBe "Property already exists"
    }

    "return 400 (with appropriate reason) json is not well formed" in {
      isLoggedInWithoutUserId

      val badlyFormedJson =
        s"""
           |{
           |   "value": "missingQuote
           |}
                 """.stripMargin

      val response: WSResponse = Await.result(
        wsClient
          .url(s"$url/$key")
          .withHttpHeaders("Content-Type" -> "application/json")
          .post(badlyFormedJson),
        10 seconds
      )
      response.status shouldBe BAD_REQUEST
      response.body.toLowerCase.contains("invalid json: illegal unquoted character") shouldBe true
    }

    "return 400 (with appropriate reason) json does not conform to the api" in {
      isLoggedInWithoutUserId
      val payload = Json.obj("foo" -> "ewrewr", "bar" -> "retrwt")

      val response: WSResponse = Await.result(
        wsClient
          .url(s"$url/$key")
          .withHttpHeaders("Authorization" -> "Bearer XYZ")
          .post(payload),
        10 seconds
      )
      response.status shouldBe BAD_REQUEST
      response.body.contains("json failed validation") shouldBe true
    }
  }

  def identifierExistsTests(
    key: String,
    isR2dw: Boolean = true
  ) = {
    "return 200 OK when property is present" in {
      isLoggedInWithoutUserId

      Await.result(createProperty(key, "4000000009"), 10 seconds)

      val response = Await.result(isAssured(key, "4000000009   "), 10 seconds)
      if (isR2dw)
        response.status shouldBe FORBIDDEN
      else
        response.status shouldBe OK
    }
    "return 404 when property is not present" in {
      isLoggedInWithoutUserId

      val response = Await.result(isAssured(key, "4000000009"), 10 seconds)
      if (isR2dw)
        response.status shouldBe OK
      else
        response.status shouldBe FORBIDDEN
    }

    "INVALID UTR" in {
      isLoggedInWithoutUserId
      val response = Await.result(isAssured(key, "INVALID_UTR"), 10 seconds)
      response.status shouldBe BAD_REQUEST
      response.statusText shouldBe "Bad Request"
    }
  }

  def getPropertyList(key: String) = {
    "return 200 OK when properties are present in first page" in {
      isLoggedInWithoutUserId

      Await.result(createProperty(key, "4000000009"), 10 seconds)
      Await.result(createProperty(key, "6660717101"), 10 seconds)
      Await.result(createProperty(key, "4660717102"), 10 seconds)
      Await.result(createProperty(key, "2660717103"), 10 seconds)

      val response = Await.result(getEntirePaginatedList(key, 1, 3), 10 seconds)
      response.status shouldBe OK
      (response.json \ "resources").as[Seq[String]] shouldBe Seq(
        "4000000009",
        "6660717101",
        "4660717102"
      )
      (response.json \ "total").as[Int] shouldBe 4
      (response.json \ "_links" \ "self" \ "href").as[String] should include(s"/agent-assurance/$key?page=1&pageSize=3")
      (response.json \ "_links" \ "first" \ "href").as[String] should include(
        s"/agent-assurance/$key?page=1&pageSize=3"
      )
      (response.json \ "_links" \ "next" \ "href").as[String] should include(s"/agent-assurance/$key?page=2&pageSize=3")
      (response.json \ "_links" \ "last" \ "href").as[String] should include(s"/agent-assurance/$key?page=2&pageSize=3")
      (response.json \ "_links" \ "previous" \ "href").toOption.isDefined shouldBe false
    }

    "return 200 OK when properties are present in second page" in {
      isLoggedInWithoutUserId

      Await.result(createProperty(key, "4000000009"), 10 seconds)
      Await.result(createProperty(key, "6660717101"), 10 seconds)
      Await.result(createProperty(key, "4660717102"), 10 seconds)
      Await.result(createProperty(key, "2660717103"), 10 seconds)
      Await.result(createProperty(key, "9660717105"), 10 seconds)

      val response = Await.result(getEntirePaginatedList(key, 2, 2), 10 seconds)
      response.status shouldBe OK
      (response.json \ "resources").as[Seq[String]] shouldBe Seq("4660717102", "2660717103")
      (response.json \ "total").as[Int] shouldBe 5
      (response.json \ "_links" \ "self" \ "href").as[String] should include(s"/agent-assurance/$key?page=2&pageSize=2")
      (response.json \ "_links" \ "first" \ "href").as[String] should include(
        s"/agent-assurance/$key?page=1&pageSize=2"
      )
      (response.json \ "_links" \ "previous" \ "href").as[String] should include(
        s"/agent-assurance/$key?page=1&pageSize=2"
      )
      (response.json \ "_links" \ "last" \ "href").as[String] should include(s"/agent-assurance/$key?page=3&pageSize=2")
      (response.json \ "_links" \ "next" \ "href").as[String] should include(s"/agent-assurance/$key?page=3&pageSize=2")
    }

    "return 200 OK when properties are present in last page" in {
      isLoggedInWithoutUserId

      Await.result(createProperty(key, "4000000009"), 10 seconds)
      Await.result(createProperty(key, "6660717101"), 10 seconds)
      Await.result(createProperty(key, "4660717102"), 10 seconds)
      Await.result(createProperty(key, "2660717103"), 10 seconds)

      val response = Await.result(getEntirePaginatedList(key, 2, 3), 10 seconds)
      response.status shouldBe OK
      (response.json \ "resources").as[Seq[String]] shouldBe Seq("2660717103")
      (response.json \ "total").as[Int] shouldBe 4
      (response.json \ "_links" \ "self" \ "href").as[String] should include(s"/agent-assurance/$key?page=2&pageSize=3")
      (response.json \ "_links" \ "first" \ "href").as[String] should include(
        s"/agent-assurance/$key?page=1&pageSize=3"
      )
      (response.json \ "_links" \ "previous" \ "href").as[String] should include(
        s"/agent-assurance/$key?page=1&pageSize=3"
      )
      (response.json \ "_links" \ "last" \ "href").as[String] should include(s"/agent-assurance/$key?page=2&pageSize=3")
      (response.json \ "_links" \ "next" \ "href").toOption.isDefined shouldBe false
    }

    "return empty results when property is not present" in {
      isLoggedInWithoutUserId

      val response = Await.result(getEntirePaginatedList(key, 10, 3), 10 seconds)
      response.status shouldBe OK
      (response.json \ "resources").as[Seq[String]] shouldBe Seq.empty
      (response.json \ "total").as[Int] shouldBe 0
    }
  }

  def deletePropertyTests(
    key: String,
    isR2dw: Boolean = true
  ) = {
    "return 204 when property is present" in {
      isLoggedInWithoutUserId
      Await.result(createProperty(key, "4000000009"), 10 seconds)

      val response = Await.result(deleteProperty(key, "4000000009"), 10 seconds)
      response.status shouldBe NO_CONTENT

      val response2 = Await.result(isAssured(key, "4000000009"), 10 seconds)
      if (isR2dw)
        response2.status shouldBe OK
      else
        response2.status shouldBe FORBIDDEN
    }
    "return 404 when property is not present" in {
      isLoggedInWithoutUserId
      val response = Await.result(deleteProperty(key, "4000000009"), 10 seconds)
      response.status shouldBe NOT_FOUND
    }

    "INVALID UTR" in {
      isLoggedInWithoutUserId
      val response = Await.result(deleteProperty(key, "INVALID_UTR"), 10 seconds)
      response.status shouldBe BAD_REQUEST
      response.statusText shouldBe "Bad Request"
    }
  }

}
