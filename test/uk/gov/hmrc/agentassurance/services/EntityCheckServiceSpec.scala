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

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

import scala.concurrent.ExecutionContext

import org.scalatestplus.play.PlaySpec
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.helpers.InstantClockTestSupport
import uk.gov.hmrc.agentassurance.helpers.TestConstants._
import uk.gov.hmrc.agentassurance.mocks.MockAppConfig
import uk.gov.hmrc.agentassurance.mocks.MockCitizenDetailsConnector
import uk.gov.hmrc.agentassurance.mocks.MockDesConnector
import uk.gov.hmrc.agentassurance.mocks.MockEmailService
import uk.gov.hmrc.agentassurance.models.entitycheck.DeceasedCheckException.EntityDeceasedCheckFailed
import uk.gov.hmrc.agentassurance.models.entitycheck.EntityCheckException
import uk.gov.hmrc.agentassurance.models.entitycheck.EntityCheckResult
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentassurance.models.EntityCheckNotification
import uk.gov.hmrc.agentmtdidentifiers.model.SuspensionDetails
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport
import uk.gov.hmrc.mongo.CurrentTimestampSupport

class EntityCheckServiceSpec
    extends PlaySpec
    with CleanMongoCollectionSupport
    with MockDesConnector
    with MockCitizenDetailsConnector
    with InstantClockTestSupport
    with MockAppConfig
    with MockEmailService {

  implicit val ac: AppConfig        = mockAppConfig
  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val req: Request[_]      = FakeRequest()

  val mongoLockRepository = new MongoLockRepository(mongoComponent, new CurrentTimestampSupport)
  val mongoLockService    = new MongoLockService(mongoLockRepository)

  val service =
    new EntityCheckService(mockDesConnector, mockCitizenDetailsConnector, mongoLockService, mockMockEmailService)

  "verifyAgent" should {
    "return Some(SuspensionDetails) when the agent is suspended" in {

      val agentDetailsDesResponse = AgentDetailsDesResponse(
        uniqueTaxReference = None,
        agencyDetails = None,
        suspensionDetails = Some(SuspensionDetails(suspensionStatus = true, regimes = Some(Set("ITSA")))),
        isAnIndividual = None
      )

      mockGetAgentRecord(testArn)(
        agentDetailsDesResponse
      )

      val result = await(service.verifyAgent(testArn))

      result mustBe EntityCheckResult(
        agentDetailsDesResponse,
        Seq.empty[EntityCheckException]
      )
    }

    "return None when the agent is not suspended" in {

      val agentDetailsDesResponse = AgentDetailsDesResponse(
        uniqueTaxReference = None,
        agencyDetails = None,
        suspensionDetails = None,
        isAnIndividual = None
      )
      mockGetAgentRecord(testArn)(
        agentDetailsDesResponse
      )

      val result = await(service.verifyAgent(testArn))

      result mustBe EntityCheckResult(agentDetailsDesResponse, Seq.empty[EntityCheckException])
    }

    "return Some(SuspensionDetails) and do entityChecks and do not sent email" in {

      val utr = Utr("1234567")
      val agentDetailsDesResponse = AgentDetailsDesResponse(
        uniqueTaxReference = Some(utr),
        agencyDetails = None,
        suspensionDetails = Some(SuspensionDetails(suspensionStatus = true, regimes = Some(Set("ITSA")))),
        isAnIndividual = None
      )

      mockGetAgentRecord(testArn)(
        agentDetailsDesResponse
      )

      mockGetCitizenDeceasedFlag(SaUtr(utr.value))(None)

      val result = await(service.verifyAgent(testArn))

      result mustBe EntityCheckResult(
        agentDetailsDesResponse,
        Seq.empty[EntityCheckException]
      )
    }

    "return Some(SuspensionDetails) and do entityChecks and sent email" in {

      val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy h:mma")
      val dateTime  = formatter.format(LocalDateTime.now())

      val utr = Utr("1234567")
      val agentDetailsDesResponse = AgentDetailsDesResponse(
        uniqueTaxReference = Some(utr),
        agencyDetails = None,
        suspensionDetails = Some(SuspensionDetails(suspensionStatus = true, regimes = Some(Set("ITSA")))),
        isAnIndividual = None
      )
      mockGetAgentRecord(testArn)(
        agentDetailsDesResponse
      )

      mockGetCitizenDeceasedFlag(SaUtr(utr.value))(Some(EntityDeceasedCheckFailed))

      mockSendEntityCheckNotification(
        EntityCheckNotification(
          arn = testArn,
          utr = utr.value,
          agencyName = "",
          failedChecks = "Agent is deceased.",
          dateTime = dateTime
        )
      )

      val result = await(service.verifyAgent(testArn))

      result mustBe EntityCheckResult(
        agentDetailsDesResponse,
        List(EntityDeceasedCheckFailed)
      )
    }

  }

}
