package uk.gov.hmrc.agentassurance.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import com.kenshoo.play.metrics.Metrics
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentassurance.support.{MetricTestSupport, WireMockSupport}
import uk.gov.hmrc.domain.AgentCode
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import wiring.WSVerbs

import scala.concurrent.ExecutionContext

case class PayeClient(govGwClientName: String, empRef: String, agentCanViewLandP: Boolean = true)

class GovernmentGatewayConnectorISpec
  extends UnitSpec with OneAppPerSuite with WireMockSupport with MetricTestSupport {
  override implicit lazy val app: Application = appBuilder.build()

  val connector =
    new GovernmentGatewayConnector(wireMockBaseUrl, new WSVerbs()(app.configuration), app.injector.instanceOf[Metrics])

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure("microservice.services.government-gateway.port" -> wireMockPort)
      .bindings(bind[GovernmentGatewayConnector].toInstance(connector))

  private implicit val hc = HeaderCarrier()
  private implicit val ec = ExecutionContext.global

  private val agentCode1 = AgentCode("foo")
  private val clientListUrl = s"/agent/${agentCode1.value}/client-list/IR-PAYE/all"

  "GovernmentGatewayConnector getPayeClientCount" should {
    "return an empty list when GG returns 202" in {
      stubSimpleGGResponse(202)
      await(connector.getPayeClientCount(agentCode1)) shouldBe 0
    }

    "return an empty list when GG returns 204" in {
      stubSimpleGGResponse(204)
      await(connector.getPayeClientCount(agentCode1)) shouldBe 0
    }

    "throw an exception when GG returns an unexpected http response code" in {
      stubSimpleGGResponse(404)
      an[Exception] should be thrownBy await(connector.getPayeClientCount(agentCode1))
    }

    "return an all clients returned by GG" in {
      stubClientsFoundGGResponse
      await(connector.getPayeClientCount(agentCode1)) shouldBe 2
    }
  }

  private def stubSimpleGGResponse(responseCode: Int) = {
    stubFor(get(urlPathEqualTo(clientListUrl)).willReturn( aResponse().withStatus(responseCode)))
  }

  private def stubClientsFoundGGResponse = {
    val responseBody =
      """[{"friendlyName" : "alice", "empRef": "123/XYZ"},{"friendlyName": "bob", "empRef": "123/ABC"}]"""
    stubFor(get(urlPathEqualTo(clientListUrl)).willReturn(
      aResponse().withStatus(200).withHeader("Content-Type", "application/json; charset=utf-8").withBody(responseBody)))
  }
}
