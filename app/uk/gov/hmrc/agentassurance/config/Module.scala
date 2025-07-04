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

package uk.gov.hmrc.agentassurance.config

import java.time.Clock
import java.time.ZoneId

import com.google.inject.AbstractModule
import play.api.Configuration
import play.api.Environment

class Module(
  environment: Environment,
  configuration: Configuration
)
extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[Clock]).toInstance(Clock.system(ZoneId.systemDefault()))

    val internalAuthTokenEnabled: Boolean = configuration.get[Boolean]("internal-auth-token-enabled-on-start")

    if (internalAuthTokenEnabled) {
      bind(classOf[InternalAuthTokenInitialiser])
        .to(classOf[InternalAuthTokenInitialiserImpl])
        .asEagerSingleton()
    }
    else {
      bind(classOf[InternalAuthTokenInitialiser])
        .to(classOf[NoOpInternalAuthTokenInitialiser])
        .asEagerSingleton()
    }
  }
}
