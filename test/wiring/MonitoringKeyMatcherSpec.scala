/*
 * Copyright 2019 HM Revenue & Customs
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

package wiring

import app.Routes
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.test.UnitSpec

class MonitoringKeyMatcherSpec extends UnitSpec {

  "MonitoringKeyMatcher" should {

    "prepare patterns and variables" in {
      val tested = new MonitoringKeyMatcher {
        override val keyToPatternMapping: Seq[(String, String)] = Seq()
      }
      tested.preparePatternAndVariables("""/some/test/:service/:clientId/:test1""") shouldBe( ("^.*/some/test/([^/]+)/([^/]+)/([^/]+)$",Seq("{service}", "{clientId}", "{test1}")) )
      tested.preparePatternAndVariables("""/some/test/:service/:clientId/:test1/""") shouldBe( ("^.*/some/test/([^/]+)/([^/]+)/([^/]+)/$",Seq("{service}", "{clientId}", "{test1}")) )
      tested.preparePatternAndVariables("""/some/test/:service/::clientId/:test1/""") shouldBe( ("^.*/some/test/([^/]+)/([^/]+)/([^/]+)/$",Seq("{service}", "{:clientId}", "{test1}")) )
      tested.preparePatternAndVariables("""/some/test/:service/clientId/:test1/""") shouldBe( ("^.*/some/test/([^/]+)/clientId/([^/]+)/$",Seq("{service}", "{test1}")) )
    }

    "throw exception if duplicate variable name in pattern" in {
      val tested = new MonitoringKeyMatcher {
        override val keyToPatternMapping: Seq[(String, String)] = Seq()
      }
      an[IllegalArgumentException] shouldBe thrownBy {
        tested.preparePatternAndVariables("""/some/test/:service/:clientId/:service""")
      }
    }

    "match value to known pattern and produce key with placeholders replaced" in {
      val tested = new MonitoringKeyMatcher {
        override val keyToPatternMapping: Seq[(String, String)] = Seq(
          "A-{service}" -> """/some/test/:service/:clientId""",
          "B-{service}" -> """/test/:service/bar/some""",
          "C-{service}" -> """/test/:service/bar""",
          "D-{service}" -> """/test/:service/""",
          "E-{clientId}-{service}" -> """/test/:service/:clientId"""
        )
      }
      tested.findMatchingKey("http://www.tax.service.hmrc.gov.uk/test/ME/bar") shouldBe Some("C-ME")
      tested.findMatchingKey("http://www.tax.service.hmrc.gov.uk/test/ME/bar/some") shouldBe Some("B-ME")
      tested.findMatchingKey("http://www.tax.service.hmrc.gov.uk/test/ME") shouldBe None
      tested.findMatchingKey("/some/test/ME/12616276") shouldBe Some("A-ME")
      tested.findMatchingKey("http://www.tax.service.hmrc.gov.uk/test/ME/TOO") shouldBe Some("E-TOO-ME")
      tested.findMatchingKey("/test/ME/TOO/") shouldBe None
    }

    "match URI to known pattern and produce key with placeholders replaced" in {
      val tested = new MonitoringKeyMatcher {
        override val keyToPatternMapping: Seq[(String, String)] = Seq(
          "activeCesaRelationship-utr-{utr}" -> "/activeCesaRelationship/utr/:utr/saAgentReference/:saAgentReference",
          "no-of-IR-PAYE-clients" -> "/acceptableNumberOfClients/service/IR-PAYE",
          "no-of-IR-SA-clients" -> "/acceptableNumberOfClients/service/IR-SA"
        )
      }
      tested.findMatchingKey("http://agent-assurance.protected.mdtp/activeCesaRelationship/utr/1234/saAgentReference/0000") shouldBe Some("activeCesaRelationship-utr-1234")
      tested.findMatchingKey("http://agent-assurance.protected.mdtp/acceptableNumberOfClients/service/IR-PAYE") shouldBe Some("no-of-IR-PAYE-clients")
      tested.findMatchingKey("http://agent-assurance.protected.mdtp/acceptableNumberOfClients/service/IR-SA") shouldBe Some("no-of-IR-SA-clients")
    }

    "parse Routes and produce monitoring key-pattern pairs" in {
      val app = GuiceApplicationBuilder().build()

      val tested = new MonitoringKeyMatcher {
        override val keyToPatternMapping: Seq[(String, String)] = KeyToPatternMappingFromRoutes(app.injector.instanceOf[Routes], Set("service"))
      }

      tested.findMatchingKey("http://agent-assurance.protected.mdtp/activeCesaRelationship/utr/1234/saAgentReference/000") shouldBe Some("__activeCesaRelationship__utr__:__saAgentReference__:")
      tested.findMatchingKey("http://agent-assurance.protected.mdtp/activeCesaRelationship/nino/1234/saAgentReference/000") shouldBe Some("__activeCesaRelationship__nino__:__saAgentReference__:")
      tested.findMatchingKey("http://agent-assurance.protected.mdtp/acceptableNumberOfClients/service/IR-PAYE") shouldBe Some("__acceptableNumberOfClients__service__IR-PAYE")
      tested.findMatchingKey("http://agent-assurance.protected.mdtp/acceptableNumberOfClients/service/IR-SA") shouldBe Some("__acceptableNumberOfClients__service__IR-SA")
    }

  }
}
