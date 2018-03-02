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
import uk.gov.hmrc.agentassurance.model._

class PropertiesControllerISpec extends UnitSpec
  with GuiceOneServerPerSuite with BeforeAndAfterEach with MongoSpecSupport {

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}")

  override implicit lazy val app: Application = appBuilder.build()

  val url = s"http://localhost:$port/agent-assurance"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]


  def updateProperty(key: String, value: String) = wsClient.url(s"$url/properties/$key").put(Json.obj("value" -> value))

  def createProperty(key: String, value: String) =
    wsClient.url(s"$url/properties").post(Json.obj("key" -> key, "value" -> value))

  def getProperty(key: String): Future[WSResponse] = wsClient.url(s"$url/properties/$key").get()

  def deleteProperty(key: String): Future[WSResponse] = wsClient.url(s"$url/properties/$key").delete()

  override def beforeEach(): Unit = dropMongoDb()

  def dropMongoDb()(implicit ec: ExecutionContext = global): Unit = Await.result(mongo().drop(), 10 seconds)

  "createProperty " should {
    "return 201 when property is not already present" in {
      val response: WSResponse = Await.result(createProperty("myKey", "myValue"), 10 seconds)
      response.status shouldBe CREATED
    }
    "return 400 (with appropriate reason) when property is already present" in {
      val response: WSResponse = Await.result(createProperty("myKey", "myValue"), 10 seconds)
      response.status shouldBe CREATED

      val response2: WSResponse = Await.result(createProperty("myKey", "myValue"), 10 seconds)
      response2.status shouldBe BAD_REQUEST
      (Json.parse(response2.body) \ "message").as[String] shouldBe "Property already exists"
    }
    "return 400 (with appropriate reason) json is not well formed" in {
      val badlyFormedJson = s"""
                               |{
                               |   "key": "NINO"
                               |   "value": "t
                               |}
                 """.stripMargin

      val response: WSResponse = Await.result(wsClient.url(s"$url/properties")
        .withHeaders("Content-Type" -> "application/json").post(badlyFormedJson), 10 seconds)
      response.status shouldBe BAD_REQUEST
      response.body.contains("bad request") shouldBe true

    }
    "return 400 (with appropriate reason) json does not conform to the api" in {
      val payload = Json.obj("foo" -> "ewrewr", "bar" -> "retrwt")

      val response: WSResponse = Await.result(wsClient.url(s"$url/properties").post(payload), 10 seconds)
      response.status shouldBe BAD_REQUEST
      response.body.contains("Invalid Property") shouldBe true
    }
  }

  "getProperty" should {
    "return 200 (along with property) when property is present" in {
      Await.result(createProperty("myKey", "myValue"), 10 seconds)

      val response = Await.result(getProperty("myKey"), 10 seconds)

      response.status shouldBe OK

      val jsonResponse = Json.parse(response.body).as[Property]
      jsonResponse.key shouldBe "myKey"
      jsonResponse.value shouldBe "myValue"
    }
    "return 404 when property is not present" in {
      val response = Await.result(getProperty("myProperty"), 10 seconds)
      response.status shouldBe NOT_FOUND
    }
  }

  "deleteProperty " should {
    "return 204 when property is present" in {
      Await.result(createProperty("myKey", "myValue"), 10 seconds)

      val response = Await.result(deleteProperty("myKey"), 10 seconds)
      response.status shouldBe NO_CONTENT

      val response2 = Await.result(getProperty("myKey"), 10 seconds)
      response2.status shouldBe NOT_FOUND
    }
    "return 404 when property is not present" in {
      val response = Await.result(deleteProperty("myProperty"), 10 seconds)
      response.status shouldBe NOT_FOUND
    }
  }

  "updateProperty" should {
    "return 204 when property is correctly updated" in {
      val response: WSResponse = Await.result(createProperty("myKey", "myValue"), 10 seconds)
      response.status shouldBe CREATED

      val response2: WSResponse = Await.result(updateProperty("myKey", "newValue"), 10 seconds)
      response2.status shouldBe NO_CONTENT

      val response3: WSResponse = Await.result(getProperty("myKey"), 10 seconds)
      response3.status shouldBe OK
      val jsonResponse = Json.parse(response3.body).as[Property]
      jsonResponse.value shouldBe "newValue"
    }
    "return 404 when updating a property that is not present" in {
      val response = Await.result(updateProperty("notPresentKey", "newValue"), 10 seconds)
      response.status shouldBe NOT_FOUND
    }
  }

}
