package uk.gov.hmrc.agentassurance.controllers

import com.google.inject.AbstractModule
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{BodyWritable, WSClient}
import play.api.test.Helpers.CONTENT_TYPE
import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.agentassurance.repositories._
import uk.gov.hmrc.agentassurance.support.{AgentAuthStubs, InstantClockTestSupport, WireMockSupport}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

import java.time.LocalDate
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class AmlsDetailsByArnControllerISpec extends PlaySpec
  with AgentAuthStubs
  with GuiceOneServerPerSuite
  with WireMockSupport
  with CleanMongoCollectionSupport
  with InstantClockTestSupport
  {


  override implicit lazy val app: Application = appBuilder.build()

  protected val ukAmlsRepository: PlayMongoRepository[UkAmlsEntity] =
    new AmlsRepositoryImpl(mongoComponent)

  protected val overseasAmlsRepository: PlayMongoRepository[OverseasAmlsEntity] =
    new OverseasAmlsRepositoryImpl(mongoComponent)

  protected val archivedAmlsRepository: PlayMongoRepository[ArchivedAmlsEntity] =
    new ArchivedAmlsRepositoryImpl(mongoComponent)


  val moduleWithOverrides: AbstractModule = new AbstractModule() {
    override def configure(): Unit = {
      bind(classOf[OverseasAmlsRepository]).toInstance(overseasAmlsRepository.asInstanceOf[OverseasAmlsRepositoryImpl])
      bind(classOf[AmlsRepository]).toInstance(ukAmlsRepository.asInstanceOf[AmlsRepositoryImpl])
      bind(classOf[ArchivedAmlsRepository]).toInstance(archivedAmlsRepository.asInstanceOf[ArchivedAmlsRepositoryImpl])
    }
  }

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure("microservice.services.auth.host" -> wireMockHost,
        "microservice.services.auth.port" -> wireMockPort,
        "auditing.enabled" -> false,
        "stride.roles.agent-assurance" -> "maintain_agent_manually_assure")
      .overrides(moduleWithOverrides)


  val arn = Arn("AARN0000002")
  val url = s"http://localhost:$port/agent-assurance/amls/arn/${arn.value}"
  override def irAgentReference: String = "IRSA-123"

  val wsClient = app.injector.instanceOf[WSClient]
  def doRequest() =
    Await.result(
      wsClient.url(url)
        .withHttpHeaders( "Authorization" -> "Bearer XYZ")
        .get(), 15.seconds
    )

  def doPostRequest[T](body: T)(implicit writes: BodyWritable[T]) =
    Await.result(
      wsClient.url(url)
        .withHttpHeaders("Authorization" -> "Bearer XYZ", CONTENT_TYPE -> "application/json")
        .post(body), 15.seconds
    )


  val testUtr: Utr = Utr("7000000002")
  val membershipExpiresOnDate: LocalDate = LocalDate.parse("2024-01-12")
  val testAmlsDetails: UkAmlsDetails = UkAmlsDetails("supervisory", membershipNumber = Some("0123456789"), appliedOn = None, membershipExpiresOn = Some(membershipExpiresOnDate))
  val testOverseasAmlsDetails: OverseasAmlsDetails = OverseasAmlsDetails("supervisory", membershipNumber = Some("0123456789"))
  val testOverseasAmlsEntity: OverseasAmlsEntity = OverseasAmlsEntity(arn,testOverseasAmlsDetails, None)

  val testCreatedDate: LocalDate = LocalDate.parse("2024-01-15")
  val amlsEntity: UkAmlsEntity = UkAmlsEntity(utr = Some(testUtr), amlsDetails = testAmlsDetails, arn = Some(arn),createdOn = testCreatedDate, amlsSource = AmlsSource.Subscription)


  "GET /amls/arn/:arn" should {
    "return 204 when no AMLS records found for the ARN" in {
      isLoggedInAsStride("stride")
      val response = doRequest()
      response.status mustBe 204
    }
    "return 200 when UK AMLS records found for the ARN" in {
      isLoggedInAsStride("stride")
      ukAmlsRepository.collection.insertOne(amlsEntity).toFuture().futureValue
      val response = doRequest()
      response.status mustBe 200
      response.body[String] mustBe """{"supervisoryBody":"supervisory","membershipNumber":"0123456789","membershipExpiresOn":"2024-01-12"}"""
    }
    "return 200 when overseas AMLS records found for the ARN" in {
      isLoggedInAsStride("stride")
      overseasAmlsRepository.collection.insertOne(testOverseasAmlsEntity).toFuture().futureValue
      val response = doRequest()
      response.status mustBe 200
      response.body[String] mustBe """{"supervisoryBody":"supervisory","membershipNumber":"0123456789"}"""
    }
    "return 500 when overseas and UK AMLS records found for the ARN" in {
      isLoggedInAsStride("stride")
      overseasAmlsRepository.collection.insertOne(testOverseasAmlsEntity).toFuture().futureValue
      ukAmlsRepository.collection.insertOne(amlsEntity).toFuture().futureValue
      val response = doRequest()
      response.status mustBe 500
    }
  }

  "POST /amls/arn/:arn" should {
    "return 201 Created for UK AMLS" when {
      "no previous record exists for the ARN" in {
        isLoggedInAsAnAfinityGroupAgent("agent1")

        val amlsRequest = AmlsRequest(
          ukRecord = true,
          utr = Some(testUtr),
          supervisoryBody = "ACCA",
          membershipNumber = "A123",
          membershipExpiresOn = Some(LocalDate.parse("2024-12-31")))

        val response = doPostRequest(Json.toJson(amlsRequest))
        response.status mustBe 201

        ukAmlsRepository.collection.find().toFuture().futureValue.size mustBe 1
        archivedAmlsRepository.collection.find().toFuture().futureValue.size mustBe 0
      }

      "an existing record exists for the ARN, archiving the existing AMLS record" in {
        isLoggedInAsAnAfinityGroupAgent("agent1")

        val ukAmlsEntity = UkAmlsEntity(
          utr = Some(Utr("1234567890")),
          amlsDetails = UkAmlsDetails(
            supervisoryBody = "ICAEW",
            membershipNumber = Some("123"),
            amlsSafeId = None,
            agentBPRSafeId = None,
            appliedOn = None,
            membershipExpiresOn = Some(LocalDate.parse("2019-10-10"))),
          arn = Some(arn),
          createdOn = LocalDate.parse("2020-10-10"),
          amlsSource = AmlsSource.Subscription)

        ukAmlsRepository.collection.insertOne(ukAmlsEntity).toFuture().futureValue

        val amlsRequest = AmlsRequest(
          ukRecord = true,
          utr = Some(testUtr),
          supervisoryBody = "ACCA",
          membershipNumber = "A123",
          membershipExpiresOn = Some(LocalDate.parse("2024-12-31")))

        val response = doPostRequest(Json.toJson(amlsRequest))
        response.status mustBe 201

        ukAmlsRepository.collection.find().toFuture().futureValue.size mustBe 1
        archivedAmlsRepository.collection.find().toFuture().futureValue.size mustBe 1
      }
    }
    "return 201 Created for overseas AMLS" when {
      "no previous record exists for the ARN" in {
        isLoggedInAsAnAfinityGroupAgent("agent1")

        val amlsRequest = AmlsRequest(
          ukRecord = false,
          utr = None,
          supervisoryBody = "Indian AC",
          membershipNumber = "X243",
          membershipExpiresOn = Some(LocalDate.parse("2024-12-31")))

        val response = doPostRequest(Json.toJson(amlsRequest))
        response.status mustBe 201

        overseasAmlsRepository.collection.find().toFuture().futureValue.size mustBe 1
        archivedAmlsRepository.collection.find().toFuture().futureValue.size mustBe 0

      }
      "an existing record exists for the ARN, archiving the existing AMLS record" in {
        isLoggedInAsAnAfinityGroupAgent("agent1")

        val overseasAmlsEntity = OverseasAmlsEntity(
          amlsDetails = OverseasAmlsDetails(
            supervisoryBody = "Indian ACA",
            membershipNumber = Some("CC123")),
          arn = arn,
          createdDate = None
        )

        overseasAmlsRepository.collection.insertOne(overseasAmlsEntity).toFuture().futureValue

        val amlsRequest = AmlsRequest(
          ukRecord = false,
          utr = None,
          supervisoryBody = "Indian BC",
          membershipNumber = "B343",
          membershipExpiresOn = Some(LocalDate.parse("2024-12-31")))

        val response = doPostRequest(Json.toJson(amlsRequest))
        response.status mustBe 201

        overseasAmlsRepository.collection.find().toFuture().futureValue.size mustBe 1
        overseasAmlsRepository.collection.find().toFuture()
          .futureValue.head mustBe OverseasAmlsEntity(arn,OverseasAmlsDetails("Indian BC", Some("B343")), Some(frozenInstant))

        archivedAmlsRepository.collection.find().toFuture().futureValue.size mustBe 1

      }

    }
    "return 400 BadRequest" when {
      "empty body is sent" in {
        isLoggedInAsAnAfinityGroupAgent("agent1")

        val response = doPostRequest("")
        response.status mustBe 400
        response.body.contains("No JSON found in request") mustBe true
      }
      "invalid JSON sent" in {
        isLoggedInAsAnAfinityGroupAgent("agent1")

        val response = doPostRequest(Json.parse("""{"not": "acceptable"}"""))
        response.status mustBe 400
        response.body.contains("Could not parse body:") mustBe true
      }
    }
  }
}
