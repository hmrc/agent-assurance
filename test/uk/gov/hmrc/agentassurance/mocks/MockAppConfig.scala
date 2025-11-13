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

import scala.concurrent.duration.DurationInt

import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import play.api.ConfigLoader
import play.api.Configuration
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait MockAppConfig
extends MockFactory { this: TestSuite =>

  val mockServiceConfig: ServicesConfig = mock[ServicesConfig]
  val mockConfig: Configuration = mock[Configuration]

  (mockServiceConfig
    .getInt(_: String))
    .expects("rate-limiter.business-names.max-calls-per-second")
    .atLeastOnce()
    .returning(10)

  (mockServiceConfig.getInt(_: String)).expects(*).atLeastOnce().returning(1)
  (mockServiceConfig.baseUrl(_: String)).expects(*).atLeastOnce().returning("some-url")
  (mockServiceConfig.getConfString(_: String, _: String)).expects(*, *).atLeastOnce().returning("some-string")
  (mockServiceConfig
    .getString(_: String))
    .expects("stride.roles.agent-assurance")
    .atLeastOnce()
    .returning("maintain_agent_manually_assure")
  (mockServiceConfig
    .getString(_: String))
    .expects("internal-auth.token")
    .atLeastOnce()
    .returning("YWdlbnQtYXNzdXJhbmNl")
  (mockServiceConfig
    .getBoolean(_: String))
    .expects("internal-auth-token-enabled-on-start")
    .atLeastOnce()
    .returning(false)
  (mockServiceConfig
    .getString(_: String))
    .expects("microservice.services.dms-submission.contact-details-submission.callbackEndpoint")
    .atLeastOnce()
    .returning("callbackEndpoint")
  (mockServiceConfig
    .getString(_: String))
    .expects("microservice.services.dms-submission.contact-details-submission.businessArea")
    .atLeastOnce()
    .returning("businessArea")
//  (mockServiceConfig.getString(_: String)).expects("microservice.services.dms-submission.contact-details-submission.classificationType)").atLeastOnce().returning("classificationType")
  (mockServiceConfig
    .getString(_: String))
    .expects("microservice.services.dms-submission.contact-details-submission.customerId")
    .atLeastOnce()
    .returning("customerId")
//  (mockServiceConfig.getString(_: String)).expects("microservice.services.dms-submission.contact-details-submission.formId)").atLeastOnce().returning("formId")
  (mockServiceConfig
    .getString(_: String))
    .expects("microservice.services.dms-submission.contact-details-submission.source")
    .atLeastOnce()
    .returning("source")
  (mockServiceConfig.getString(_: String)).expects(*).atLeastOnce().returning("other-string")

  (mockConfig
    .get[Seq[String]](_: String)(_: ConfigLoader[Seq[String]]))
    .expects("internalServiceHostPatterns", *)
    .atLeastOnce()
    .returning(Seq(
      "^.*\\.service$",
      "^.*\\.mdtp$",
      "^localhost$"
    ))

  (mockServiceConfig
    .getDuration(_: String))
    .expects("agent.entity-check.lock.expires")
    .atLeastOnce()
    .returning(1.second)

  (mockServiceConfig
    .getDuration(_: String))
    .expects("agent.entity-check.email.lock.expires")
    .atLeastOnce()
    .returning(1.second)

  (mockConfig
    .get[String](_: String)(_: ConfigLoader[String]))
    .expects("agent-maintainer-email", *)
    .atLeastOnce()
    .returning("test@example.com")

  implicit val mockAppConfig: AppConfig = new AppConfig(mockConfig, mockServiceConfig)

}
