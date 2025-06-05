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

package test.uk.gov.hmrc.agentassurance.repositories

import java.time.LocalDate

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatestplus.play.PlaySpec
import test.uk.gov.hmrc.agentassurance.support.InstantClockTestSupport
import uk.gov.hmrc.agentassurance.models.OverseasAmlsDetails
import uk.gov.hmrc.agentassurance.models.OverseasAmlsEntity
import uk.gov.hmrc.agentassurance.repositories.OverseasAmlsRepositoryImpl
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

class OverseasAmlsRepositoryISpec
extends PlaySpec
with DefaultPlayMongoRepositorySupport[OverseasAmlsEntity]
with InstantClockTestSupport {

  override lazy val repository = new OverseasAmlsRepositoryImpl(mongoComponent)

  val arn = Arn("TARN0000001")
  val today = LocalDate.now()
  val newOverseasAmlsDetails = OverseasAmlsDetails(
    supervisoryBody = "Australian AC",
    membershipNumber = Some("ii77")
  )

  "createOrUpdate" should {
    "create a new record when none currently exists" in {

      val amlsEntity = OverseasAmlsEntity(
        arn,
        newOverseasAmlsDetails,
        None
      )

      val result = repository.createOrUpdate(amlsEntity).futureValue
      result mustBe None

      val checkResult = repository.collection.find().toFuture().futureValue
      checkResult.size mustBe 1
      checkResult.head mustBe amlsEntity.copy(createdDate = Some(frozenInstant))

    }

    "replace an existing record with the new AMLS details and return the old record" in {

      val oldOverseasAmlsDetails = OverseasAmlsDetails(supervisoryBody = "Auckland ARP", membershipNumber = None)

      repository.collection.insertOne(OverseasAmlsEntity(
        arn,
        oldOverseasAmlsDetails,
        None
      )).toFuture().futureValue

      val result =
        repository.createOrUpdate(OverseasAmlsEntity(
          arn,
          newOverseasAmlsDetails,
          None
        )).futureValue
      result mustBe Some(OverseasAmlsEntity(
        arn,
        oldOverseasAmlsDetails,
        None
      ))

      val checkResult = repository.collection.find().toFuture().futureValue
      checkResult.size mustBe 1
      checkResult.head mustBe OverseasAmlsEntity(
        arn,
        newOverseasAmlsDetails,
        Some(frozenInstant)
      )
    }
  }

}
