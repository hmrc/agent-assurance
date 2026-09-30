/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentassurance.stubs

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.Scenario
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import uk.gov.hmrc.agentassurance.models.Arn
import uk.gov.hmrc.agentassurance.models.Utr
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.domain.TaxIdentifier

trait HipStubs {

  def givenHIPRespondsWithRegistrationData(
                                            identifier: TaxIdentifier,
                                            isIndividual: Boolean
                                          ): StubMapping = stubFor(
    post(urlEqualTo(s"/RESTAdapter/registration/${identifier.getClass.getSimpleName.toLowerCase}/${identifier.value}"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(registrationData(isIndividual))
      )
  )

  def verifyHIPGetAgentRegistrationData(
                                         identifier: TaxIdentifier,
                                         count: Int = 1
                                       ): Unit =
    eventually(Timeout(Span(5, Seconds))) {
      verify(
        count,
        postRequestedFor(
          urlEqualTo(s"/RESTAdapter/registration/${identifier.getClass.getSimpleName.toLowerCase}/${identifier.value}")
        )
      )
    }

  def givenHIPRespondsWithoutRegistrationData(identifier: TaxIdentifier): StubMapping = stubFor(
    post(urlEqualTo(s"/RESTAdapter/registration/${identifier.getClass.getSimpleName.toLowerCase}/${identifier.value}"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(invalidRegistrationData)
      )
  )

  def givenHIPReturnsErrorForRegistration(
                                           identifier: TaxIdentifier,
                                           responseCode: Int,
                                           errorMessage: String = failureResponseBody422
                                         ): StubMapping = stubFor(
    post(urlEqualTo(s"/RESTAdapter/registration/${identifier.getClass.getSimpleName.toLowerCase}/${identifier.value}"))
      .inScenario("HIP failure")
      .whenScenarioStateIs(Scenario.STARTED)
      .willReturn(
        aResponse()
          .withStatus(responseCode)
          .withBody(errorMessage)
      )
  )

  def givenHIPReturnsErrorFirstAndValidDataLater(
                                                  identifier: TaxIdentifier,
                                                  isIndividual: Boolean,
                                                  responseCode: Int
                                                ): StubMapping = {
    stubFor(
      post(urlEqualTo(s"/RESTAdapter/registration/${identifier.getClass.getSimpleName.toLowerCase}/${identifier.value}"))
        .inScenario("Retry")
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(
          aResponse()
            .withStatus(responseCode)
            .withBody(failureResponseBody)
        )
        .willSetStateTo("HIP Failure #2")
    )
    stubFor(
      post(urlEqualTo(s"/RESTAdapter/registration/${identifier.getClass.getSimpleName.toLowerCase}/${identifier.value}"))
        .inScenario("Retry")
        .whenScenarioStateIs("HIP Failure #2")
        .willReturn(
          aResponse()
            .withStatus(responseCode)
            .withBody(failureResponseBody)
        )
        .willSetStateTo("HIP Success")
    )
    stubFor(
      post(urlEqualTo(s"/RESTAdapter/registration/${identifier.getClass.getSimpleName.toLowerCase}/${identifier.value}"))
        .inScenario("Retry")
        .whenScenarioStateIs("HIP Success")
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(registrationData(isIndividual))
        )
        .willSetStateTo(Scenario.STARTED)
    )
  }

  private def registrationData(isIndividual: Boolean) =
    if isIndividual then
      registrationDataForIndividual
    else
      registrationDataForOrganisation

  private val registrationDataForOrganisation: String =
    s"""
       |{
       |   "contactDetails" : {},
       |   "organisation" : {
       |      "organisationName" : "CT AGENT 165",
       |      "organisationType" : "Not Specified",
       |      "isAGroup" : false
       |   },
       |   "address" : {
       |      "addressLine1" : "Matheson House 165",
       |      "countryCode" : "GB",
       |      "addressLine2" : "Grange Central 165",
       |      "addressLine4" : "Shropshire 165",
       |      "addressLine3" : "Telford 165",
       |      "postalCode" : "TF3 4ER"
       |   },
       |   "isEditable" : false,
       |   "isAnAgent" : true,
       |   "safeId" : "XH0000100100761",
       |   "agentReferenceNumber" : "SARN0001028",
       |   "isAnASAgent" : true,
       |   "isAnIndividual" : false,
       |   "sapNumber" : "0100100761"
       |}
     """.stripMargin

  private val registrationDataForIndividual: String =
    s"""
       |{
       |   "isAnIndividual" : true,
       |   "isAnASAgent" : true,
       |   "isEditable" : false,
       |   "isAnAgent" : true,
       |   "contactDetails" : {},
       |   "safeId" : "XR0000100115180",
       |   "agentReferenceNumber" : "PARN0002156",
       |   "individual" : {
       |      "firstName" : "First Name QM",
       |      "dateOfBirth" : "1992-05-10",
       |      "lastName" : "Last Name QM"
       |   },
       |   "address" : {
       |      "postalCode" : "TF3 4ER",
       |      "addressLine4" : "AddressFour 190",
       |      "addressLine2" : "AddressTwo 190",
       |      "addressLine1" : "AddressOne 190",
       |      "addressLine3" : "AddressThree 190",
       |      "countryCode" : "GB"
       |   },
       |   "sapNumber" : "0100115180"
       |}
     """.stripMargin

  private val invalidRegistrationData: String =
    s"""
       |{
       |   "isAnIndividual" : true,
       |   "isAnASAgent" : true,
       |   "isEditable" : false,
       |   "isAnAgent" : true,
       |   "contactDetails" : {},
       |   "safeId" : "XR0000100115180",
       |   "agentReferenceNumber" : "PARN0002156",
       |   "address" : {
       |      "postalCode" : "TF3 4ER",
       |      "addressLine4" : "AddressFour 190",
       |      "addressLine2" : "AddressTwo 190",
       |      "addressLine1" : "AddressOne 190",
       |      "addressLine3" : "AddressThree 190",
       |      "countryCode" : "GB"
       |   },
       |   "sapNumber" : "0100115180"
       |}
     """.stripMargin

  private val failureResponseBody: String = {
    """
      |{
      |   "code" : "SOME_FAILURE",
      |   "reason" : "Some reason"
      |}
    """.stripMargin
  }

  private val failureResponseBody422: String = {
    """
      |{
      |  "errors": {
      |    "code": "001",
      |    "processingDate": "2022-01-31T09:26:17Z",
      |    "text": "Request cannot be processed"
      |  }
      |}
       """.stripMargin
  }

}
