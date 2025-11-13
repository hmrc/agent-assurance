import sbt.*

object AppDependencies {

  private val mongoVer: String = "2.10.0"
  private val bootstrapVer: String = "10.4.0"

  lazy val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapVer,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30" % mongoVer,
    "org.julienrf" %% "play-json-derived-codecs" % "11.0.0",
    "uk.gov.hmrc" %% "internal-auth-client-play-30" % "4.3.0",
    "io.github.openhtmltopdf" % "openhtmltopdf-pdfbox" % "1.1.30",
    "com.beachape" %% "enumeratum-play-json" % "1.9.0",
    "uk.gov.hmrc" %% "crypto-json-play-30" % "8.4.0",
    "uk.gov.hmrc" %% "domain-play-30" % "11.0.0"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % mongoVer % Test,
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVer % Test,
    "org.scalamock" %% "scalamock" % "7.4.1" % Test,
    "org.scalacheck" %% "scalacheck" % "1.18.1" % Test
  )

}
