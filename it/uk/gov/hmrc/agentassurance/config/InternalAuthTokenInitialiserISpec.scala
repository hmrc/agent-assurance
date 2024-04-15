/*
package uk.gov.hmrc.agentassurance.config

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.agentassurance.stubs.InternalAuthStub
import uk.gov.hmrc.agentassurance.support.{UnitSpec, WireMockSupport}

class InternalAuthTokenInitialiserISpec extends UnitSpec with WireMockSupport with InternalAuthStub {

  val authToken = "authToken"
  val appName = "agent-assurance"

/*  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "internal-auth-token-enabled" -> true,
      "internal-auth.token" -> authToken,
      "microservice.services.internal-auth.host" -> wireMockHost,
      "microservice.services.internal-auth.port" -> wireMockPort
    ).build()*/

  "InternalAuth" should {
    "initialise the internal-auth token " in {


      val expectedRequest = Json.obj(
        "token" -> authToken,
        "principal" -> appName,
        "permissions" -> Seq(
          Json.obj(
            "resourceType" -> "dms-submission",
            "resourceLocation" -> "submit",
            "actions" -> List("WRITE")
          )
        )
      )

      getTestStubInternalAuthorised()
      postTestStubInternalAuthorised()

      GuiceApplicationBuilder()
        .configure(
          "internal-auth-token-enabled" -> true,
          "internal-auth.token" -> authToken,
          "microservice.services.internal-auth.host" -> wireMockHost,
          "microservice.services.internal-auth.port" -> wireMockPort
        ).build()

      verifyGetTestStubInternalAuth(authToken)
      verifyPostTestStubInternalAuth(expectedRequest)

    }
  }

}*/
