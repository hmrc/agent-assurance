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

package uk.gov.hmrc.agentassurance.services

import scala.concurrent.ExecutionContext

import org.scalatestplus.play.PlaySpec
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentassurance.helpers.TestConstants._
import uk.gov.hmrc.agentassurance.mocks.MockAuditService
import uk.gov.hmrc.agentassurance.mocks.MockDesConnector
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentmtdidentifiers.model.SuspensionDetails
import uk.gov.hmrc.http.HeaderCarrier

class EntityCheckServiceSpec extends PlaySpec with MockDesConnector with MockAuditService {

  val service = new EntityCheckService(mockDesConnector, mockAuditService)

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val req: Request[_]      = FakeRequest()

  "verifyAgent" should {
    "return AgentDetailsDesResponse " in {

      mockGetAgentRecord(testArn)(
        AgentDetailsDesResponse(
          uniqueTaxReference = None,
          agencyDetails = None,
          suspensionDetails = Some(SuspensionDetails(suspensionStatus = true, regimes = Some(Set("ITSA")))),
          isAnIndividual = None
        )
      )

      val result = await(service.verifyAgent(testArn))

      result mustBe AgentDetailsDesResponse(
        uniqueTaxReference = None,
        agencyDetails = None,
        suspensionDetails = Some(SuspensionDetails(suspensionStatus = true, regimes = Some(Set("ITSA")))),
        isAnIndividual = None
      )
    }
  }

}
