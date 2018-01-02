/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Singleton

import play.api.http.HttpErrorHandler
import play.api.http.Status._
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.http.{HttpException, Upstream4xxResponse, Upstream5xxResponse}

import scala.concurrent.Future

@Singleton
class ErrorHandler extends HttpErrorHandler {

  case class ErrorResponse(statusCode: Int, message: String, xStatusCode: Option[String] = None, requested: Option[String] = None)

  implicit val erFormats: OFormat[ErrorResponse] = Json.format[ErrorResponse]

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(
      Status(statusCode)(Json.toJson(ErrorResponse(statusCode, message, requested = Some(request.uri))))
    )
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    val errorResponse = exception match {
      case e: HttpException => ErrorResponse(e.responseCode, e.getMessage)
      case e: Upstream4xxResponse => ErrorResponse(e.reportAs, e.getMessage)
      case e: Upstream5xxResponse => ErrorResponse(e.reportAs, e.getMessage)
      case e: Throwable => ErrorResponse(INTERNAL_SERVER_ERROR, e.getMessage)
    }
    Future.successful(new Status(errorResponse.statusCode)(Json.toJson(errorResponse)))
  }
}
