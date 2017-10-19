package uk.gov.hmrc.agentassurance.support

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import org.scalatest.{Matchers, Suite}
import play.api.Application

import scala.collection.JavaConversions

trait MetricTestSupport {
  self: Suite with Matchers =>

  def app: Application

  private var metricsRegistry: MetricRegistry = _

  def givenCleanMetricRegistry(): Unit = {
    val registry = app.injector.instanceOf[Metrics].defaultRegistry
    for (metric <- JavaConversions.asScalaIterator[String](registry.getMetrics.keySet().iterator())) {
      registry.remove(metric)
    }
    metricsRegistry = registry
  }

  def timerShouldExistsAndBeenUpdated(metric: String): Unit = {
    metricsRegistry.getTimers.get(s"Timer-$metric").getCount should be >= 1L
  }

}
