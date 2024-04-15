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

import play.api.Logging
import uk.gov.hmrc.agentassurance.connectors.DesConnector
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgencyDetailsService @Inject()(desConnector: DesConnector) extends Logging {

  def agencyDetailsHasUkAddress(arn: Arn)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] =
   desConnector.getAgentRecord(arn).map(_.agencyDetails.exists(_.hasUkAddress))
}
