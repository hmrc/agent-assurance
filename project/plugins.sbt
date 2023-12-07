resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe.play"    % "sbt-plugin"          % "2.9.0")
addSbtPlugin("uk.gov.hmrc"          % "sbt-auto-build"      % "3.15.0")
addSbtPlugin("uk.gov.hmrc"          % "sbt-distributables"  % "2.4.0")
addSbtPlugin("org.scoverage"        % "sbt-scoverage"       % "1.9.3")
//fix for scoverage compile errors for scala 2.13.10
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
