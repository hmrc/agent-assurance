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

package test.uk.gov.hmrc.agentassurance.connectors

import java.time.Clock

import scala.concurrent.ExecutionContext

import com.google.inject.AbstractModule
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.Application
import test.uk.gov.hmrc.agentassurance.stubs.EnrolmentStoreProxyStubs
import test.uk.gov.hmrc.agentassurance.support.UnitSpec
import test.uk.gov.hmrc.agentassurance.support.WireMockSupport
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.connectors.EnrolmentStoreProxyConnectorImpl
import uk.gov.hmrc.http.HeaderCarrier

class EnrolmentStoreProxyConnectorISpec
extends UnitSpec
with GuiceOneAppPerSuite
with WireMockSupport
with EnrolmentStoreProxyStubs {

  override implicit lazy val app: Application = appBuilder.build()

  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val connector = app.injector.instanceOf[EnrolmentStoreProxyConnectorImpl]

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.host" -> wireMockHost,
      "microservice.services.auth.port" -> wireMockPort,
      "microservice.services.des.host" -> wireMockHost,
      "microservice.services.des.port" -> wireMockPort,
      "microservice.services.des.environment" -> "test",
      "microservice.services.des.authorization-token" -> "secret",
      "microservice.services.enrolment-store-proxy.host" -> wireMockHost,
      "microservice.services.enrolment-store-proxy.port" -> wireMockPort,
      "microservice.services.internal-auth.host" -> wireMockHost,
      "microservice.services.internal-auth.port" -> wireMockPort,
      "auditing.consumer.baseUri.host" -> wireMockHost,
      "auditing.consumer.baseUri.port" -> wireMockPort,
      "internal-auth-token-enabled-on-start" -> false,
      "agent.cache.enabled" -> true,
      "agent.cache.expires" -> "1 hour",
      "auditing.enabled" -> false
    )
    .overrides(moduleWithOverrides)

  lazy val moduleWithOverrides: AbstractModule =
    new AbstractModule {
      override def configure(): Unit = {
        bind(classOf[Clock]).toInstance(clock)
      }
    }

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val ec: ExecutionContext = ExecutionContext.global

  private val service = "IR-PAYE"
  private val userId = "0000001531072644"

  "EnrolmentStoreProxyConnector getClientCount via EACD's ES2" should {

    "return an empty list when ES2 returns 204" in {
      noClientsAreAllocated(
        service,
        userId,
        204
      )
      await(connector.getClientCount(service, userId)) shouldBe 0
    }

    "throw an exception when ES2 returns an unexpected http response code" in {
      noClientsAreAllocated(
        service,
        userId,
        404
      )
      an[Exception] should be thrownBy await(connector.getClientCount(service, userId))
    }

    "return only clients from ES2 whose enrolments are in the 'Activated' or 'Unknown' state" when {
      def checkClientCount(
        withEnrolmentState: String,
        expectedClientListSize: Int
      ) = {
        sufficientClientsAreAllocated(
          service,
          userId,
          state = withEnrolmentState
        )

        await(connector.getClientCount(service, userId)) shouldBe expectedClientListSize
      }

      "enrolment state is 'Unknown'" in { // during the interim period before migration of data from GG to MDTP
        checkClientCount(
          withEnrolmentState = "Unknown",
          expectedClientListSize = 6
        )
      }
      "enrolment state is 'Activated'" in {
        checkClientCount(
          withEnrolmentState = "Activated",
          expectedClientListSize = 6
        )
      }
      "enrolment state is 'NotYetActivated'" in {
        checkClientCount(
          withEnrolmentState = "NotYetActivated",
          expectedClientListSize = 0
        )
      }
    }
  }

}
