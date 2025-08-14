/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.agentassurance.models.Service.HMRCCGTPD
import uk.gov.hmrc.agentassurance.models.Service.HMRCMTDIT
import uk.gov.hmrc.agentassurance.models.Service.HMRCMTDITSUPP

class SuspensionDetailsSpec
extends AnyWordSpec
with Matchers {

  val suspensionStatusTrue = true
  val inputRegimes = Set("aRegime", "bRegime")

  "Suspended regimes" should {
    "be empty set when input regimes set is not present" in {
      val regimes = None

      val suspensionDetails = SuspensionDetails(suspensionStatusTrue, regimes)

      suspensionDetails.suspendedRegimes shouldBe Set.empty[String]
    }

    "be empty set when input regimes set is empty" in {
      val regimes = Some(Set.empty[String])

      val suspensionDetails = SuspensionDetails(suspensionStatusTrue, regimes)

      suspensionDetails.suspendedRegimes shouldBe Set.empty[String]
    }

    "be all valid suspension regimes when input regimes contains 'ALL' or 'AGSV'" in {
      SuspensionDetails(
        suspensionStatusTrue,
        Some(inputRegimes + "ALL")
      ).suspendedRegimes shouldBe SuspensionDetails.validSuspensionRegimes
      SuspensionDetails(
        suspensionStatusTrue,
        Some(inputRegimes + "AGSV")
      ).suspendedRegimes shouldBe SuspensionDetails.validSuspensionRegimes
    }

    "match provided regimes when input does not contain either 'ALL' or 'AGSV'" in {
      SuspensionDetails(suspensionStatusTrue, Some(inputRegimes)).suspendedRegimes shouldBe Set("aRegime", "bRegime")
    }

  }

  "isRegimeSuspended" should {

    def getServiceOfRegimeName(regimeName: String): Service = SuspensionDetails.serviceToRegime.find(_._2 == regimeName).get._1

    "return false when input regime names do not match well-defined regime names AND input regime names do not contain either 'ALL' or 'AGSV'" in {
      val regimes = Some(inputRegimes)

      val suspensionDetails = SuspensionDetails(suspensionStatusTrue, regimes = regimes)

      SuspensionDetails.validSuspensionRegimes.foreach { regimeName =>
        val service = getServiceOfRegimeName(regimeName)

        suspensionDetails.isRegimeSuspended(service) shouldBe false
      }
    }

    "return true when input regime names do not match well-defined regime names BUT input regime names contains 'ALL' or 'AGSV'" in {
      SuspensionDetails.validSuspensionRegimes.foreach { regimeName =>
        val service = getServiceOfRegimeName(regimeName)

        SuspensionDetails(suspensionStatusTrue, regimes = Some(inputRegimes + "ALL"))
          .isRegimeSuspended(service) shouldBe true
        SuspensionDetails(suspensionStatusTrue, regimes = Some(inputRegimes + "AGSV"))
          .isRegimeSuspended(service) shouldBe true
      }
    }

    "return true when input regimes include well-defined values" in {
      SuspensionDetails.serviceToRegime.keySet.foreach { service =>
        val suspensionDetails = SuspensionDetails(suspensionStatusTrue, regimes = Some(Set(SuspensionDetails.serviceToRegime(service))))

        suspensionDetails.isRegimeSuspended(service) shouldBe true
      }
    }

    "return true when the regime is suspended" in {
      SuspensionDetails(suspensionStatus = true, Some(Set("ITSA", "VATC"))).isRegimeSuspended(HMRCMTDIT) shouldBe true
    }

    "return true when ALL regimes are suspended" in {
      SuspensionDetails(suspensionStatus = true, Some(Set("ALL"))).isRegimeSuspended(HMRCMTDIT) shouldBe true
    }

    "return false when the regime is not suspended" in {
      SuspensionDetails(suspensionStatus = true, Some(Set("ITSA", "VATC")))
        .isRegimeSuspended(HMRCCGTPD) shouldBe false
    }
  }

  "suspendedRegimesForServices" should {
    "return only the suspended regimes" in {
      SuspensionDetails(suspensionStatus = true, Some(Set("ITSA", "VATC")))
        .suspendedRegimesForServices(Set(
          HMRCMTDIT,
          HMRCMTDITSUPP,
          HMRCCGTPD
        )) shouldBe Set("ITSA")
    }

    "return all of the regimes if ALL are suspended" in {
      SuspensionDetails(suspensionStatus = true, Some(Set("ALL")))
        .suspendedRegimesForServices(Set(HMRCMTDIT, HMRCCGTPD)) shouldBe Set("ITSA", "CGT")
    }
  }

  "isAnyRegimeSuspendedForServices" should {
    "return true if the agent is suspended for any regimes in the consents" in {
      SuspensionDetails(suspensionStatus = true, Some(Set("ITSA", "VATC")))
        .isAnyRegimeSuspendedForServices(Set(HMRCMTDITSUPP)) shouldBe true
    }
    "return false if the agent is not suspended for any service in the consents" in {
      SuspensionDetails(suspensionStatus = true, Some(Set("ITSA", "VATC")))
        .isAnyRegimeSuspendedForServices(Set(HMRCCGTPD)) shouldBe false
    }
  }

}
