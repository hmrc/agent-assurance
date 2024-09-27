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

package uk.gov.hmrc.agentassurance.utils

import scala.util.Try

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.Logging

object StringFormatFallbackSetup extends Logging {

  def stringFormatFallback(format: Format[String]): Format[String] =
    Format(
      json =>
        Try(format.reads(json)).recover {
          case e: Throwable =>
            logger.warn(s"[StringFormatFallbackSetup][stringFormatFallback] failed to decrypt string: ${e.getMessage}")
            Json.fromJson[String](json)
        }.get,
      (value: String) => format.writes(value)
    )
}
