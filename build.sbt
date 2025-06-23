import uk.gov.hmrc.{DefaultBuildSettings, SbtAutoBuildPlugin}

val appName = "agent-assurance"

ThisBuild / majorVersion := 2
ThisBuild / scalaVersion := "2.13.16"


lazy val root = (project in file("."))
  .settings(
    name := appName,
    organization := "uk.gov.hmrc",
    PlayKeys.playDefaultPort := 9565,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    resolvers ++= Seq(Resolver.typesafeRepo("releases")),
    routesImport ++= Seq(
      "uk.gov.hmrc.agentassurance.binders.PathBinders._",
      "uk.gov.hmrc.agentassurance.binders._",
      "uk.gov.hmrc.agentassurance.models.utrcheck.CollectionName",
      "uk.gov.hmrc.agentmtdidentifiers.model.Utr",
      "uk.gov.hmrc.agentmtdidentifiers.model.Arn",
      "uk.gov.hmrc.domain.Nino",
      "uk.gov.hmrc.domain.SaAgentReference"
    ),
    scalacOptions ++= scalaCOptions,
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
  )
  .settings(
    Test / parallelExecution := false,
    CodeCoverageSettings.scoverageSettings
  )
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)

val scalaCOptions = Seq(
  "-Yrangepos",
//  "-Xfatal-warnings",
  "-Xlint:-missing-interpolator,_",
  "-Xlint:-byname-implicit",
  "-Ywarn-dead-code",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:implicitConversions",
  "-Wconf:src=target/.*:s", // silence warnings from compiled files
  "-Wconf:src=Routes/.*:s"  // silence warnings from routes files
)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(root % "compile->compile;test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.test)
  .settings(
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )
