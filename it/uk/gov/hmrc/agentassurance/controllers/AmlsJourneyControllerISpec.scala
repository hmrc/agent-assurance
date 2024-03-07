package uk.gov.hmrc.agentassurance.controllers

import com.google.inject.AbstractModule
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, NO_CONTENT, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{BodyWritable, WSClient}
import play.api.test.Helpers.CONTENT_TYPE
import uk.gov.hmrc.agentassurance.models.AmlsJourney
import uk.gov.hmrc.agentassurance.repositories.AmlsJourneyRepository
import uk.gov.hmrc.agentassurance.services.{AmlsJourneyService, AmlsJourneyServiceImpl}
import uk.gov.hmrc.agentassurance.support.{AgentAuthStubs, InstantClockTestSupport, WireMockSupport}
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

import java.time.{Clock, Instant, LocalDate}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class AmlsJourneyControllerISpec extends PlaySpec
  with AgentAuthStubs
  with GuiceOneServerPerSuite
  with WireMockSupport
  with CleanMongoCollectionSupport with InstantClockTestSupport {

  override implicit lazy val app: Application = appBuilder.build()

  protected val repository = new AmlsJourneyRepository(mongoComponent)

  protected val service = new AmlsJourneyServiceImpl(repository)

  val moduleWithOverrides: AbstractModule = new AbstractModule() {
    override def configure(): Unit = {
      bind(classOf[AmlsJourneyService]).toInstance(service)
      bind(classOf[Clock]).toInstance(clock)
    }
  }

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure("microservice.services.auth.host" -> wireMockHost,
        "microservice.services.auth.port" -> wireMockPort,
        "auditing.enabled" -> false,
      )
      .overrides(moduleWithOverrides)


    val wsClient = app.injector.instanceOf[WSClient]

    val url = s"http://localhost:$port/agent-assurance/amls-journey"

  val now = Instant.now

    def doGetRequest() =
      Await.result(
        wsClient.url(url)
          .withHttpHeaders( "Authorization" -> "Bearer XYZ", "X-Session-ID" -> "session-1234")
          .get(), 15.seconds
      )

    def doPutRequest(body: JsValue)(implicit writes: BodyWritable[JsValue]) =
      Await.result(
        wsClient.url(url)
          .withHttpHeaders("Authorization" -> "Bearer XYZ", CONTENT_TYPE -> "application/json", "X-Session-ID" -> "session-123")
          .put(body), 15.seconds
      )

  def doDeleteRequest() =
    Await.result(
    wsClient.url(url)
      .withHttpHeaders( "Authorization" -> "Bearer XYZ", "X-Session-ID" -> "session-1234")
      .delete(), 15.seconds
  )

  val amlsJourney = AmlsJourney(
    status = "NoAMLSDetailsUK",
    utr = Some(Utr("1234567890")),
    newAmlsBody = Some("ICAEW"),
    newMembershipNumber = Some("XX123"),
    newExpirationDate = Some(LocalDate.parse("2024-12-12")))

    "putAmlsJourneyRecord" should {
      "return ACCEPTED and save an amlsJourney" in {
        isLoggedInAsAnAfinityGroupAgent("agent1")
        val response = doPutRequest(Json.toJson(amlsJourney))

        response.status mustBe ACCEPTED
      }

      "return BAD_REQUEST when invalid data sent" in {
        isLoggedInAsAnAfinityGroupAgent("agent1")
        val response = doPutRequest(Json.parse("""{"notValid": "1234"}"""))

        response.status mustBe BAD_REQUEST
      }
    }

  "getAmlsJourneyRecord" should {
    "return NO_CONTENT when no data exists" in {
      isLoggedInAsAnAfinityGroupAgent("agent1")
      val response = doGetRequest()

      response.status mustBe NO_CONTENT
    }

    "return OK when a journey exists" in {
      isLoggedInAsAnAfinityGroupAgent("agent1")
      repository.put("session-1234")(DataKey("amlsJourney"), amlsJourney).map(_=>()).futureValue

      val response = doGetRequest()

      response.status mustBe OK
      response.json mustBe Json.toJson(amlsJourney)
    }
  }

  "deleteAmlsJourneyRecord" should {
    "return NO_CONTENT when no record exists" in {
      isLoggedInAsAnAfinityGroupAgent("agent1")

      val response = doDeleteRequest()

      response.status mustBe NO_CONTENT
    }

    "return NO_CONTENT and delete an existing record" in {
      isLoggedInAsAnAfinityGroupAgent("agent1")
      repository.put("session-1234")(DataKey("amlsJourney"), amlsJourney).map(_=>()).futureValue

      val response = doDeleteRequest()

      response.status mustBe NO_CONTENT

      val check = repository.get("session-1234")(DataKey[AmlsJourney]("amlsJourney")).futureValue
      check mustBe None
    }
  }
}
