package uk.gov.hmrc.agentkyc.controllers

import controllers.HelloWorldController
import play.api.test.Helpers.status
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

class HelloWorldControllerISpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite {

  val mockHelloWorldController = new HelloWorldController()

  implicit val hc = new HeaderCarrier

  "HelloWorldController" should {
    "return Status: OK Body: empty" in {
      val response = mockHelloWorldController.helloWorld()(FakeRequest("GET", "/hello-world"))

      status(response) mustBe OK
    }
  }
}
