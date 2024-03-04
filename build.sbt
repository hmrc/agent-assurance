
lazy val root = Project("agent-assurance", file("."))
  .settings(
    name := "agent-assurance",
    organization := "uk.gov.hmrc",
    majorVersion := 1,
    scalaVersion := "2.13.10",
    PlayKeys.playDefaultPort := 9565,
    scalacOptions ++= Seq(
      "-Yrangepos",
      "-Xfatal-warnings",
      "-Xlint:-missing-interpolator,_",
      "-Xlint:-byname-implicit",
      "-Ywarn-dead-code",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-Wconf:src=target/.*:s", // silence warnings from compiled files
      "-Wconf:src=Routes/.*:s"  // silence warnings from routes files
    ),
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases"),
      Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns),
      "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
    ),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    routesImport ++= Seq("uk.gov.hmrc.agentassurance.binders.PathBinders._")
  )
  .settings(
    CodeCoverageSettings.scoverageSettings,
    Test / parallelExecution := false
  )
  .configs(IntegrationTest)
  .settings(
    //fix for scoverage compile errors for scala 2.13.10
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
  )
  .settings(
    IntegrationTest / Keys.fork := false,
    Defaults.itSettings,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value,
    IntegrationTest / parallelExecution := false
  )
  .enablePlugins(Seq(PlayScala,SbtDistributablesPlugin) : _*)

