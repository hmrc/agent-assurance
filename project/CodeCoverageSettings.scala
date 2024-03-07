import sbt.Def

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "uk.gov.hmrc.BuildInfo",
    ".*Routes.*",
    ".*RoutesPrefix.*",
    ".*Filters?",
    "MicroserviceAuditConnector",
    "Module",
    "GraphiteStartUp",
    "ErrorHandler",
    ".*.Reverse[^.]*",
    "uk.gov.hmrc.agentassurance.controllers.testOnly",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*"
  )

  lazy val scoverageSettings = {
    import scoverage.ScoverageKeys
    Seq(
      ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
      ScoverageKeys.coverageMinimumStmtTotal := 80.00,
      //ScoverageKeys.coverageMinimumStmtPerFile := 80.00,
      ScoverageKeys.coverageFailOnMinimum := false,
      ScoverageKeys.coverageHighlighting := true
    )
  }

}
