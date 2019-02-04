import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

scalaVersion := "2.11.11"

lazy val compileDeps = Seq(
  "uk.gov.hmrc" %% "auth-client" % "2.19.0-play-25",
  "uk.gov.hmrc" %% "logback-json-logger" % "3.1.0",
  "de.threedimensions" %% "metrics-play" % "2.5.13",
  "uk.gov.hmrc" %% "bootstrap-play-25" % "4.8.0",
  "uk.gov.hmrc" %% "agent-mtd-identifiers" % "0.13.0",
  "uk.gov.hmrc" %% "agent-kenshoo-monitoring" % "3.4.0",
  "uk.gov.hmrc" %% "simple-reactivemongo" % "7.9.0-play-25"
)

def testDeps(scope: String) = Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % scope,
  "org.scalamock" %% "scalamock" % "4.1.0" % scope,
  "org.mockito" % "mockito-core" % "2.18.3" % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % scope, //upgrade
  "uk.gov.hmrc" %% "hmrctest" % "3.4.0-play-25" % scope,
  "com.github.tomakehurst" % "wiremock" % "2.17.0" % scope,
  "uk.gov.hmrc" %% "reactivemongo-test" % "4.6.0-play-25" % scope
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
    scalaVersion := "2.11.11",
    PlayKeys.playDefaultPort := 9565,
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.bintrayRepo("hmrc", "release-candidates"),
      Resolver.typesafeRepo("releases"),
      Resolver.jcenterRepo
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
    unmanagedSourceDirectories in IntegrationTest += baseDirectory(_ / "it").value,
    parallelExecution in IntegrationTest := false,
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value)
  )
  .enablePlugins(Seq(PlayScala,SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory) : _*)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) = {
  tests.map { test =>
    new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq(s"-Dtest.name=${test.name}"))))
  }
}
