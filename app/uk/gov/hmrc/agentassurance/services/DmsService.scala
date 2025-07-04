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

import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.apache.pekko.NotUsed
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.DataPart
import play.api.mvc.MultipartFormData.FilePart
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.connectors.DmsConnector
import uk.gov.hmrc.agentassurance.models.DmsResponse
import uk.gov.hmrc.agentassurance.models.DmsSubmissionReference
import uk.gov.hmrc.agentassurance.utils.PdfGenerator.buildPdf
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.http.UpstreamErrorResponse

@Singleton
class DmsService @Inject() (
  dmsConnector: DmsConnector,
  appConfig: AppConfig
)(implicit ec: ExecutionContext) {

  def submitToDms(
    base64EncodedDmsSubmissionHtml: Option[String],
    now: Instant,
    submissionReference: DmsSubmissionReference
  )(
    implicit hc: HeaderCarrier
  ): Future[DmsResponse] =
    for {
      pdf <- createPdf(base64EncodedDmsSubmissionHtml)
      body <- createBody(
        pdf,
        now,
        submissionReference
      )
      response <- sendPdf(body, now)(hc)
    } yield response

  private def createPdf(
    base64EncodedDmsSubmissionHtml: Option[String]
  ): Future[ByteArrayOutputStream] = Future.successful(
    base64EncodedDmsSubmissionHtml match {
      case Some(value) =>
        Try(new String(Base64.getDecoder.decode(value))) match {
          case Success(result) =>
            Try(buildPdf(result)) match {
              case Success(pdfResult) => pdfResult
              case Failure(e) => throw new RuntimeException(s"build PDF failed with error:${e.getCause}")
            }
          case Failure(e) => throw new RuntimeException(s"build PDF failed with error:${e.getCause}")
        }
      case None => throw new InternalServerException(s"base64 encoding failed with field not provided")
    }
  )

  private def createBody(
    pdf: ByteArrayOutputStream,
    now: Instant,
    submissionReference: DmsSubmissionReference
  ): Future[Source[MultipartFormData.Part[Source[ByteString, NotUsed]], NotUsed]] = Future.successful(
    Try(
      DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
        LocalDateTime.ofInstant(now, ZoneOffset.UTC)
      )
    ) match {
      case Success(result) =>
        assembleBodySource(
          pdf,
          result,
          submissionReference
        )
      case Failure(e) => throw new InternalServerException(s"build PDF failed with error:${e.getCause}")
    }
  )

  private def assembleBodySource(
    pdf: ByteArrayOutputStream,
    dateOfReceipt: String,
    submissionReference: DmsSubmissionReference
  ): Source[MultipartFormData.Part[Source[ByteString, NotUsed]], NotUsed] = {

    Source(
      Seq(
        DataPart("callbackUrl", appConfig.dmsSubmissionCallbackUrl),
        DataPart("submissionReference", submissionReference.submissionReference),
        DataPart("metadata.source", appConfig.dmsSubmissionSource),
        DataPart("metadata.timeOfReceipt", dateOfReceipt),
        DataPart("metadata.formId", appConfig.dmsSubmissionFormId),
        DataPart("metadata.customerId", appConfig.dmsSubmissionCustomerId),
        DataPart("metadata.classificationType", appConfig.dmsSubmissionClassificationType),
        DataPart("metadata.businessArea", appConfig.dmsSubmissionBusinessArea),
        FilePart(
          key = "form",
          filename = "form.pdf",
          contentType = Some("application/pdf"),
          ref = Source.single(ByteString(pdf.toByteArray))
        )
      )
    )
  }

  def sendPdf(
    body: Source[MultipartFormData.Part[Source[ByteString, NotUsed]], NotUsed],
    now: Instant
  )(implicit hc: HeaderCarrier): Future[DmsResponse] = dmsConnector
    .sendPdf(body)
    .map(_ => DmsResponse(now, ""))
    .recover {
      case error @ UpstreamErrorResponse(
            message,
            code,
            _,
            _
          ) =>
        throw UpstreamErrorResponse(
          message,
          code,
          code
        )
      case NonFatal(e) => throw new InternalServerException(s"send PDF failed with error:${e.getCause}")
    }

}
