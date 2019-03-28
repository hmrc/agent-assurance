package uk.gov.hmrc.agentassurance.connectors

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.kenshoo.play.metrics.Metrics
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentassurance.models.DateOfBirth
import uk.gov.hmrc.agentassurance.stubs.{CitizenDetailsStubs, DataStreamStub}
import uk.gov.hmrc.agentassurance.support.{MetricTestSupport, WireMockSupport}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext

class CitizenDetailsConnectorISpec
  extends UnitSpec with OneAppPerSuite with WireMockSupport with CitizenDetailsStubs with DataStreamStub with MetricTestSupport {

  override implicit lazy val app: Application = appBuilder.build()

  val cdConnector = new CitizenDetailsConnectorImpl(wireMockBaseUrl, app.injector.instanceOf[HttpGet], app.injector.instanceOf[Metrics])

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.citizen-details.port" -> wireMockPort,
        "auditing.consumer.baseUri.host" -> wireMockHost,
        "auditing.consumer.baseUri.port" -> wireMockPort
      )
      .bindings(bind[CitizenDetailsConnector].toInstance(cdConnector))

  private implicit val hc = HeaderCarrier()
  private implicit val ec = ExecutionContext.global


  "citizen details connector" should {
    "return a DateOfBirth when given a valid Nino" in {
      val nino = Nino("XX212121B")
      val dobString = "20121990"
      givencitizenDetailsFoundForNino(nino.value, dobString)
      await(cdConnector.getDateOfBirth(nino)) shouldBe Some(DateOfBirth(LocalDate.parse(dobString, DateTimeFormatter.ofPattern("ddMMyyyy"))))
    }

    "return None when the Nino is invalid" in {
      val nino = Nino("XX212121B")
      givenCitizenDetailsNotFoundForNino(nino.value)
      await(cdConnector.getDateOfBirth(nino)) shouldBe None
    }

    "return None when there is a network failure" in {
      val nino = Nino("XX212121B")
      givenCitizenDetailsNetworkError(nino.value)
      await(cdConnector.getDateOfBirth(nino)) shouldBe None
    }
  }

}
