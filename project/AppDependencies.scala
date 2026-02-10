import sbt.*

object AppDependencies {

  private val mongoVer: String = "2.12.0"
  private val bootstrapVer: String = "10.5.0"
  private val playVer: String = "play-30"

  lazy val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-backend-$playVer"    % bootstrapVer,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-$playVer"           % mongoVer,
    "uk.gov.hmrc"            %% s"internal-auth-client-$playVer" % "4.3.0",
    "uk.gov.hmrc"            %% s"crypto-json-$playVer"          % "8.4.0",
    "uk.gov.hmrc"            %% s"domain-$playVer"               % "11.0.0",
    "org.julienrf"           %% "play-json-derived-codecs"       % "11.0.0",
    "com.beachape"           %% "enumeratum-play-json"           % "1.9.0",
    "io.github.openhtmltopdf" % "openhtmltopdf-pdfbox"           % "1.1.30"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% s"bootstrap-test-$playVer" % bootstrapVer,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVer" % mongoVer,
    "org.scalamock" %% "scalamock" % "7.4.1",
    "org.scalacheck" %% "scalacheck" % "1.18.1"
  ).map(_ % Test)

}
