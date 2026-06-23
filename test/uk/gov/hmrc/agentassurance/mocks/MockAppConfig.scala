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

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq as equal
import org.mockito.Mockito.when
import org.scalatest.TestSuite
import org.scalatestplus.mockito.MockitoSugar
import play.api.ConfigLoader
import play.api.Configuration
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.DurationInt

trait MockAppConfig
extends MockitoSugar { this: TestSuite =>

  val mockServiceConfig: ServicesConfig = mock[ServicesConfig]
  val mockConfig: Configuration = mock[Configuration]

  when(mockServiceConfig.getInt(any[String])).thenReturn(1)
  when(mockServiceConfig.getInt(equal("rate-limiter.business-names.max-calls-per-second"))).thenReturn(10)
  when(mockServiceConfig.baseUrl(any[String])).thenReturn("some-url")
  when(mockServiceConfig.getString(any[String])).thenReturn("other-string")
  when(mockServiceConfig.getString(equal("stride.roles.agent-assurance"))).thenReturn("maintain_agent_manually_assure")
  when(mockServiceConfig.getString(equal("internal-auth.token"))).thenReturn("YWdlbnQtYXNzdXJhbmNl")
  when(mockServiceConfig.getBoolean(equal("internal-auth-token-enabled-on-start"))).thenReturn(false)
  when(mockServiceConfig.getString(equal("microservice.services.dms-submission.contact-details-submission.callbackEndpoint"))).thenReturn("callbackEndpoint")
  when(mockServiceConfig.getString(equal("microservice.services.dms-submission.contact-details-submission.businessArea"))).thenReturn("businessArea")
  when(mockServiceConfig.getString(equal("microservice.services.dms-submission.contact-details-submission.customerId"))).thenReturn("customerId")
  when(mockServiceConfig.getString(equal("microservice.services.dms-submission.contact-details-submission.source"))).thenReturn("source")
  when(mockServiceConfig.getDuration(any[String])).thenReturn(1.second)
  when(mockServiceConfig.getDuration(equal("agent.entity-check.lock.expires"))).thenReturn(1.second)
  when(mockServiceConfig.getDuration(equal("agent.entity-check.email.lock.expires"))).thenReturn(1.second)

  when(mockConfig.get[Seq[String]](equal("internalServiceHostPatterns"))(using any[ConfigLoader[Seq[String]]]))
    .thenReturn(Seq(
      "^.*\\.service$",
      "^.*\\.mdtp$",
      "^localhost$"
    ))
  when(mockConfig.get[String](equal("microservice.services.des.environment"))(using any[ConfigLoader[String]]))
    .thenReturn("test")
  when(mockConfig.get[String](equal("microservice.services.des.authorization-token"))(using any[ConfigLoader[String]]))
    .thenReturn("secret")

  when(mockConfig.get[String](equal("agent-maintainer-email"))(using any[ConfigLoader[String]]))
    .thenReturn("test@example.com")

  when(mockConfig.get[Boolean](equal("features.use-agent-services-account-amls"))(using any[ConfigLoader[Boolean]]))
    .thenReturn(false)

  implicit val mockAppConfig: AppConfig = new AppConfig(mockConfig, mockServiceConfig)

}
