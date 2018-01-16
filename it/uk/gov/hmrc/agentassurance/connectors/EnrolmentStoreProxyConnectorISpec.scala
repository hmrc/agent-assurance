package uk.gov.hmrc.agentassurance.connectors

import com.kenshoo.play.metrics.Metrics
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentassurance.stubs.EnrolmentStoreProxyStubs
import uk.gov.hmrc.agentassurance.support.{MetricTestSupport, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext

class EnrolmentStoreProxyConnectorISpec extends UnitSpec with OneAppPerSuite with WireMockSupport with MetricTestSupport with EnrolmentStoreProxyStubs {
  override implicit lazy val app: Application = appBuilder.build()

  val connector =
    new EnrolmentStoreProxyConnector(wireMockBaseUrl,app.injector.instanceOf[HttpGet], app.injector.instanceOf[Metrics])

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure("microservice.services.enrolment-store-proxy.port" -> wireMockPort)
      .bindings(bind[EnrolmentStoreProxyConnector].toInstance(connector))

  private implicit val hc = HeaderCarrier()
  private implicit val ec = ExecutionContext.global

  private val service = "IR-PAYE"
  private val userId = "0000001531072644"

  "EnrolmentStoreProxyConnector getClientCount" should {

    "return an empty list when Emac returns 204" in {
      noClientsAreAllocated(service, userId, 204)
      await(connector.getClientCount(service, userId)) shouldBe 0
    }

    "throw an exception when Emac returns an unexpected http response code" in {
      noClientsAreAllocated(service, userId, 404)
      an[Exception] should be thrownBy await(connector.getClientCount(service, userId))
    }

    "return an all clients returned by Emac" in {
      givenCleanMetricRegistry()
      sufficientClientsAreAllocated(service, userId)
      ( await(connector.getClientCount(service, userId)) > 0 ) shouldBe true
      timerShouldExistsAndBeenUpdated("ConsumedAPI-ESP-ES2-GetAgentClientList-IR-PAYE-GET")
      histogramShouldExistsAndBeenUpdated("Size-ESP-ES2-GetAgentClientList-IR-PAYE",6)
    }
  }

}
