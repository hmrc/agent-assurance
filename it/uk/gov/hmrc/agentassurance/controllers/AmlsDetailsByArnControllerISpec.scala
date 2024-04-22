package uk.gov.hmrc.agentassurance.controllers

import com.google.inject.AbstractModule
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{BodyWritable, WSClient}
import play.api.test.Helpers._
import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.agentassurance.repositories._
import uk.gov.hmrc.agentassurance.stubs.DesStubs
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
  with DesStubs {


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
        "microservice.services.des.host" -> wireMockHost,
        "microservice.services.des.port" -> wireMockPort,
        "auditing.enabled" -> false,
        "stride.roles.agent-assurance" -> "maintain_agent_manually_assure",
        "internal-auth-token-enabled-on-start" -> false
      )
      .overrides(moduleWithOverrides)


  val arn = Arn("AARN0000002")
  val url = s"http://localhost:$port/agent-assurance/amls/arn/${arn.value}"

  override def irAgentReference: String = "IRSA-123"

  val wsClient = app.injector.instanceOf[WSClient]

  def doRequest() =
    Await.result(
      wsClient.url(url)
        .withHttpHeaders("Authorization" -> "Bearer XYZ")
        .get(), 15.seconds
    )

  def doPostRequest[T](body: T)(implicit writes: BodyWritable[T]) =
    Await.result(
      wsClient.url(url)
        .withHttpHeaders("Authorization" -> "Bearer XYZ", CONTENT_TYPE -> "application/json")
        .post(body), 15.seconds
    )

  def agentDetails(countryCode: String = "GB") = AgencyDetails(
    Some("My Agency"),
    Some("abc@abc.com"),
    Some("07345678901"),
    Some(BusinessAddress(
      "25 Any Street",
      Some("Central Grange"),
      Some("Telford"),
      None,
      Some("TF4 3TR"),
      countryCode))
  )

  val testUtr: Utr = Utr("7000000002")
  val membershipExpiresOnDate: LocalDate = LocalDate.now.plusWeeks(4)
  val testAmlsDetails: UkAmlsDetails = UkAmlsDetails("supervisory", membershipNumber = Some("0123456789"), appliedOn = None, membershipExpiresOn = Some(membershipExpiresOnDate))
  val testOverseasAmlsDetails: OverseasAmlsDetails = OverseasAmlsDetails("supervisory", membershipNumber = Some("0123456789"))
  val testOverseasAmlsEntity: OverseasAmlsEntity = OverseasAmlsEntity(arn, testOverseasAmlsDetails, None)

  val testCreatedDate: LocalDate = LocalDate.now.plusWeeks(2)
  val amlsEntity: UkAmlsEntity = UkAmlsEntity(utr = Some(testUtr), amlsDetails = testAmlsDetails, arn = Some(arn), createdOn = testCreatedDate, amlsSource = AmlsSource.Subscription)

  "GET /amls/arn/:arn" should {
    s"return OK with status NoAmlsDetailsUK when no AMLS records found for the ARN" in {
      isLoggedInAsStride("stride")
      givenDESGetAgentRecord(arn, Some(testUtr))
      val response = doRequest()
      response.status mustBe OK
      response.json mustBe Json.obj("status" -> "NoAmlsDetailsUK")
    }

    s"return OK with status NoAmlsDetailsNonUK when no AMLS records found for the ARN" in {
      isLoggedInAsStride("stride")
      givenDESGetAgentRecord(arn, Some(testUtr), overseas = true)
      val response = doRequest()
      response.status mustBe OK
      response.json mustBe Json.obj("status" -> "NoAmlsDetailsNonUK")
    }

    s"return OK with status when UK AMLS records found for the ARN" in {
      isLoggedInAsStride("stride")
      ukAmlsRepository.collection.insertOne(amlsEntity).toFuture().futureValue
      val response = doRequest()
      response.status mustBe OK
      response.json mustBe Json.obj("status" -> "ValidAmlsDetailsUK", "details" -> Json.obj("supervisoryBody" -> "supervisory", "membershipNumber" -> "0123456789","membershipExpiresOn" -> membershipExpiresOnDate))
    }

    s"return OK with status when overseas AMLS details found for the ARN" in {
      isLoggedInAsStride("stride")
      overseasAmlsRepository.collection.insertOne(testOverseasAmlsEntity).toFuture().futureValue
      val response = doRequest()
      response.status mustBe OK
      response.json mustBe Json.obj("status" -> "ValidAmlsNonUK", "details" -> Json.obj("supervisoryBody" -> "supervisory", "membershipNumber" -> "0123456789"))
    }

    "return INTERNAL_SERVER_ERROR when overseas and UK AMLS records found for the ARN" in {
      isLoggedInAsStride("stride")
      overseasAmlsRepository.collection.insertOne(testOverseasAmlsEntity).toFuture().futureValue
      ukAmlsRepository.collection.insertOne(amlsEntity).toFuture().futureValue
      val response = doRequest()
      response.status mustBe INTERNAL_SERVER_ERROR
    }
  }

  "POST /amls/arn/:arn" should {
    "return CREATED for UK AMLS" when {
      "no previous record exists for the ARN" in {
        isLoggedInAsAnAfinityGroupAgent("agent1")
        givenDESGetAgentRecord(arn, Some(testUtr))

        val amlsRequest = AmlsRequest(
          ukRecord = true,
          supervisoryBody = "ACCA",
          membershipNumber = "A123",
          membershipExpiresOn = Some(LocalDate.parse("2024-12-31")))

        val response = doPostRequest(Json.toJson(amlsRequest))
        response.status mustBe CREATED

        ukAmlsRepository.collection.find().toFuture().futureValue.size mustBe 1
        archivedAmlsRepository.collection.find().toFuture().futureValue.size mustBe 0
      }

      "an existing record (including UTR) exists for the ARN, archiving the existing AMLS record" in {
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
          supervisoryBody = "ACCA",
          membershipNumber = "A123",
          membershipExpiresOn = Some(LocalDate.parse("2024-12-31")))

        val response = doPostRequest(Json.toJson(amlsRequest))
        response.status mustBe CREATED

        ukAmlsRepository.collection.find().toFuture().futureValue.size mustBe 1
        archivedAmlsRepository.collection.find().toFuture().futureValue.size mustBe 1
      }

      "an existing record (without a UTR) exists for the ARN, archiving the existing AMLS record" in {
        isLoggedInAsAnAfinityGroupAgent("agent1")
        givenDESGetAgentRecord(arn, Some(testUtr))

        val ukAmlsEntity = UkAmlsEntity(
          utr = None,
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
          supervisoryBody = "ACCA",
          membershipNumber = "A123",
          membershipExpiresOn = Some(LocalDate.parse("2024-12-31")))

        val response = doPostRequest(Json.toJson(amlsRequest))
        response.status mustBe CREATED

        ukAmlsRepository.collection.find().toFuture().futureValue.size mustBe 1
        archivedAmlsRepository.collection.find().toFuture().futureValue.size mustBe 1
      }
    }
    "return 201 Created for overseas AMLS" when {
      "no previous record exists for the ARN" in {
        isLoggedInAsAnAfinityGroupAgent("agent1")

        val amlsRequest = AmlsRequest(
          ukRecord = false,
          supervisoryBody = "Indian AC",
          membershipNumber = "X243",
          membershipExpiresOn = Some(LocalDate.parse("2024-12-31")))

        val response = doPostRequest(Json.toJson(amlsRequest))
        response.status mustBe CREATED

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
          supervisoryBody = "Indian BC",
          membershipNumber = "B343",
          membershipExpiresOn = Some(LocalDate.parse("2024-12-31")))

        val response = doPostRequest(Json.toJson(amlsRequest))
        response.status mustBe CREATED

        overseasAmlsRepository.collection.find().toFuture().futureValue.size mustBe 1
        overseasAmlsRepository.collection.find().toFuture()
          .futureValue.head mustBe OverseasAmlsEntity(arn, OverseasAmlsDetails("Indian BC", Some("B343")), Some(frozenInstant))

        archivedAmlsRepository.collection.find().toFuture().futureValue.size mustBe 1

      }

    }
    "return BAD_REQUEST" when {
      "empty body is sent" in {
        isLoggedInAsAnAfinityGroupAgent("agent1")

        val response = doPostRequest("")
        response.status mustBe BAD_REQUEST
        response.body.contains("No JSON found in request") mustBe true
      }
      "invalid JSON sent" in {
        isLoggedInAsAnAfinityGroupAgent("agent1")

        val response = doPostRequest(Json.obj("not" -> "acceptable"))
        response.status mustBe BAD_REQUEST
        response.body.contains("Could not parse JSON body:") mustBe true
      }
    }
  }
}
