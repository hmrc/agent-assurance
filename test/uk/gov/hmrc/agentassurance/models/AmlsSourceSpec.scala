/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentassurance.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.JsString
import play.api.libs.json.Json
import uk.gov.hmrc.agentassurance.helpers.TestConstants.testUKAmlsEntity

class AmlsSourceSpec
extends AnyFlatSpec
with Matchers:

  AmlsSource.values.foreach: amlsSource =>
    it should s"serialize and deserialize ${amlsSource.toString}" in:
      Json.toJson(amlsSource) shouldBe JsString(amlsSource.toString)
      Json.parse(s"\"${amlsSource.toString}\"").as[AmlsSource] shouldBe amlsSource

  it should "reject unknown values" in:
    val result = Json.parse("\"NotARealValue\"").validate[AmlsSource]
    result.isError shouldBe true

  it should "round trip inside UkAmlsEntity JSON" in:
    Json.toJson(testUKAmlsEntity).as[UkAmlsEntity] shouldBe testUKAmlsEntity

end AmlsSourceSpec
