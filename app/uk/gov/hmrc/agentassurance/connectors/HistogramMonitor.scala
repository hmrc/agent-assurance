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

package uk.gov.hmrc.agentassurance.connectors

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Success

import com.codahale.metrics.MetricRegistry
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

trait HistogramMonitor {

  val metrics: Metrics
  val registry: MetricRegistry = metrics.defaultRegistry

  def reportHistogramValue[T](name: String)(function: => Future[Int])(implicit ec: ExecutionContext): Future[Int] = function.andThen {
    case Success(c) => registry.getHistograms.getOrDefault(histogramName(name), registry.histogram(histogramName(name))).update(c)
  }

  def histogramName[T](counterName: String): String = {
    s"Histogram-$counterName"
  }

}
