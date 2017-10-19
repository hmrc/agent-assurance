package uk.gov.hmrc.agentassurance.support

import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}
import play.api.http.Status

trait IntegrationSpec extends FeatureSpec with GivenWhenThen with Matchers with Eventually with Status {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))
}
