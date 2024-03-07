/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentassurance.config

import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(servicesConfig: ServicesConfig) {

  val appName = "agent-assurance"

  private def getConf(key: String) =
    servicesConfig.getConfString(key, throw new RuntimeException(s"config $key not found"))

  private def baseUrl(key: String) = servicesConfig.baseUrl(key)

  val authBaseUrl: String = baseUrl("auth")
  val desBaseUrl: String = baseUrl("des")
  val acaBaseUrl: String = baseUrl("agent-client-authorisation")
  val esProxyUrl: String = baseUrl("enrolment-store-proxy")

  val minimumIRPAYEClients: Int = servicesConfig.getInt("minimumIRPAYEClients")
  val minimumIRSAClients: Int = servicesConfig.getInt("minimumIRSAClients")
  val minimumVatDecOrgClients: Int = servicesConfig.getInt("minimumVatDecOrgClients")
  val minimumIRCTClients: Int = servicesConfig.getInt("minimumIRCTClients")

  val manuallyAssuredStrideRole: String = servicesConfig.getString("stride.roles.agent-assurance")

  val desEnv: String = getConf("des.environment")
  val desAuthToken: String = getConf("des.authorization-token")
}
