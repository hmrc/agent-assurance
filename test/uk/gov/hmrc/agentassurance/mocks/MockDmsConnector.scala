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

package uk.gov.hmrc.agentassurance.mocks

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.scalamock.scalatest.MockFactory
import play.api.mvc.MultipartFormData
import play.api.test.Helpers.{ACCEPTED, BAD_GATEWAY}
import uk.gov.hmrc.agentassurance.connectors.DmsConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future

trait MockDmsConnector extends MockFactory {

  val mockDmsConnector = mock[DmsConnector]

  def mocksendPdfAccepted() = {
    (mockDmsConnector.sendPdf(_: Source[MultipartFormData.Part[Source[ByteString, NotUsed]], NotUsed])(_: HeaderCarrier))
      .expects(*, *)
      .returning(Future successful HttpResponse.apply(ACCEPTED, ""))
  }

  def mocksendPdfUpstreamErrorResponse() = {
    (mockDmsConnector.sendPdf(_: Source[MultipartFormData.Part[Source[ByteString, NotUsed]], NotUsed])(_: HeaderCarrier))
      .expects(*, *)
      .returning(Future failed UpstreamErrorResponse.apply("Error message", BAD_GATEWAY))
  }

  def mocksendPdfNonFatal() = {
    (mockDmsConnector.sendPdf(_: Source[MultipartFormData.Part[Source[ByteString, NotUsed]], NotUsed])(_: HeaderCarrier))
      .expects(*, *)
      .returning(Future failed new Exception("Error message"))
  }


}
