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

package test.uk.gov.hmrc.agentassurance.controllers

import java.time.LocalDate

import scala.concurrent.duration.DurationInt
import scala.concurrent.Await

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.BodyWritable
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import play.api.Application
import test.uk.gov.hmrc.agentassurance.stubs.DesStubs
import test.uk.gov.hmrc.agentassurance.stubs.DmsSubmissionStubs
import test.uk.gov.hmrc.agentassurance.support.AgentAuthStubs
import test.uk.gov.hmrc.agentassurance.support.WireMockSupport
import uk.gov.hmrc.agentassurance.models._
import uk.gov.hmrc.agentassurance.models.Arn
import uk.gov.hmrc.agentassurance.models.Utr

class AgentServicesControllerISpec
extends PlaySpec
with AgentAuthStubs
with GuiceOneServerPerSuite
with WireMockSupport
with DesStubs
with DmsSubmissionStubs {

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.host" -> wireMockHost,
      "microservice.services.auth.port" -> wireMockPort,
      "microservice.services.des.host" -> wireMockHost,
      "microservice.services.des.port" -> wireMockPort,
      "microservice.services.dms-submission.host" -> wireMockHost,
      "microservice.services.dms-submission.port" -> wireMockPort,
      "auditing.enabled" -> false,
      "stride.roles.agent-assurance" -> "maintain_agent_manually_assure",
      "internal-auth-token-enabled-on-start" -> false,
      "http-verbs.retries.intervals" -> List("1ms"),
      "agent.cache.enabled" -> false
    )

  val arn = Arn("AARN0000002")
  val url = s"http://localhost:$port/agent-assurance/agent/agency-details/arn/${arn.value}"

  override def irAgentReference: String = "IRSA-123"

  val wsClient = app.injector.instanceOf[WSClient]

  def doGETRequest() = Await.result(
    wsClient
      .url(url)
      .withHttpHeaders("Authorization" -> "Bearer XYZ")
      .get(),
    15.seconds
  )

  def doPOSTRequest[T](body: T)(implicit wr: BodyWritable[T]) = Await.result(
    wsClient
      .url(url)
      .withHttpHeaders("Authorization" -> "Bearer XYZ")
      .post(body),
    15.seconds
  )

  val testUtr: Utr = Utr("7000000002")
  val membershipExpiresOnDate: LocalDate = LocalDate.now.plusWeeks(4)
  val testAmlsDetails: UkAmlsDetails = UkAmlsDetails(
    "supervisory",
    membershipNumber = Some("0123456789"),
    appliedOn = None,
    membershipExpiresOn = Some(membershipExpiresOnDate)
  )
  val testOverseasAmlsDetails: OverseasAmlsDetails = OverseasAmlsDetails("supervisory", membershipNumber = Some("0123456789"))
  val testOverseasAmlsEntity: OverseasAmlsEntity = OverseasAmlsEntity(
    arn,
    testOverseasAmlsDetails,
    None
  )

  val testCreatedDate: LocalDate = LocalDate.now.plusWeeks(2)
  val amlsEntity: UkAmlsEntity = UkAmlsEntity(
    utr = Some(testUtr),
    amlsDetails = testAmlsDetails,
    arn = Some(arn),
    createdOn = testCreatedDate,
    amlsSource = AmlsSource.Subscription
  )

  "GET /agent/agency-details/:arn" should {

    s"return OK with status when Agent details and Utr for the ARN" in {
      isLoggedInAsStride("stride")
      givenDESGetAgentRecord(arn, Some(testUtr))
      val response = doGETRequest()
      response.status mustBe OK
      response.json mustBe Json.obj(
        "agencyDetails" -> Json.obj(
          "agencyName" -> "ABC Accountants",
          "agencyEmail" -> "abc@xyz.com",
          "agencyTelephone" -> "07345678901",
          "agencyAddress" -> Json.obj(
            "addressLine1" -> "Matheson House",
            "addressLine2" -> "Grange Central",
            "addressLine3" -> "Town Centre",
            "addressLine4" -> "Telford",
            "postalCode" -> "TF3 4ER",
            "countryCode" -> "GB"
          )
        ),
        "utr" -> "7000000002"
      )

    }

    s"return OK with status when Agent details and No Utr for the ARN" in {
      isLoggedInAsStride("stride")
      givenDESGetAgentRecord(arn, None)
      val response = doGETRequest()
      response.status mustBe OK
      response.json mustBe Json.obj(
        "agencyDetails" -> Json.obj(
          "agencyName" -> "ABC Accountants",
          "agencyEmail" -> "abc@xyz.com",
          "agencyTelephone" -> "07345678901",
          "agencyAddress" -> Json.obj(
            "addressLine1" -> "Matheson House",
            "addressLine2" -> "Grange Central",
            "addressLine3" -> "Town Centre",
            "addressLine4" -> "Telford",
            "postalCode" -> "TF3 4ER",
            "countryCode" -> "GB"
          )
        )
      )

    }

    s"return NO_CONTENT when no Agent Details found for ARN" in {
      isLoggedInAsStride("stride")
      givenNoDESGetAgentRecord(arn, None)
      val response = doGETRequest()
      response.status mustBe NO_CONTENT
    }

  }

  "POST /agent/agency-details/:arn" should {
    "return ACCEPTED status when the DMS submission was successful" in {
      isLoggedInAsStride("stride")
      givenDmsSubmissionSuccess

      val html = "<html><head></head><body></body></html>"
      val encodedHtmlStr = java.util.Base64.getEncoder.encodeToString(html.getBytes())
      val response = doPOSTRequest(encodedHtmlStr)
      response.status mustBe CREATED
    }

    "return internal server error when payload is not encoded" in {
      isLoggedInAsStride("stride")

      val response = doPOSTRequest(s"""{"a":"b"}""")
      response.status mustBe INTERNAL_SERVER_ERROR
      response.body.contains("build PDF failed with error:")
    }

    "return internal server error when payload empty" in {
      isLoggedInAsStride("stride")

      val response = doPOSTRequest("")
      response.status mustBe INTERNAL_SERVER_ERROR
      response.body.contains("base64 encoding failed with field not provided")
    }
  }

}
