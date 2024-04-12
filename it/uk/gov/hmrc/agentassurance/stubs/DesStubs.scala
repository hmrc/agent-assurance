package uk.gov.hmrc.agentassurance.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.domain.{Nino, SaAgentReference, TaxIdentifier}

trait DesStubs {

  def givenClientIdentifierIsInvalid(identifier: TaxIdentifier) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlMatching(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse().withStatus(400))
    )
  }

  val someAlienAgent = """{"hasAgent":false,"agentId":"alien"}"""
  val someCeasedAgent = """{"hasAgent":true,"agentId":"ex-agent","agentCeasedDate":"someDate"}"""

  def givenClientHasRelationshipWithAgentInCESA(identifier: TaxIdentifier, agentId: SaAgentReference) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlEqualTo(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse().withStatus(200)
          .withBody(s"""{"agents":[$someCeasedAgent,{"hasAgent":true,"agentId":"${agentId.value}"}, $someAlienAgent]}"""))
    )
  }

  def givenClientHasRelationshipWithMultipleAgentsInCESA(identifier: TaxIdentifier, agentIds: Seq[SaAgentReference]) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlEqualTo(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse().withStatus(200)
          .withBody(s"""{"agents":[${agentIds.map(saRef =>s"""{"hasAgent":true,"agentId":"${saRef.value}"}""").mkString(",")}, $someAlienAgent, $someCeasedAgent ]}"""))
    )
  }

  def givenClientRelationshipWithAgentCeasedInCESA(identifier: TaxIdentifier, agentId: String) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlEqualTo(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse().withStatus(200)
          .withBody(s"""{"agents":[{"hasAgent":true,"agentId":"$agentId","agentCeasedDate":"2010-01-01"}]}"""))
    )
  }

  def givenAllClientRelationshipsWithAgentsCeasedInCESA(identifier: TaxIdentifier, agentIds: Seq[String]) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlEqualTo(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse().withStatus(200)
          .withBody(s"""{"agents":[${agentIds.map(id =>s"""{"hasAgent":true,"agentId":"$id","agentCeasedDate":"2020-12-31"}""").mkString(",")}]}"""))
    )
  }

  def givenClientIsUnknown404(identifier: TaxIdentifier) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlEqualTo(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse()
          .withStatus(404)))
  }


  def givenClientHasNoActiveRelationshipWithAgentInCESA(identifier: TaxIdentifier) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlEqualTo(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse().withStatus(200)
          .withBody(s"""{"agents":[$someCeasedAgent, $someAlienAgent]}"""))
    )
  }

  def givenClientHasNoRelationshipWithAnyAgentInCESA(identifier: TaxIdentifier) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlEqualTo(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse().withStatus(200)
          .withBody(s"""{}"""))
    )
  }

  def givenClientIsUnknownInCESAFor(identifier: TaxIdentifier) = {
    val identifierType = clientIdentifierType(identifier)
    stubFor(
      get(urlEqualTo(s"/registration/relationship/$identifierType/${identifier.value}"))
        .willReturn(aResponse().withStatus(404))
    )
  }

  def givenDesReturnsServerError() = {
    stubFor(
      get(urlMatching(s"/registration/.*"))
        .willReturn(aResponse().withStatus(500))
    )
  }

  def givenDesReturnBadGateway() = {
    stubFor(
      get(urlMatching(s"/registration/.*"))
        .willReturn(aResponse().withStatus(502))
    )
  }

  def givenDesReturnsServiceUnavailable() = {
    stubFor(
      get(urlMatching(s"/registration/.*"))
        .willReturn(aResponse().withStatus(503))
    )
  }

  def amlsSubscriptionRecordExists(amlsRegNumber: String, status: String = "Approved") = {
    stubFor(get(urlEqualTo(s"/anti-money-laundering/subscription/$amlsRegNumber/status"))
      .willReturn(aResponse()
        .withStatus(200).withBody(
        s"""{
           |"formBundleStatus": "$status",
           |"safeId": "xyz",
           |"currentRegYearStartDate": "2021-01-01",
           |"currentRegYearEndDate": "2021-12-31",
           |"suspended": false
           |}""".stripMargin)))
  }

  def amlsSubscriptionRecordFails(amlsRegNumber: String, status: Int) = {
    stubFor(get(urlEqualTo(s"/anti-money-laundering/subscription/$amlsRegNumber/status"))
      .willReturn(aResponse()
        .withStatus(status)))
  }

  def givenDESGetAgentRecord(arn: Arn, utr: Option[Utr], overseas: Boolean = false): StubMapping =
    stubFor(
      get(urlEqualTo(
        s"/registration/personal-details/arn/${arn.value}"))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(personalDetailsResponseBodyWithValidData(utr, overseas))))

  def givenNoDESGetAgentRecord(arn: Arn, optUtr: Option[Utr]): StubMapping =
    stubFor(
      get(urlEqualTo(
        s"/registration/personal-details/arn/${arn.value}"))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(noPersonalDetailsResponseBodyWithValidData(optUtr))))



  private def clientIdentifierType(identifer: TaxIdentifier): String =
    identifer match {
      case _: Nino => "nino"
      case _: Utr => "utr"
      case e => throw new RuntimeException(s"Unacceptable taxIdentifier: $e")
    }


  def personalDetailsResponseBodyWithValidData(optUtr: Option[Utr], overseas: Boolean) =
    s"""
       |{
       |   "isAnOrganisation" : true,
       |   "contactDetails" : {
       |      "phoneNumber" : "07000000000"
       |   },
       |   "isAnAgent" : true,
       |   "safeId" : "XB0000100101711",
       |   """.stripMargin ++ optUtr.map(utr =>
      s""" "uniqueTaxReference": "${utr.value}",
         |""".stripMargin).getOrElse("") ++
          s""" "agencyDetails" : {
       |      "agencyAddress" : {
       |         "addressLine2" : "Grange Central",
       |         "addressLine3" : "Town Centre",
       |         "addressLine4" : "Telford",
       |         "postalCode" : "TF3 4ER",
       |         "countryCode" : "${if(overseas)"NZ" else "GB"}",
       |         "addressLine1" : "Matheson House"
       |      },
       |      "agencyName" : "ABC Accountants",
       |      "agencyEmail" : "abc@xyz.com",
       |      "agencyTelephone" : "07345678901"
       |   },
       |   "suspensionDetails": {"suspensionStatus": false},
       |   "organisation" : {
       |      "organisationName" : "CT AGENT 183",
       |      "isAGroup" : false,
       |      "organisationType" : "0000"
       |   },
       |   "addressDetails" : {
       |      "addressLine2" : "Grange Central 183",
       |      "addressLine3" : "Telford 183",
       |      "addressLine4" : "Shropshire 183",
       |      "postalCode" : "TF3 4ER",
       |      "countryCode" : "GB",
       |      "addressLine1" : "Matheson House 183"
       |   },
       |   "individual" : {
       |      "firstName" : "John",
       |      "lastName" : "Smith"
       |   },
       |   "isAnASAgent" : true,
       |   "isAnIndividual" : false,
       |   "businessPartnerExists" : true,
       |   "agentReferenceNumber" : "TestARN"
       |}
            """.stripMargin

  def noPersonalDetailsResponseBodyWithValidData(optUtr: Option[Utr]) =
    s"""
       |{
       |   "isAnOrganisation" : true,
       |   "contactDetails" : {
       |      "phoneNumber" : "07000000000"
       |   },
       |   "isAnAgent" : true,
       |   "safeId" : "XB0000100101711",
       |   """.stripMargin ++ optUtr.map(utr =>
      s""" "uniqueTaxReference": "${utr.value}",
         |""".stripMargin).getOrElse("") ++
      s""" "suspensionDetails": {"suspensionStatus": false},
         |   "organisation" : {
         |      "organisationName" : "CT AGENT 183",
         |      "isAGroup" : false,
         |      "organisationType" : "0000"
         |   },
         |   "addressDetails" : {
         |      "addressLine2" : "Grange Central 183",
         |      "addressLine3" : "Telford 183",
         |      "addressLine4" : "Shropshire 183",
         |      "postalCode" : "TF3 4ER",
         |      "countryCode" : "GB",
         |      "addressLine1" : "Matheson House 183"
         |   },
         |   "individual" : {
         |      "firstName" : "John",
         |      "lastName" : "Smith"
         |   },
         |   "isAnASAgent" : true,
         |   "isAnIndividual" : false,
         |   "businessPartnerExists" : true,
         |   "agentReferenceNumber" : "TestARN"
         |}
            """.stripMargin

}


