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

///*
// * Copyright 2024 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.agentassurance.controllers
//
//import com.google.inject.AbstractModule
//import org.mongodb.scala.SingleObservableFuture
//import org.scalatest.BeforeAndAfterEach
//import org.scalatestplus.play.guice.GuiceOneServerPerSuite
//import play.api.Application
//import play.api.http.Status.*
//import play.api.inject.guice.GuiceApplicationBuilder
//import play.api.libs.json.Json
//import play.api.libs.ws.DefaultBodyWritables.writeableOf_String
//import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
//import play.api.libs.ws.WSClient
//import play.api.libs.ws.WSResponse
//import play.api.test.Helpers.NOT_FOUND
//import uk.gov.hmrc.agentassurance.stubs.DesStubs
//import uk.gov.hmrc.agentassurance.stubs.HipStubs
//import uk.gov.hmrc.agentassurance.support.AgentAuthStubs
//import uk.gov.hmrc.agentassurance.support.UnitSpec
//import uk.gov.hmrc.agentassurance.support.WireMockSupport
//import uk.gov.hmrc.agentassurance.models.Property
//import uk.gov.hmrc.agentassurance.models.utrcheck.BusinessNameByUtr
//import uk.gov.hmrc.agentassurance.models.utrcheck.UtrDetails
//import uk.gov.hmrc.agentassurance.repositories.PropertiesRepository
//import uk.gov.hmrc.agentassurance.repositories.PropertiesRepositoryImpl
//import uk.gov.hmrc.agentassurance.models.Utr
//import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
//
//import java.time.Clock
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//
//class ManagedUtrsControllerHipISpec
//extends UnitSpec
//with GuiceOneServerPerSuite
//with BeforeAndAfterEach
//with AgentAuthStubs
//with HipStubs
//with WireMockSupport
//with DefaultPlayMongoRepositorySupport[Property] {
//
//  override val repository: PropertiesRepositoryImpl = new PropertiesRepositoryImpl(mongoComponent)
//
//  val moduleWithOverrides: AbstractModule =
//    new AbstractModule() {
//      override def configure(): Unit = {
//        bind(classOf[PropertiesRepository]).toInstance(repository)
//        bind(classOf[Clock]).toInstance(clock)
//      }
//    }
//
//  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
//    .configure(
//      "auditing.enabled" -> false,
//      "microservice.services.auth.host" -> wireMockHost,
//      "microservice.services.auth.port" -> wireMockPort,
//      "microservice.services.des.host" -> wireMockHost,
//      "microservice.services.des.port" -> wireMockPort,
//      "microservice.services.des.environment" -> "test",
//      "microservice.services.des.authorization-token" -> "secret",
//      "microservice.services.enrolment-store-proxy.host" -> wireMockHost,
//      "microservice.services.enrolment-store-proxy.port" -> wireMockPort,
//      "auditing.consumer.baseUri.host" -> wireMockHost,
//      "auditing.consumer.baseUri.port" -> wireMockPort,
//      "internal-auth-token-enabled-on-start" -> false,
//      "agent.name.cache.enabled" -> false,
//      "features.registration-1163-use-hip" -> true,
//      "microservice.services.hip.host" -> wireMockHost,
//      "microservice.services.hip.port" -> wireMockPort,
//      "microservice.services.hip.authorization-token" -> "secret"
//    )
//    .overrides(moduleWithOverrides)
//
//  override implicit lazy val app: Application = appBuilder.build()
//
//  val baseUrl = s"http://localhost:$port"
//
//  val wsClient: WSClient = app.injector.instanceOf[WSClient]
//
//  def getUtrDetails(
//    utr: Utr,
//    nameRequired: java.lang.Boolean = null
//  ): Future[WSResponse] = wsClient
//    .url(s"$baseUrl/agent-assurance/managed-utrs/utr/${utr.value}${Option(nameRequired).map(nr => s"?nameRequired=$nr").getOrElse("")}")
//    .withHttpHeaders("Authorization" -> "Bearer XYZ")
//    .get()
//
//  def listUtrs(
//    collectionName: String,
//    page: Int,
//    pageSize: Int
//  ): Future[WSResponse] = wsClient
//    .url(s"$baseUrl/agent-assurance/managed-utrs/collection/$collectionName?page=$page&pageSize=$pageSize")
//    .withHttpHeaders("Authorization" -> "Bearer XYZ")
//    .get()
//
//  "a getProperty entire List for refusal-to-deal-with" should {
//    behave.like(extractUtrDetailsList("refusal-to-deal-with"))
//  }
//
//  "a getProperty entire List for manually-assured" should {
//    behave.like(extractUtrDetailsList("manually-assured"))
//  }
//
//  "a identifier exists in  getProperty endpoint for refusal-to-deal-with" should {
//    behave.like(checkUtr())
//  }
//
//  private val utr4000000009: Utr = Utr("4000000009")
//
//  def extractUtrDetailsList(collection: String): Unit = {
//    "return 200 OK when properties are present in first page" in {
//      isLoggedInWithoutUserId
//
//      givenHIPRespondsWithRegistrationData(identifier = utr4000000009, isIndividual = true)
//      givenHIPRespondsWithRegistrationData(identifier = Utr("6660717101"), isIndividual = false)
//      givenHIPRespondsWithRegistrationData(identifier = Utr("4660717102"), isIndividual = true)
//      givenHIPRespondsWithRegistrationData(identifier = Utr("2660717103"), isIndividual = false)
//
//      repository.collection.insertOne(Property(key = collection, value = "4000000009")).toFuture().futureValue
//      repository.collection.insertOne(Property(key = collection, value = "6660717101")).toFuture().futureValue
//      repository.collection.insertOne(Property(key = collection, value = "4660717102")).toFuture().futureValue
//      repository.collection.insertOne(Property(key = collection, value = "2660717103")).toFuture().futureValue
//
//      val response =
//        listUtrs(
//          collectionName = collection,
//          page = 1,
//          pageSize = 3
//        ).futureValue
//      response.status shouldBe OK
//      (response.json \ "resources").as[Seq[BusinessNameByUtr]] shouldBe Seq(
//        BusinessNameByUtr("4000000009", Some("First Name QM Last Name QM")),
//        BusinessNameByUtr("6660717101", Some("CT AGENT 165")),
//        BusinessNameByUtr("4660717102", Some("First Name QM Last Name QM"))
//      )
//      (response.json \ "total").as[Int] shouldBe 4
//      (response.json \ "_links" \ "self" \ "href").as[String] should include(
//        s"/agent-assurance/managed-utrs/collection/$collection?page=1&pageSize=3"
//      )
//      (response.json \ "_links" \ "first" \ "href").as[String] should include(
//        s"/agent-assurance/managed-utrs/collection/$collection?page=1&pageSize=3"
//      )
//      (response.json \ "_links" \ "next" \ "href").as[String] should include(
//        s"/agent-assurance/managed-utrs/collection/$collection?page=2&pageSize=3"
//      )
//      (response.json \ "_links" \ "last" \ "href").as[String] should include(
//        s"/agent-assurance/managed-utrs/collection/$collection?page=2&pageSize=3"
//      )
//      (response.json \ "_links" \ "previous" \ "href").toOption.isDefined shouldBe false
//    }
//
//    "return 200 OK when properties are present in second page" in {
//      isLoggedInWithoutUserId
//
//      givenHIPRespondsWithRegistrationData(identifier = utr4000000009, isIndividual = true)
//      givenHIPRespondsWithRegistrationData(identifier = Utr("6660717101"), isIndividual = false)
//      givenHIPRespondsWithRegistrationData(identifier = Utr("4660717102"), isIndividual = true)
//      givenHIPRespondsWithRegistrationData(identifier = Utr("2660717103"), isIndividual = false)
//      givenHIPRespondsWithRegistrationData(identifier = Utr("9660717105"), isIndividual = false)
//
//      repository.collection.insertOne(Property(key = collection, value = "4000000009")).toFuture().futureValue
//      repository.collection.insertOne(Property(key = collection, value = "6660717101")).toFuture().futureValue
//      repository.collection.insertOne(Property(key = collection, value = "4660717102")).toFuture().futureValue
//      repository.collection.insertOne(Property(key = collection, value = "2660717103")).toFuture().futureValue
//      repository.collection.insertOne(Property(key = collection, value = "9660717105")).toFuture().futureValue
//
//      val response =
//        listUtrs(
//          collection,
//          2,
//          2
//        ).futureValue
//      response.status shouldBe OK
//      (response.json \ "resources").as[Seq[BusinessNameByUtr]] shouldBe Seq(
//        BusinessNameByUtr("4660717102", Some("First Name QM Last Name QM")),
//        BusinessNameByUtr("2660717103", Some("CT AGENT 165"))
//      )
//      (response.json \ "total").as[Int] shouldBe 5
//      (response.json \ "_links" \ "self" \ "href").as[String] should include(
//        s"/agent-assurance/managed-utrs/collection/$collection?page=2&pageSize=2"
//      )
//      (response.json \ "_links" \ "first" \ "href").as[String] should include(
//        s"/agent-assurance/managed-utrs/collection/$collection?page=1&pageSize=2"
//      )
//      (response.json \ "_links" \ "previous" \ "href").as[String] should include(
//        s"/agent-assurance/managed-utrs/collection/$collection?page=1&pageSize=2"
//      )
//      (response.json \ "_links" \ "last" \ "href").as[String] should include(
//        s"/agent-assurance/managed-utrs/collection/$collection?page=3&pageSize=2"
//      )
//      (response.json \ "_links" \ "next" \ "href").as[String] should include(
//        s"/agent-assurance/managed-utrs/collection/$collection?page=3&pageSize=2"
//      )
//    }
//
//    "return 200 OK when properties are present in last page" in {
//      isLoggedInWithoutUserId
//
//      givenHIPRespondsWithRegistrationData(identifier = utr4000000009, isIndividual = true)
//      givenHIPRespondsWithRegistrationData(identifier = Utr("6660717101"), isIndividual = false)
//      givenHIPRespondsWithRegistrationData(identifier = Utr("4660717102"), isIndividual = true)
//      givenHIPRespondsWithRegistrationData(identifier = Utr("2660717103"), isIndividual = false)
//
//      repository.collection.insertOne(Property(key = collection, value = "4000000009")).toFuture().futureValue
//      repository.collection.insertOne(Property(key = collection, value = "6660717101")).toFuture().futureValue
//      repository.collection.insertOne(Property(key = collection, value = "4660717102")).toFuture().futureValue
//      repository.collection.insertOne(Property(key = collection, value = "2660717103")).toFuture().futureValue
//
//      val response =
//        listUtrs(
//          collection,
//          2,
//          3
//        ).futureValue
//      response.status shouldBe OK
//      (response.json \ "resources").as[Seq[BusinessNameByUtr]] shouldBe Seq(
//        BusinessNameByUtr("2660717103", Some("CT AGENT 165"))
//      )
//      (response.json \ "total").as[Int] shouldBe 4
//      (response.json \ "_links" \ "self" \ "href").as[String] should include(
//        s"/agent-assurance/managed-utrs/collection/$collection?page=2&pageSize=3"
//      )
//      (response.json \ "_links" \ "first" \ "href").as[String] should include(
//        s"/agent-assurance/managed-utrs/collection/$collection?page=1&pageSize=3"
//      )
//      (response.json \ "_links" \ "previous" \ "href").as[String] should include(
//        s"/agent-assurance/managed-utrs/collection/$collection?page=1&pageSize=3"
//      )
//      (response.json \ "_links" \ "last" \ "href").as[String] should include(
//        s"/agent-assurance/managed-utrs/collection/$collection?page=2&pageSize=3"
//      )
//      (response.json \ "_links" \ "next" \ "href").toOption.isDefined shouldBe false
//    }
//
//    "return empty results when property is not present" in {
//      isLoggedInWithoutUserId
//
//      val response =
//        listUtrs(
//          collection,
//          10,
//          3
//        ).futureValue
//      response.status shouldBe OK
//      (response.json \ "resources").as[Seq[String]] shouldBe Seq.empty
//      (response.json \ "total").as[Int] shouldBe 0
//    }
//  }
//
//  def checkUtr(): Unit = {
//
//    "return 200 OK and correct payload when utr on manually-assured and not refusal-to-deal-with" in {
//      isLoggedInWithoutUserId
//      givenHIPRespondsWithRegistrationData(identifier = utr4000000009, isIndividual = true)
//
//      repository.collection.insertOne(Property(key = "manually-assured", value = "4000000009")).toFuture().futureValue
//
//      val response = getUtrDetails(utr = utr4000000009, nameRequired = true).futureValue
//      response.status shouldBe OK
//
//      response.json.as[UtrDetails] shouldBe UtrDetails(
//        utr = utr4000000009,
//        isManuallyAssured = true,
//        isRefusalToDealWith = false,
//        businessName = Some("First Name QM Last Name QM")
//      )
//    }
//
//    "return 200 OK and correct payload when utr on manually-assured and on refusal-to-deal-with" in {
//      isLoggedInWithoutUserId
//      givenHIPRespondsWithRegistrationData(identifier = utr4000000009, isIndividual = true)
//
//      repository.collection.insertOne(Property(key = "manually-assured", value = "4000000009")).toFuture().futureValue
//      repository.collection.insertOne(Property(key = "refusal-to-deal-with", value = "4000000009")).toFuture().futureValue
//
//      val response = getUtrDetails(utr = utr4000000009, nameRequired = true).futureValue
//      response.status shouldBe OK
//
//      response.json.as[UtrDetails] shouldBe UtrDetails(
//        utr = utr4000000009,
//        isManuallyAssured = true,
//        isRefusalToDealWith = true,
//        businessName = Some("First Name QM Last Name QM")
//      )
//    }
//
//    "return 200 OK and correct payload when utr not on  manually-assured and on refusal-to-deal-with" in {
//      isLoggedInWithoutUserId
//      givenHIPRespondsWithRegistrationData(identifier = utr4000000009, isIndividual = true)
//
//      repository.collection.insertOne(Property(key = "refusal-to-deal-with", value = "4000000009")).toFuture().futureValue
//
//      val response = getUtrDetails(utr = utr4000000009, nameRequired = true).futureValue
//      response.status shouldBe OK
//
//      response.json.as[UtrDetails] shouldBe UtrDetails(
//        utr = utr4000000009,
//        isManuallyAssured = false,
//        isRefusalToDealWith = true,
//        businessName = Some("First Name QM Last Name QM")
//      )
//    }
//
//    "return 200 OK and correct payload when utr not on  manually-assured and not on refusal-to-deal-with" in {
//      isLoggedInWithoutUserId
//      givenHIPRespondsWithRegistrationData(identifier = utr4000000009, isIndividual = true)
//
//      val response = getUtrDetails(utr = utr4000000009, nameRequired = true).futureValue
//      response.status shouldBe OK
//
//      response.json.as[UtrDetails] shouldBe UtrDetails(
//        utr = utr4000000009,
//        isManuallyAssured = false,
//        isRefusalToDealWith = false,
//        businessName = Some("First Name QM Last Name QM")
//      )
//
//    }
//
//    "return 200 OK and correct payload when utr on manually-assured and not refusal-to-deal-with and no name" in {
//      isLoggedInWithoutUserId
//      givenHIPReturnsErrorForRegistration(identifier = utr4000000009, responseCode = NOT_FOUND)
//
//      repository.collection.insertOne(Property(key = "manually-assured", value = "4000000009")).toFuture().futureValue
//
//      val response = getUtrDetails(utr = utr4000000009, nameRequired = true).futureValue
//      response.status shouldBe OK
//
//      response.json.as[UtrDetails] shouldBe UtrDetails(
//        utr = utr4000000009,
//        isManuallyAssured = true,
//        isRefusalToDealWith = false,
//        businessName = None
//      )
//    }
//
//    "return 200 OK and correct payload when utr on manually-assured and on refusal-to-deal-with and no name" in {
//      isLoggedInWithoutUserId
//      givenHIPReturnsErrorForRegistration(identifier = utr4000000009, responseCode = NOT_FOUND)
//      repository.collection.insertOne(Property(key = "manually-assured", value = "4000000009")).toFuture().futureValue
//      repository.collection.insertOne(Property(key = "refusal-to-deal-with", value = "4000000009")).toFuture().futureValue
//
//      val response = getUtrDetails(utr = utr4000000009, nameRequired = true).futureValue
//      response.status shouldBe OK
//
//      response.json.as[UtrDetails] shouldBe UtrDetails(
//        utr = utr4000000009,
//        isManuallyAssured = true,
//        isRefusalToDealWith = true,
//        businessName = None
//      )
//    }
//
//    "return 200 OK and correct payload when utr not on  manually-assured and on refusal-to-deal-with and no name" in {
//      isLoggedInWithoutUserId
//      givenHIPReturnsErrorForRegistration(identifier = utr4000000009, responseCode = NOT_FOUND)
//
//      repository.collection.insertOne(Property(key = "refusal-to-deal-with", value = "4000000009")).toFuture().futureValue
//
//      val response = getUtrDetails(utr = utr4000000009, nameRequired = true).futureValue
//      response.status shouldBe OK
//
//      response.json.as[UtrDetails] shouldBe UtrDetails(
//        utr = utr4000000009,
//        isManuallyAssured = false,
//        isRefusalToDealWith = true,
//        businessName = None
//      )
//    }
//
//    "return 200 OK and correct payload when utr not on  manually-assured and not on refusal-to-deal-with and no name" in {
//      isLoggedInWithoutUserId
//      givenHIPReturnsErrorForRegistration(identifier = utr4000000009, responseCode = NOT_FOUND)
//
//      val response = getUtrDetails(utr = utr4000000009, nameRequired = true).futureValue
//      response.status shouldBe OK
//
//      response.json.as[UtrDetails] shouldBe UtrDetails(
//        utr = utr4000000009,
//        isManuallyAssured = false,
//        isRefusalToDealWith = false,
//        businessName = None
//      )
//    }
//
//    "return 200 OK and correct payload when utr on manually-assured and not refusal-to-deal-with and no name is not required" in {
//      isLoggedInWithoutUserId
//      givenHIPRespondsWithRegistrationData(identifier = utr4000000009, isIndividual = true)
//      repository.collection.insertOne(Property(key = "manually-assured", value = "4000000009")).toFuture().futureValue
//
//      val response = getUtrDetails(utr = utr4000000009, nameRequired = false).futureValue
//      response.status shouldBe OK
//
//      response.json.as[UtrDetails] shouldBe UtrDetails(
//        utr = utr4000000009,
//        isManuallyAssured = true,
//        isRefusalToDealWith = false,
//        businessName = None
//      )
//    }
//
//    "nameRequired is optional and defaults to false" in {
//      isLoggedInWithoutUserId
//      givenHIPRespondsWithRegistrationData(identifier = utr4000000009, isIndividual = true)
//      repository.collection.insertOne(Property(key = "manually-assured", value = "4000000009")).toFuture().futureValue
//
//      val response = getUtrDetails(utr = utr4000000009).futureValue
//      response.status shouldBe OK
//
//      response.json.as[UtrDetails] shouldBe UtrDetails(
//        utr = utr4000000009,
//        isManuallyAssured = true,
//        isRefusalToDealWith = false,
//        businessName = None
//      )
//    }
//  }
//
//}
