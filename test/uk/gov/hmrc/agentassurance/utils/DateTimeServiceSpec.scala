/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentassurance.utils

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import java.time._

class DateTimeServiceSpec
extends AnyWordSpec
with Matchers {

  "NowAsString" should {

    "format the fixed instant in UTC correctly" in {
      val instant = Instant.parse("2025-03-29T22:00:00Z")
      implicit val clock: Clock = Clock.fixed(instant, ZoneId.of("UTC"))

      val result = DateTimeService.nowAsString

      result shouldBe "29 March 2025 10:00PM UTC"
    }

    "format the same instant in London zone correctly" in {
      val instant = Instant.parse("2025-03-29T22:00:00Z")
      implicit val clock: Clock = Clock.fixed(instant, ZoneId.of("Europe/London"))

      val result = DateTimeService.nowAsString

      result shouldBe "29 March 2025 10:00PM GMT"
    }

    "format an instant in summer time as BST" in {
      val instant = Instant.parse("2025-07-15T09:00:00Z")
      implicit val clock: Clock = Clock.fixed(instant, ZoneId.of("Europe/London"))

      val result = DateTimeService.nowAsString

      result shouldBe "15 July 2025 10:00AM BST"
    }
  }
}
