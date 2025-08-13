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

package uk.gov.hmrc.agentassurance.controllers

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import uk.gov.hmrc.agentassurance.helpers.TestConstants._
import uk.gov.hmrc.agentassurance.mocks.MockAppConfig
import uk.gov.hmrc.agentassurance.mocks.MockAuthConnector
import uk.gov.hmrc.agentassurance.mocks.MockEntityCheckService
import uk.gov.hmrc.agentassurance.models.entitycheck.EntityCheckException
import uk.gov.hmrc.agentassurance.models.entitycheck.EntityCheckResult
import uk.gov.hmrc.agentassurance.models.entitycheck.VerifyEntityRequest
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentassurance.models.SuspensionDetails
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.internalauth.client.test.BackendAuthComponentsStub
import uk.gov.hmrc.internalauth.client.test.StubBehaviour
import uk.gov.hmrc.internalauth.client.BackendAuthComponents
import uk.gov.hmrc.internalauth.client.Predicate
import uk.gov.hmrc.internalauth.client.Retrieval

class EntityCheckControllerSpec
extends PlaySpec
with DefaultAwaitTimeout
with GuiceOneAppPerTest
with MockAuthConnector
with MockAppConfig
with MockEntityCheckService
with MockFactory {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val as: ActorSystem = ActorSystem()
  implicit val mat: Materializer = Materializer(as)
  val mockStubBehaviour: StubBehaviour = mock[StubBehaviour]
  val stubBackendAuthComponents: BackendAuthComponents = BackendAuthComponentsStub(mockStubBehaviour)(stubControllerComponents(), implicitly)

  val controller =
    new EntityCheckController(
      stubControllerComponents(),
      mockEntityCheckService,
      mockAuthConnector,
      stubBackendAuthComponents
    )(ec, mockAppConfig)

  "agentVerifyEntity" should {
    "return NO_CONTENT" when {
      "not suspended and a POST request to /agent/verify-entity" in {

        mockAuth()(Right(enrolmentsWithNoIrSAAgent))

        val agentDetailsDesResponse = AgentDetailsDesResponse(
          uniqueTaxReference = None,
          agencyDetails = None,
          suspensionDetails = None,
          isAnIndividual = None
        )

        mockVerifyEntitySuccess(testArn)(EntityCheckResult(agentDetailsDesResponse, Seq.empty[EntityCheckException]))

        val result = controller
          .agentVerifyEntity()
          .apply(
            FakeRequest(POST, "/agent/verify-entity")
              .withHeaders(HeaderNames.authorisation -> "Some auth token")
              .withBody("")
          )

        status(result) mustBe NO_CONTENT

      }
    }

    "return OK" when {
      "suspended and a POST request to /agent/verify-entity" in {

        mockAuth()(Right(enrolmentsWithNoIrSAAgent))

        val agentDetailsDesResponse = AgentDetailsDesResponse(
          uniqueTaxReference = None,
          agencyDetails = None,
          suspensionDetails = Some(SuspensionDetails(suspensionStatus = true, Some(Set("ITSA")))),
          isAnIndividual = None
        )

        mockVerifyEntitySuccess(testArn)(
          EntityCheckResult(
            agentDetailsDesResponse,
            Seq.empty[EntityCheckException]
          )
        )

        val result = controller
          .agentVerifyEntity()
          .apply(
            FakeRequest(POST, "/agent/verify-entity")
              .withHeaders(HeaderNames.authorisation -> "Some auth token")
              .withBody("")
          )

        status(result) mustBe OK

      }
    }
  }

  "clientVerifyEntity" should {
    "return NO_CONTENT" when {
      "not suspended and a POST request to /client/verify-entity" in {

        (mockStubBehaviour
          .stubAuth[Unit](_: Option[Predicate], _: Retrieval[Unit]))
          .expects(*, *)
          .returning(Future.unit)

        val agentDetailsDesResponse = AgentDetailsDesResponse(
          uniqueTaxReference = None,
          agencyDetails = None,
          suspensionDetails = None,
          isAnIndividual = None
        )

        mockVerifyEntitySuccess(testArn)(EntityCheckResult(agentDetailsDesResponse, Seq.empty[EntityCheckException]))

        val result = controller
          .clientVerifyEntity()
          .apply(
            FakeRequest(POST, "/client/verify-entity")
              .withHeaders(HeaderNames.authorisation -> "Some auth token", "Content-Type" -> "application/json")
              .withBody(Json.toJson(VerifyEntityRequest(testArn)))
          )

        status(result) mustBe NO_CONTENT

      }

      "return OK" when {
        "suspended and a POST request to /client/verify-entity" in {

          (mockStubBehaviour
            .stubAuth[Unit](_: Option[Predicate], _: Retrieval[Unit]))
            .expects(*, *)
            .returning(Future.unit)

          val agentDetailsDesResponse = AgentDetailsDesResponse(
            uniqueTaxReference = None,
            agencyDetails = None,
            suspensionDetails = Some(SuspensionDetails(suspensionStatus = true, Some(Set("ITSA")))),
            isAnIndividual = None
          )

          mockVerifyEntitySuccess(testArn)(
            EntityCheckResult(
              agentDetailsDesResponse,
              Seq.empty[EntityCheckException]
            )
          )

          val result = controller
            .clientVerifyEntity()
            .apply(
              FakeRequest(POST, "/client/verify-entity")
                .withHeaders(HeaderNames.authorisation -> "Some auth token", "Content-Type" -> "application/json")
                .withBody(Json.toJson(VerifyEntityRequest(testArn)))
            )

          status(result) mustBe OK

          contentAsJson(result) mustBe Json.toJson(
            SuspensionDetails(suspensionStatus = true, regimes = Some(Set("ITSA")))
          )

        }
      }

      "return Bad request" when {
        "invalid request" in {

          (mockStubBehaviour
            .stubAuth[Unit](_: Option[Predicate], _: Retrieval[Unit]))
            .expects(*, *)
            .returning(Future.unit)

          val result = controller
            .clientVerifyEntity()
            .apply(
              FakeRequest(POST, "/client/verify-entity")
                .withHeaders(HeaderNames.authorisation -> "Some auth token", "Content-Type" -> "application/json")
                .withBody(Json.parse("""{"invalid": "invalid"}"""))
            )

          status(result) mustBe BAD_REQUEST
          contentAsString(result) mustBe "Invalid Arn"
        }
      }
    }
  }

}
