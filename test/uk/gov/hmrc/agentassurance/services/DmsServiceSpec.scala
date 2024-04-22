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

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Base64

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.mocks._
import uk.gov.hmrc.agentassurance.models.DmsResponse
import uk.gov.hmrc.agentassurance.models.DmsSubmissionReference
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.http.UpstreamErrorResponse

class DmsServiceSpec extends PlaySpec with MockDmsConnector with MockAppConfig {

  val html                          = "<html><head></head><body></body></html>"
  val now: Instant                  = Instant.now
  implicit val appConfig: AppConfig = mockAppConfig
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  val service = new DmsService(mockDmsConnector, appConfig)

  "submitToDms" should {
    "return correct value when the submission is successful" in {
      val encoded = Base64.getEncoder.encodeToString(html.getBytes)

      val timestamp = LocalDateTime
        .of(2022, 3, 2, 12, 30, 45)
        .atZone(ZoneId.of("UTC"))
        .toInstant

      mocksendPdfAccepted()

      val result =
        await(service.submitToDms(Some(encoded), timestamp, DmsSubmissionReference("DmsSubmissionReference"))(hc))

      result mustBe DmsResponse(timestamp, "")
    }

    "return upstream error if submission fails" in {
      val encoded = Base64.getEncoder.encodeToString(html.getBytes)

      val timestamp = LocalDateTime
        .of(2022, 3, 2, 12, 30, 45)
        .atZone(ZoneId.of("UTC"))
        .toInstant

      mocksendPdfUpstreamErrorResponse()

      an[UpstreamErrorResponse] mustBe thrownBy {
        await(service.submitToDms(Some(encoded), timestamp, DmsSubmissionReference("DmsSubmissionReference")))
      }

    }

    "return upstream error if submission fails with NonFatal code" in {
      val encoded = Base64.getEncoder.encodeToString(html.getBytes)
      mocksendPdfNonFatal()

      an[InternalServerException] mustBe thrownBy {
        await(service.submitToDms(Some(encoded), now, DmsSubmissionReference("DmsSubmissionReference")))
      }
    }

    "return upstream error if no data to submit" in {

      an[InternalServerException] mustBe thrownBy {
        await(service.submitToDms(None, now, DmsSubmissionReference("DmsSubmissionReference")))
      }
    }
  }
}
