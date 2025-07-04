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

package uk.gov.hmrc.agentassurance.mocks

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.AuthProviders
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier

trait MockAuthConnector
extends MockFactory { this: TestSuite =>

  val mockAuthConnector = mock[AuthConnector]

  def mockAuth()(response: Either[String, Enrolments]) = {
    (mockAuthConnector
      .authorise(_: Predicate, _: Retrieval[Enrolments])(_: HeaderCarrier, _: ExecutionContext))
      .expects(
        AuthProviders(GovernmentGateway),
        Retrievals.allEnrolments,
        *,
        *
      )
      .returning(response.fold[Future[Enrolments]](e => Future.failed(new Exception(e)), r => Future.successful(r)))
  }

  def mockAuthWithNoRetrievals[A](retrieval: Retrieval[A])(result: A) = {
    (mockAuthConnector
      .authorise[A](_: Predicate, _: Retrieval[A])(_: HeaderCarrier, _: ExecutionContext))
      .expects(
        EmptyPredicate,
        retrieval,
        *,
        *
      )
      .returning(Future.successful(result))
  }

  def mockAgentAuth()(response: Either[String, Unit]) = {
    (mockAuthConnector
      .authorise(_: Predicate, _: EmptyRetrieval.type)(_: HeaderCarrier, _: ExecutionContext))
      .expects(
        AuthProviders(GovernmentGateway).and(AffinityGroup.Agent),
        EmptyRetrieval,
        *,
        *
      )
      .returning(response.fold[Future[Unit]](e => Future.failed(new Exception(e)), r => Future.successful(r)))
  }

}
