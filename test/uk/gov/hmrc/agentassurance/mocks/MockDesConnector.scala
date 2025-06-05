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

import scala.concurrent.Future

import org.scalamock.handlers.CallHandler2
import org.scalamock.handlers.CallHandler3
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import play.api.mvc.Request
import uk.gov.hmrc.agentassurance.connectors.DesConnector
import uk.gov.hmrc.agentassurance.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentassurance.models.AmlsSubscriptionRecord
import uk.gov.hmrc.agentassurance.util.toFuture
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.domain.SaAgentReference
import uk.gov.hmrc.domain.TaxIdentifier
import uk.gov.hmrc.http.HeaderCarrier

trait MockDesConnector
extends MockFactory { this: TestSuite =>

  val mockDesConnector = mock[DesConnector]

  def mockGetActiveCesaAgentRelationships(ti: TaxIdentifier)(
    response: Either[String, Seq[SaAgentReference]]
  ): CallHandler2[
    TaxIdentifier,
    HeaderCarrier,
    Future[Seq[SaAgentReference]]
  ] = {
    (mockDesConnector
      .getActiveCesaAgentRelationships(_: TaxIdentifier)(_: HeaderCarrier))
      .expects(ti, *)
      .returning(
        response.fold[Future[Seq[SaAgentReference]]](e => Future.failed(new Exception(e)), r => toFuture(r))
      )
  }

  def mockGetAmlsSubscriptionStatus(registrationNumber: String)(
    response: Future[AmlsSubscriptionRecord]
  ): CallHandler2[
    String,
    HeaderCarrier,
    Future[AmlsSubscriptionRecord]
  ] = {
    (mockDesConnector
      .getAmlsSubscriptionStatus(_: String)(_: HeaderCarrier))
      .expects(registrationNumber, *)
      .returning(response)
  }

  def mockGetAgentRecord(
    arn: Arn
  )(response: AgentDetailsDesResponse): CallHandler3[
    Arn,
    Request[_],
    HeaderCarrier,
    Future[AgentDetailsDesResponse]
  ] =
    (mockDesConnector
      .getAgentRecord(_: Arn)(_: Request[_], _: HeaderCarrier))
      .expects(arn, *, *)
      .returning(Future.successful(response))

  def mockGetBusinessNameRecord(
    utr: String
  )(response: Option[String]): CallHandler2[
    String,
    HeaderCarrier,
    Future[Option[String]]
  ] =
    (mockDesConnector
      .getBusinessName(_: String)(_: HeaderCarrier))
      .expects(utr, *)
      .returning(Future.successful(response))

}
