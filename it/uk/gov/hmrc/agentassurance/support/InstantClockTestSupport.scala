package uk.gov.hmrc.agentassurance.support

import java.time._

trait  InstantClockTestSupport /*extends AnyFeatureSpec with GuiceOneServerPerSuite*/{
  lazy val localDateTime: LocalDateTime = LocalDateTime.now()
  lazy val instant: Instant = localDateTime.toInstant(ZoneOffset.UTC)
  lazy val frozenInstant: Instant = instant

  implicit val clock: Clock = Clock.fixed(frozenInstant, ZoneId.of("UTC"))



}
