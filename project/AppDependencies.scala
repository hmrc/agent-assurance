import sbt._

object AppDependencies {

  private val mongoVer: String      = "1.8.0"
  private val bootstrapVer: String  = "8.5.0"
  private val openHtmlToPdfVersion = "1.0.10"

  lazy val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"    % bootstrapVer,
    "uk.gov.hmrc"       %% "agent-mtd-identifiers"        % "2.0.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"           % mongoVer,
    "org.julienrf"      %% "play-json-derived-codecs"     % "11.0.0",
    "uk.gov.hmrc"       %% "internal-auth-client-play-30" % "2.0.0",
    "com.openhtmltopdf"  % "openhtmltopdf-pdfbox"         % openHtmlToPdfVersion,
    "com.beachape"      %% "enumeratum-play-json"         % "1.8.0",
    "uk.gov.hmrc"       %% "crypto-json-play-30"          % "8.1.0"
  )

  lazy val test = Seq(
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30" % mongoVer     % Test,
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"  % bootstrapVer % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"      % "7.0.1"      % Test,
    "org.scalatestplus"      %% "mockito-5-10"            % "3.2.18.0"   % Test,
    "org.scalamock"          %% "scalamock"               % "6.0.0"      % Test,
    "com.vladsch.flexmark"   % "flexmark-all"             % "0.64.8"     % Test
  )

}
