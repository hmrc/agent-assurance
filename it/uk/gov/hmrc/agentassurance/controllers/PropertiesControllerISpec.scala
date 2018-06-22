package uk.gov.hmrc.agentassurance.controllers

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt
import play.api.http.Status._

class PropertiesControllerISpec extends UnitSpec
  with GuiceOneServerPerSuite with BeforeAndAfterEach with MongoSpecSupport {

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}")

  override implicit lazy val app: Application = appBuilder.build()

  val url = s"http://localhost:$port/agent-assurance"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  def createProperty(key: String, value: String) =
    wsClient.url(s"$url/$key").post(Json.obj("value" -> value))

  def isAssured(key: String, identifier: String): Future[WSResponse] = wsClient.url(s"$url/$key/utr/$identifier").get()

  def getEntirePaginatedList(key: String, page: Int, pageSize: Int): Future[WSResponse] = {wsClient.url(s"$url/$key?page=$page&pageSize=$pageSize").get()}

  def deleteProperty(key: String, identifier: String): Future[WSResponse] = wsClient.url(s"$url/$key/utr/$identifier").delete()

  override def beforeEach(): Unit = dropMongoDb()

  def dropMongoDb()(implicit ec: ExecutionContext = global): Unit = Await.result(mongo().drop(), 10 seconds)

  "a getProperty entire List for refusal-to-deal-with" should {
    behave like getPropertyList("refusal-to-deal-with")
  }

  "a getProperty entire List for manually-assured" should {
    behave like getPropertyList("manually-assured")
  }

  "a createProperty endpoint for refusal-to-deal-with" should {
      behave like createPropertyTests("refusal-to-deal-with")
  }

  "a createProperty endpoint for manually-assured" should {
      behave like createPropertyTests("manually-assured")
  }

  "a identifier exists in  getProperty endpoint for refusal-to-deal-with" should {
    behave like identifierExistsTests("refusal-to-deal-with")
  }

  "a identifier exists in getProperty endpoint for manually-assured" should {
    behave like identifierExistsTests("manually-assured", false)
  }

  "a deleteIdentifierInProperty endpoint for refusal-to-deal-with" should {
    behave like deletePropertyTests("refusal-to-deal-with")
  }

  "a deleteIdentifierInProperty endpoint for manually-assured" should {
    behave like deletePropertyTests("manually-assured", false)
  }


  def createPropertyTests(key: String) = {

    "return 201 when property is not already present" in {
      val response: WSResponse = Await.result(createProperty(key, "myValue"), 10 seconds)
      response.status shouldBe CREATED
    }
    "return 409 (with appropriate reason) when property is already present" in {
      val response: WSResponse = Await.result(createProperty(key, "myValue"), 10 seconds)
      response.status shouldBe CREATED

      val response2: WSResponse = Await.result(createProperty(key, "myValue"), 10 seconds)
      response2.status shouldBe CONFLICT
      (Json.parse(response2.body) \ "message").as[String] shouldBe "Property already exists"
    }
    "return 400 (with appropriate reason) json is not well formed" in {
      val badlyFormedJson = s"""
                               |{
                               |   "value": "t
                               |}
                 """.stripMargin

      val response: WSResponse = Await.result(wsClient.url(s"$url/$key")
        .withHeaders("Content-Type" -> "application/json").post(badlyFormedJson), 10 seconds)
      response.status shouldBe BAD_REQUEST
      response.body.contains("bad request") shouldBe true

    }
    "return 400 (with appropriate reason) json does not conform to the api" in {
      val payload = Json.obj("foo" -> "ewrewr", "bar" -> "retrwt")

      val response: WSResponse = Await.result(wsClient.url(s"$url/$key").post(payload), 10 seconds)
      response.status shouldBe BAD_REQUEST
      response.body.contains("Invalid Value") shouldBe true
    }
  }

  def identifierExistsTests(key: String, isR2dw: Boolean = true) = {
    "return 200 OK when property is present" in {
      Await.result(createProperty(key, "myValue"), 10 seconds)

      val response = Await.result(isAssured(key, "myValue   "), 10 seconds)
      if(isR2dw)
        response.status shouldBe FORBIDDEN
      else response.status shouldBe OK
    }
    "return 404 when property is not present" in {
      val response = Await.result(isAssured(key, "myValue"), 10 seconds)
      if(isR2dw)
        response.status shouldBe OK
      else response.status shouldBe FORBIDDEN
    }
  }

  def getPropertyList(key: String) = {
    "return 200 OK when properties are present in first page" in {
      Await.result(createProperty(key, "myValue"), 10 seconds)
      Await.result(createProperty(key, "myValue1"), 10 seconds)
      Await.result(createProperty(key, "myValue2"), 10 seconds)
      Await.result(createProperty(key, "myValue3"), 10 seconds)

      val response = Await.result(getEntirePaginatedList(key, 1, 3), 10 seconds)
      response.status shouldBe OK
      (response.json \ "resources").as[Seq[String]] shouldBe Seq("myValue", "myValue1", "myValue2")
      (response.json \ "total").as[Int] shouldBe 4
      (response.json \ "_links" \ "self" \ "href").as[String] should include(s"/agent-assurance/$key?page=1&pageSize=3")
      (response.json \ "_links" \ "first" \ "href").as[String] should include(s"/agent-assurance/$key?page=1&pageSize=3")
      (response.json \ "_links" \ "next" \ "href").as[String] should include(s"/agent-assurance/$key?page=2&pageSize=3")
      (response.json \ "_links" \ "last" \ "href").as[String] should include(s"/agent-assurance/$key?page=2&pageSize=3")
      (response.json \ "_links" \ "previous" \ "href").toOption.isDefined shouldBe false
    }

    "return 200 OK when properties are present in second page" in {
      Await.result(createProperty(key, "myValue"), 10 seconds)
      Await.result(createProperty(key, "myValue1"), 10 seconds)
      Await.result(createProperty(key, "myValue2"), 10 seconds)
      Await.result(createProperty(key, "myValue3"), 10 seconds)
      Await.result(createProperty(key, "myValue4"), 10 seconds)

      val response = Await.result(getEntirePaginatedList(key, 2, 2), 10 seconds)
      response.status shouldBe OK
      (response.json \ "resources").as[Seq[String]] shouldBe Seq("myValue2", "myValue3")
      (response.json \ "total").as[Int] shouldBe 5
      (response.json \ "_links" \ "self" \ "href").as[String] should include(s"/agent-assurance/$key?page=2&pageSize=2")
      (response.json \ "_links" \ "first" \ "href").as[String] should include(s"/agent-assurance/$key?page=1&pageSize=2")
      (response.json \ "_links" \ "previous" \ "href").as[String] should include(s"/agent-assurance/$key?page=1&pageSize=2")
      (response.json \ "_links" \ "last" \ "href").as[String] should include(s"/agent-assurance/$key?page=3&pageSize=2")
      (response.json \ "_links" \ "next" \ "href").as[String] should include(s"/agent-assurance/$key?page=3&pageSize=2")
    }

    "return 200 OK when properties are present in last page" in {
      Await.result(createProperty(key, "myValue"), 10 seconds)
      Await.result(createProperty(key, "myValue1"), 10 seconds)
      Await.result(createProperty(key, "myValue2"), 10 seconds)
      Await.result(createProperty(key, "myValue3"), 10 seconds)

      val response = Await.result(getEntirePaginatedList(key, 2, 3), 10 seconds)
      response.status shouldBe OK
      (response.json \ "resources").as[Seq[String]] shouldBe Seq("myValue3")
      (response.json \ "total").as[Int] shouldBe 4
      (response.json \ "_links" \ "self" \ "href").as[String] should include(s"/agent-assurance/$key?page=2&pageSize=3")
      (response.json \ "_links" \ "first" \ "href").as[String] should include(s"/agent-assurance/$key?page=1&pageSize=3")
      (response.json \ "_links" \ "previous" \ "href").as[String] should include(s"/agent-assurance/$key?page=1&pageSize=3")
      (response.json \ "_links" \ "last" \ "href").as[String] should include(s"/agent-assurance/$key?page=2&pageSize=3")
      (response.json \ "_links" \ "next" \ "href").toOption.isDefined shouldBe false
    }

    "return 404 when property is not present" in {
      val response = Await.result(getEntirePaginatedList(key, 10, 3), 10 seconds)
        response.status shouldBe NO_CONTENT
    }
  }

  def deletePropertyTests(key: String, isR2dw: Boolean = true) = {
    "return 204 when property is present" in {
      Await.result(createProperty(key, "someValue"), 10 seconds)

      val response = Await.result(deleteProperty(key, "   someValue"), 10 seconds)
      response.status shouldBe NO_CONTENT

      val response2 = Await.result(isAssured(key, "someValue    "), 10 seconds)
      if(isR2dw)
        response2.status shouldBe OK
      else response2.status shouldBe FORBIDDEN
    }
    "return 404 when property is not present" in {
      val response = Await.result(deleteProperty(key,  "someValue"), 10 seconds)
      response.status shouldBe NOT_FOUND
    }
  }
}