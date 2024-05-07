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

import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.OK
import play.api.test.DefaultAwaitTimeout
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import play.api.test.Helpers.stubControllerComponents
import play.api.test.Helpers.GET
import uk.gov.hmrc.agentassurance.helpers.TestConstants.enrolmentsWithNoIrSAAgent
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testArn
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testUtr
import uk.gov.hmrc.agentassurance.mocks.MockAuthConnector
import uk.gov.hmrc.agentassurance.mocks.MockEntityCheckService
import uk.gov.hmrc.agentassurance.models.entitycheck.EntityCheckException
import uk.gov.hmrc.agentassurance.models.entitycheck.EntityCheckResult
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.http.HeaderNames

class GetAgentRecordWithEntityChecksControllerSpec
    extends PlaySpec
    with DefaultAwaitTimeout
    with GuiceOneAppPerTest
    with MockAuthConnector
    with MockEntityCheckService
    with MockFactory {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  val controller = new GetAgentRecordWithEntityChecksController(
    stubControllerComponents(),
    mockEntityCheckService,
    mockAuthConnector,
  )(ec)

  "get" should {
    "return OK" in {

      mockAuth()(Right(enrolmentsWithNoIrSAAgent))
      val agentDetailsDesResponse =
        AgentDetailsDesResponse(
          uniqueTaxReference = Some(testUtr),
          agencyDetails = None,
          suspensionDetails = None,
          isAnIndividual = None
        )
      mockVerifyEntitySuccess(testArn)(
        EntityCheckResult(
          agentDetailsDesResponse,
          Seq.empty[EntityCheckException]
        )
      )

      val result = controller
        .get()
        .apply(
          FakeRequest(GET, "/agent-record-with-checks")
            .withHeaders(HeaderNames.authorisation -> "Some auth token")
        )

      status(result) mustBe OK
    }
  }

}
