/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

class HelloWorldControllerSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite {

  val mockHelloWorldController = new HelloWorldController()

  implicit val hc = new HeaderCarrier

  "HelloWorldController" should {
    "return Status: OK Body: empty" in {
      val response = mockHelloWorldController.helloWorld()(FakeRequest("GET", "/hello-world"))

      status(response) mustBe OK
    }
  }
}
