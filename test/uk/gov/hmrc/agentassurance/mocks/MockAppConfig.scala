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

import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait MockAppConfig extends MockFactory {

  val mockServiceConfig: ServicesConfig = mock[ServicesConfig]

  (mockServiceConfig.getInt(_: String)).expects(*).atLeastOnce().returning(1)
  (mockServiceConfig.baseUrl(_: String)).expects(*).atLeastOnce().returning("some-url")
  (mockServiceConfig.getConfString(_: String, _: String)).expects(*, *).atLeastOnce().returning("some-string")
  (mockServiceConfig.getString(_: String)).expects("stride.roles.agent-assurance").atLeastOnce().returning("maintain_agent_manually_assure")

  val mockAppConfig: AppConfig = new AppConfig(mockServiceConfig)
}
