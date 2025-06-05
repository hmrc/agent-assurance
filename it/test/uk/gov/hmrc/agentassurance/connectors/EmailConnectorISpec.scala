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

package uk.gov.hmrc.agentassurance.connectors

import scala.concurrent.ExecutionContext

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.Application
import test.uk.gov.hmrc.agentassurance.stubs.DataStreamStub
import test.uk.gov.hmrc.agentassurance.support.UnitSpec
import test.uk.gov.hmrc.agentassurance.support.WireMockSupport
import uk.gov.hmrc.agentassurance.config.AppConfig
import uk.gov.hmrc.agentassurance.models.EmailInformation
import uk.gov.hmrc.agentassurance.stubs.EmailStub
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

class EmailConnectorISpec
extends UnitSpec
with GuiceOneAppPerSuite
with WireMockSupport
with DataStreamStub
with EmailStub {

  override implicit lazy val app: Application = appBuilder
    .build()
  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]
  val metrics: Metrics = app.injector.instanceOf[Metrics]
  val connector: EmailConnector =
    new EmailConnectorImpl(
      appConfig,
      httpClient,
      metrics
    )
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val ec: ExecutionContext = ExecutionContext.global

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.email.host" -> wireMockHost,
      "microservice.services.email.port" -> wireMockPort,
      "agent-maintainer-email" -> "test@example.com",
      "auditing.enabled" -> false
    )
    .bindings(bind[EmailConnector].toInstance(connector))

  val emailInfo: EmailInformation = EmailInformation(
    to = Seq("abc@xyz.com"),
    templateId = "template-id",
    parameters = Map("param1" -> "foo", "param2" -> "bar")
  )

  "sendEmail" should {

    "return Unit when the email service responds" in {
      givenEmailSent(emailInfo)

      val result: Unit = await(connector.sendEmail(emailInfo))

      result shouldBe (())
    }

    "not throw an Exception when the email service throws an Exception" in {
      givenEmailReturns500

      val result: Unit = await(connector.sendEmail(emailInfo))

      result shouldBe (())
    }
  }

}
