package uk.gov.hmrc.agentassurance.controllers

import com.google.inject.AbstractModule
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentassurance.models.{AmlsEntity, AmlsSources, OverseasAmlsDetails, OverseasAmlsEntity, UkAmlsDetails}
import uk.gov.hmrc.agentassurance.repositories.{AmlsRepository, AmlsRepositoryImpl, OverseasAmlsRepository, OverseasAmlsRepositoryImpl}
import uk.gov.hmrc.agentassurance.support.{AgentAuthStubs, WireMockSupport}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

import java.time.LocalDate
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class GetAmlsDetailsByArnControllerISpec extends PlaySpec
  with AgentAuthStubs
  with GuiceOneServerPerSuite
  with WireMockSupport
  with CleanMongoCollectionSupport {


  override implicit lazy val app: Application = appBuilder.build()

  protected val ukAmlsRepository: PlayMongoRepository[AmlsEntity] =
    new AmlsRepositoryImpl(mongoComponent)

  protected val overseasAmlsRepository: PlayMongoRepository[OverseasAmlsEntity] =
    new OverseasAmlsRepositoryImpl(mongoComponent)


  val moduleWithOverrides: AbstractModule = new AbstractModule() {
    override def configure(): Unit = {
      bind(classOf[OverseasAmlsRepository]).toInstance(overseasAmlsRepository.asInstanceOf[OverseasAmlsRepositoryImpl])
      bind(classOf[AmlsRepository]).toInstance(ukAmlsRepository.asInstanceOf[AmlsRepositoryImpl])
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
        .get(), 10.seconds
    )


  val testUtr: Utr = Utr("7000000002")
  val membershipExpiresOnDate: LocalDate = LocalDate.parse("2024-01-12")
  val testAmlsDetails: UkAmlsDetails = UkAmlsDetails("supervisory", membershipNumber = Some("0123456789"), appliedOn = None, membershipExpiresOn = Some(membershipExpiresOnDate))
  val testOverseasAmlsDetails: OverseasAmlsDetails = OverseasAmlsDetails("supervisory", membershipNumber = Some("0123456789"))
  val testOverseasAmlsEntity: OverseasAmlsEntity = OverseasAmlsEntity(arn,testOverseasAmlsDetails, None, None)

  val testCreatedDate: LocalDate = LocalDate.parse("2024-01-15")
  val amlsEntity: AmlsEntity = AmlsEntity(utr = Some(testUtr), amlsDetails = testAmlsDetails, arn = Some(arn),createdOn = testCreatedDate, amlsSource = AmlsSources.Subscription)


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

}
