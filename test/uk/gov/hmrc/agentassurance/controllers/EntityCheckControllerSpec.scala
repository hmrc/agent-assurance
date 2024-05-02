/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentassurance.controllers

import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.scalatestplus.play.PlaySpec
import play.api.test._
import play.api.test.Helpers._

class EntityCheckControllerSpec extends PlaySpec with DefaultAwaitTimeout with GuiceOneAppPerTest with Injecting {

  "agentVerifyEntity" should {
    "return OK" when {
      "a GET request to /agent/verify-entity" in {
        val controller = inject[EntityCheckController]

        val result = controller.agentVerifyEntity().apply(FakeRequest(GET, "/agent/verify-entity"))

        status(result) mustBe OK
      }
    }
  }

  "clientVerifyEntity" should {
    "return OK" when {
      "a GET request to /agent/verify-entity" in {
        val controller = inject[EntityCheckController]

        val result = controller.clientVerifyEntity().apply(FakeRequest(GET, "/client/verify-entity"))

        status(result) mustBe OK
      }
    }
  }
}
