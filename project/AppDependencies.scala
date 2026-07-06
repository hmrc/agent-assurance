import sbt.*

object AppDependencies {

  private val mongoVer: String = "2.12.0"
  private val bootstrapVer: String = "10.7.0"
  private val playVer: String = "play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-backend-$playVer"    % bootstrapVer,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-$playVer"           % mongoVer,
    "uk.gov.hmrc"            %% s"internal-auth-client-$playVer" % "4.4.0",
    "uk.gov.hmrc"            %% s"crypto-json-$playVer"          % "8.4.0",
    "uk.gov.hmrc"            %% s"domain-$playVer"               % "13.0.0",
    "io.github.openhtmltopdf" % "openhtmltopdf-pdfbox"           % "1.1.37",
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVer"  % bootstrapVer,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVer" % mongoVer,
    "org.scalamock"     %% "scalamock"                 % "7.5.5",
    "org.scalacheck"    %% "scalacheck"                % "1.19.0"
  ).map(_ % Test)

}
