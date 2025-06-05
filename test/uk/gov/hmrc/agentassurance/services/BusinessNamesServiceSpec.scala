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

package uk.gov.hmrc.agentassurance.services

import scala.concurrent.ExecutionContext.Implicits.global

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.agentassurance.mocks._
import uk.gov.hmrc.agentassurance.models.utrcheck.BusinessNameByUtr
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.http.HeaderCarrier

class BusinessNamesServiceSpec
extends PlaySpec
with MockDesConnector
with MockAppConfig
with ScalaFutures {

  implicit val system: ActorSystem = ActorSystem("test-system")
  implicit val mat: Materializer = Materializer(system)
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val service =
    new BusinessNamesService(mockDesConnector)(
      mockAppConfig,
      mat,
      global
    )
  val utr = Utr("1234567890")
  val utr1 = Utr("1234567891")
  val utr2 = Utr("1234567892")
  val utr3 = Utr("1234567893")

  "BusinessNamesService get(utr)" must {
    "return business name if connector returns Some" in {
      mockGetBusinessNameRecord(utr.value)(Some("HMRC"))

      service.get(utr.value).map { result =>
        result mustBe Some("HMRC")
      }
    }

    "return None if connector returns None" in {
      mockGetBusinessNameRecord(utr.value)(None)

      service.get(utr.value).map { result =>
        result mustBe None
      }
    }
  }

  "BusinessNamesService get(Seq[utr])" must {
    "return set of BusinessNameByUtr for all UTRs" in {
      val utrs = Seq(
        utr.value,
        utr1.value,
        utr2.value
      )
      val expectedResults = Map(
        utr -> Some("Name1"),
        utr1 -> Some("Name2"),
        utr2 -> None
      )

      expectedResults.foreach {
        case (utr, nameOpt) => mockGetBusinessNameRecord(utr.value)(nameOpt)
      }

      whenReady(service.get(utrs), timeout(Span(2, Seconds))) { result =>
        result should contain allElementsOf expectedResults.collect {
          case (utrStr, name) => BusinessNameByUtr(utrStr.value, name)
        }
      }

    }
  }

}
