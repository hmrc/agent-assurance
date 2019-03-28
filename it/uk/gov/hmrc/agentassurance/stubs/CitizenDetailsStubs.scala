package uk.gov.hmrc.agentassurance.stubs

import com.github.tomakehurst.wiremock.client.WireMock._


trait CitizenDetailsStubs {

  def givencitizenDetailsFoundForNino(nino: String, dob: String) = {
      stubFor(
        get(urlEqualTo(s"/citizen-details/nino/$nino"))
          .willReturn(aResponse().withStatus(200)
          .withBody(s"""{
                      |  "name" : {
                      |    "current" : {
                      |      "firstName" : "firstName",
                      |      "lastName" : "lastName"
                      |    },
                      |    "previous" : [ ]
                      |  },
                      |  "ids" : {
                      |    "nino" : "$nino"
                      |  },
                      |  "dateOfBirth" : "$dob"
                      |}""".stripMargin))
      )
  }

  def givenCitizenDetailsNotFoundForNino(nino: String) = {
    stubFor(
      get(urlEqualTo(s"/citizen-details/nino/$nino"))
        .willReturn(aResponse().withStatus(400)
        .withBody(
          s"""{
            |  "code" : "INVALID_NINO",
            |  "message" : "Provided NINO $nino is not valid"
            |}""".stripMargin
        ))

    )
  }

  def givenCitizenDetailsNetworkError(nino: String) = {
    stubFor(
      get(urlEqualTo(s"/citizen-details/nino/$nino"))
        .willReturn(aResponse().withStatus(500))
    )
  }

}
