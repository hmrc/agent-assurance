import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

lazy val compileDeps = Seq(
  "uk.gov.hmrc" %% "bootstrap-backend-play-27" % "5.6.0",
  "uk.gov.hmrc" %% "auth-client" % "5.6.0-play-27",
  "uk.gov.hmrc" %% "agent-mtd-identifiers" % "0.25.0-play-27",
  "uk.gov.hmrc" %% "agent-kenshoo-monitoring" % "4.7.0-play-27",
  "uk.gov.hmrc" %% "simple-reactivemongo" % "8.0.0-play-27"
)

def testDeps(scope: String) = Seq(
  "org.scalatest" %% "scalatest" % "3.0.8" % scope,
  "org.scalamock" %% "scalamock" % "4.4.0" % scope,
  "org.mockito" % "mockito-core" % "2.27.0" % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % scope, //upgrade
  "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-26" % scope,
  "com.github.tomakehurst" % "wiremock-jre8" % "2.27.1" % scope,
  "uk.gov.hmrc" %% "reactivemongo-test" % "4.21.0-play-27" % scope
)

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;ErrorHandler;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimum := 80.00,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

lazy val root = Project("agent-assurance", file("."))
  .settings(
    name := "agent-assurance",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.10",
    PlayKeys.playDefaultPort := 9565,
    resolvers := Seq(
      Resolver.typesafeRepo("releases"),
      Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
    ),
    resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2",
    resolvers += "HMRC-local-artefacts-maven" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases-local",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.4.4" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.4.4" % Provided cross CrossVersion.full
    ),
    libraryDependencies ++= compileDeps ++ testDeps("test") ++ testDeps("it"),
    publishingSettings,
    scoverageSettings,
    routesImport ++= Seq("uk.gov.hmrc.agentassurance.binders.PathBinders._")
  )
  .configs(IntegrationTest)
  .settings(
    majorVersion := 0,
    Keys.fork in IntegrationTest := false,
    Defaults.itSettings,
    scalacOptions ++= Seq(
      "-Yrangepos",
      "-Xfatal-warnings",
      "-Xlint:-missing-interpolator,_",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-P:silencer:pathFilters=views;routes"),
    unmanagedSourceDirectories in IntegrationTest += baseDirectory(_ / "it").value,
    parallelExecution in IntegrationTest := false
  )
  .enablePlugins(Seq(PlayScala,SbtDistributablesPlugin) : _*)

