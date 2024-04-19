package uk.gov.hmrc.agentassurance.connectors

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.github.tomakehurst.wiremock.client.WireMock._
import com.typesafe.config.Config
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import play.api.test.Helpers._
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.stubs.{DataStreamStub, DesStubs}
import uk.gov.hmrc.agentassurance.support.{MetricTestSupport, UnitSpec, WireMockSupport}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import java.time.{LocalDateTime, ZoneId}
import scala.concurrent.ExecutionContext

class DmsConnectorISpec extends UnitSpec with GuiceOneAppPerSuite with WireMockSupport with DesStubs with DataStreamStub with MetricTestSupport {

  override implicit lazy val app: Application = appBuilder
    .build()

  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val config: Config = app.injector.instanceOf[Config]
  implicit private lazy val as: ActorSystem = ActorSystem()
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val ec: ExecutionContext = ExecutionContext.global


  val dmsConnector = new DmsConnector(app.injector.instanceOf[HttpClientV2], appConfig, config, as)

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "internal-auth-token-enabled-on-start" -> false,
        "microservice.services.internal-auth.host" -> wireMockHost,
        "microservice.services.internal-auth.port" -> wireMockPort,
        "microservice.services.dms-submission.port" -> wireMockPort,
        "microservice.services.dms-submission.host" -> wireMockHost,
        "microservice.services.dms-submission.contact-details-submission.callbackEndpoint" -> "http://localhost/callback",
        "microservice.services.dms-submission.contact-details-submission.classificationType" -> "classificationType",
        "microservice.services.dms-submission.contact-details-submission.source" -> "source",
        "microservice.services.dms-submission.contact-details-submission.formId" -> "formId",
        "microservice.services.dms-submission.contact-details-submission.customerId" -> "customerId",
        "microservice.services.dms-submission.contact-details-submission.businessArea" -> "businessArea",
        "internal-auth.token" -> "authKey",
        "http-verbs.retries.intervals" -> List("1ms")
      )
      .bindings(bind[DmsConnector].toInstance(dmsConnector))


  "DmsConnector sendPdf" should {

//    val submissionReference = "submissionReference"

    val timestamp = LocalDateTime
      .of(2022, 3, 2, 12, 30, 45)
      .atZone(ZoneId.of("UTC"))
      .toInstant


    val sourcePart: Source[ByteString, NotUsed] = Source.single(ByteString.fromString("SomePdfBytes"))
    val source: Source[MultipartFormData.Part[Source[ByteString, NotUsed]] with Serializable, NotUsed] = Source(
      Seq(
        DataPart("callbackUrl", "http://localhost/callback"),
        MultipartFormData.DataPart("submissionReference", "submissionReference"),
        DataPart("metadata.source", "source"),
        DataPart("metadata.timeOfReceipt", timestamp.toString),
        DataPart("metadata.formId", "formId"),
        DataPart("metadata.customerId", "customerId"),
        DataPart("metadata.classificationType", "classificationType"),
        DataPart("metadata.businessArea", "businessArea"),
        FilePart(
          key = "form",
          filename = "form.pdf",
          contentType = Some("application/pdf"),
          ref = sourcePart
        )
      )
    )

    "must return Done when the server returns ACCEPTED" in {

      stubFor(
        post(urlEqualTo("/dms-submission/submit"))
          .withHeader(AUTHORIZATION, equalTo("authKey"))
          .withHeader(USER_AGENT, equalTo("agent-assurance"))
          .withMultipartRequestBody(
            aMultipart().withName("submissionReference").withBody(equalTo("submissionReference"))
          )
          .withMultipartRequestBody(aMultipart().withName("callbackUrl").withBody(equalTo("http://localhost/callback")))
          .withMultipartRequestBody(aMultipart().withName("metadata.source").withBody(equalTo("source")))
          .withMultipartRequestBody(
            aMultipart().withName("metadata.timeOfReceipt").withBody(equalTo("2022-03-02T12:30:45Z"))
          )
          .withMultipartRequestBody(aMultipart().withName("metadata.formId").withBody(equalTo("formId")))
          .withMultipartRequestBody(aMultipart().withName("metadata.customerId").withBody(equalTo("customerId")))
          .withMultipartRequestBody(aMultipart().withName("metadata.classificationType").withBody(equalTo("classificationType")))
          .withMultipartRequestBody(aMultipart().withName("metadata.businessArea").withBody(equalTo("businessArea")))
          .withMultipartRequestBody(
            aMultipart()
              .withName("form")
              .withBody(equalTo("SomePdfBytes"))
              .withHeader("Content-Disposition", containing("""filename="form.pdf""""))
              .withHeader("Content-Type", equalTo("application/pdf"))
          )
          .willReturn(
            aResponse()
              .withStatus(ACCEPTED)
              .withBody(Json.stringify(Json.obj("id" -> "foobar")))
          )
      )

      dmsConnector.sendPdf(source)(hc).futureValue
    }

    "must fail when the server returns another status" in {

     stubFor(
        post(urlEqualTo("/dms-submission/submit"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )
        dmsConnector.sendPdf(source)(hc).failed.futureValue
    }
  }
}


