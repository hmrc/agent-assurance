package uk.gov.hmrc.agentassurance.support

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import org.scalatest.Suite
import org.scalatest.matchers.should.Matchers
import play.api.Application

import scala.jdk.CollectionConverters._

trait MetricTestSupport {
  self: Suite with Matchers =>

  def app: Application

  private var metricsRegistry: MetricRegistry = _

  def givenCleanMetricRegistry(): Unit = {
    val registry = app.injector.instanceOf[Metrics].defaultRegistry
    for (metric <- (registry.getMetrics.keySet().iterator().asScala)) {
      registry.remove(metric)
    }
    metricsRegistry = registry
  }

  def timerShouldExistsAndBeenUpdated(metric: String): Unit = {
    val timers = metricsRegistry.getTimers
    val metrics = timers.get(s"Timer-$metric")
    if (metrics == null) throw new Exception(s"Metric [$metric] not found, try one of ${timers.keySet()}")
    metrics.getCount should be >= 1L
  }

  def histogramShouldExistsAndBeenUpdated(metric: String, max: Long): Unit = {
    val histogram = metricsRegistry.getHistograms
    val metrics = histogram.get(s"Histogram-$metric")
    if (metrics == null) throw new Exception(s"Metric [$metric] not found, try one of ${histogram.keySet()}")
    metrics.getSnapshot.getMax shouldBe max
  }

}
