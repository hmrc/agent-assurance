/*
 * Copyright 2023 HM Revenue & Customs
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

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.Materializer
import play.api.Logging
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.connectors.DesConnector
import uk.gov.hmrc.agentassurance.models.utrcheck.BusinessNameByUtr
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class BusinessNamesService @Inject() (desConnector: DesConnector)(
  implicit
  val appConfig: AppConfig,
  mat: Materializer,
  ec: ExecutionContext
)
extends Logging {

  val maxCallsPerSecondBusinessNames: Int = appConfig.maxCallsPerSecondBusinessNames

  def get(utrs: Seq[String])(implicit headerCarrier: HeaderCarrier): Future[Set[BusinessNameByUtr]] = {
    Source(utrs.toList)
      .throttle(maxCallsPerSecondBusinessNames, 1.second)
      .mapAsync(parallelism = 1) { utrStr =>
        get(utrStr).map(an => BusinessNameByUtr(utrStr, an))
      }
      .runWith(Sink.collection[BusinessNameByUtr, Set[BusinessNameByUtr]])
  }

  def get(utr: String)(implicit headerCarrier: HeaderCarrier): Future[Option[String]] = {
    desConnector.getBusinessName(utr)
  }

}
