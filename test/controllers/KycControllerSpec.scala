/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.agentkyc.controllers.KycController
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future


class KycControllerSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite {
  val authConnector =  mock[AuthConnector]
  val controller = new KycController(authConnector)

  implicit val hc = new HeaderCarrier

  val agentEnrolment = Set(
    Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", "123")), state = "activated", delegatedAuthRule = None)
  )

  val enrolmentsWithIrSAAgent = Enrolments(agentEnrolment)
  val enrolmentsWithoutIrSAAgent = Enrolments(Set.empty)

  "KycController" should {
    "return NO_CONTENT where the current user is enrolled in IR-SA-AGENT" in {
      when(authConnector.authorise(any[Predicate],any[Retrieval[Enrolments]])(any(), any())).thenReturn(Future.successful(enrolmentsWithIrSAAgent))

      val response = controller.authorisedForIrSAAgent()(FakeRequest())

      status(response) mustBe NO_CONTENT
    }

    "return FORBIDDEN where the current user is not enrolled in IR-SA-AGENT" in {
      when(authConnector.authorise(any[Predicate],any[Retrieval[Enrolments]])(any(), any())).thenReturn(Future.successful(enrolmentsWithoutIrSAAgent))

      val response = controller.authorisedForIrSAAgent()(FakeRequest())

      status(response) mustBe FORBIDDEN
    }
  }
}
