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

  def updateProperty(key: String, value: String) = wsClient.url(s"$url/$key").put(Json.obj("value" -> value))

  def createProperty(key: String, value: String) =
    wsClient.url(s"$url/$key").post(Json.obj("value" -> value))

  def isAssured(key: String, identifier: String): Future[WSResponse] = wsClient.url(s"$url/$key/utr/$identifier").get()

  def getEntireList(key: String): Future[WSResponse] = {wsClient.url(s"$url/$key").get()}

  def getUtrsPagination(key: String, pageSize: Int, pageNumber:Int): Future[WSResponse] = {wsClient.url(s"$url/$key/pageSize/$pageSize/page/$pageNumber").get()}

  def deleteEntireProperty(key: String): Future[WSResponse] = {
    wsClient.url(s"$url/$key").delete()
  }

  def deleteIdentifierInProperty(key: String, identifier: String): Future[WSResponse] = wsClient.url(s"$url/$key/utr/$identifier").delete()

  override def beforeEach(): Unit = dropMongoDb()

  def dropMongoDb()(implicit ec: ExecutionContext = global): Unit = Await.result(mongo().drop(), 10 seconds)

  "a getProperty entire List for refusal-to-deal-with" should {
    behave like getPropertyList("refusal-to-deal-with")
  }

  "a getProperty entire List for manually-assured" should {
    behave like getPropertyList("manually-assured")
  }

  "a identifier exists in  getProperty endpoint for refusal-to-deal-with" should {
    behave like identifierExistsTests("refusal-to-deal-with")
  }

  "a identifier exists in getProperty endpoint for manually-assured" should {
    behave like identifierExistsTests("manually-assured", false)
  }

  "a deleteIdentifierInProperty endpoint for refusal-to-deal-with" should {
    behave like deleteIdentifierInPropertyTests("refusal-to-deal-with")
  }

  "a deleteIdentifierInProperty endpoint for manually-assured" should {
    behave like deleteIdentifierInPropertyTests("manually-assured", false)
  }

  "a updateProperty endpoint for refusal-to-deal-with" should {
    behave like updatePropertyTests("refusal-to-deal-with")
  }

  "a updateProperty endpoint for manually-assured" should {
    behave like updatePropertyTests("manually-assured", false)
  }

  "a checkPagination endpoint for manually-assured" should {
    behave like checkPagination("manually-assured", false)
  }

  "a checkPagination endpoint for refusal-to-deal-with" should {
    behave like checkPagination("refusal-to-deal-with")
  }

  def checkPagination(key: String, isR2dw: Boolean = true) = {
    "skips first 1 UTR" in {
      Await.result(createProperty(key, "myValue"), 10 seconds)
      Await.result(createProperty(key, "myValue1"), 10 seconds)
      Await.result(createProperty(key, "myValue2"), 10 seconds)
      Await.result(createProperty(key, "myValue3"), 10 seconds)

      val response = Await.result(getUtrsPagination(key, 2, 1), 10 seconds)
        response.body should contain("myValue,myValue1")
    }

    "NotFound => Check utr found for assurance collection" in {
      val response = Await.result(isAssured(key, "myValue"), 10 seconds)
      if(isR2dw)
        response.status shouldBe OK
      else response.status shouldBe FORBIDDEN
    }
  }

  def identifierExistsTests(key: String, isR2dw: Boolean = true) = {
    "Found => Check utr found for assurance collection" in {
      Await.result(createProperty(key, "myValue"), 10 seconds)

      val response = Await.result(isAssured(key, "myValue   "), 10 seconds)
      if(isR2dw)
        response.status shouldBe FORBIDDEN
      else response.status shouldBe OK
    }

    "NotFound => Check utr found for assurance collection" in {
      val response = Await.result(isAssured(key, "myValue"), 10 seconds)
      if(isR2dw)
        response.status shouldBe OK
      else response.status shouldBe FORBIDDEN
    }
  }


  def getPropertyList(key: String) = {
    "return 200 OK when found utrs is present" in {
      Await.result(createProperty(key, "myValue,    value2,value3,value4"), 10 seconds)

      val response = Await.result(getEntireList(key), 10 seconds)
        response.status shouldBe OK
        response.body shouldBe "myValue,value2,value3,value4"
    }
    "return No Content when no utrs found for key" in {
      val response = Await.result(getEntireList(key), 10 seconds)
        response.status shouldBe NO_CONTENT
    }
  }

  def deleteIdentifierInPropertyTests(key: String, isR2dw: Boolean = true) = {
    "return 204 when property is present" in {
      Await.result(createProperty(key, "someValue  "), 10 seconds)

      val response = Await.result(deleteIdentifierInProperty(key, "   someValue"), 10 seconds)
      response.status shouldBe NO_CONTENT

      val response2 = Await.result(isAssured(key, "someValue    "), 10 seconds)
      if(isR2dw)
        response2.status shouldBe OK
      else response2.status shouldBe FORBIDDEN
    }
  }

  //TODO apply validation for single valid UTRS?
  def updatePropertyTests(key: String, isR2dw: Boolean = true) = {
    "204 whenupdated with new utr" in {
      val response: WSResponse = Await.result(createProperty(key, "oneValue   "), 10 seconds)
      response.status shouldBe NO_CONTENT

      val response2: WSResponse = Await.result(updateProperty(key, "addedValue      "), 10 seconds)
      response2.status shouldBe NO_CONTENT
    }
  }
}