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

import java.time.Clock
import java.time.LocalDate

import scala.concurrent.ExecutionContext.Implicits.global

import com.google.inject.AbstractModule
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application
import test.uk.gov.hmrc.agentassurance.support.InstantClockTestSupport
import uk.gov.hmrc.agentassurance.models.AmlsSource
import uk.gov.hmrc.agentassurance.models.UkAmlsDetails
import uk.gov.hmrc.agentassurance.models.UkAmlsEntity
import uk.gov.hmrc.agentassurance.repositories.AmlsRepositoryImpl
import uk.gov.hmrc.agentassurance.models.Arn
import uk.gov.hmrc.agentassurance.models.Utr
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

class AmlsRepositoryISpec
extends PlaySpec
with DefaultPlayMongoRepositorySupport[UkAmlsEntity]
with GuiceOneServerPerSuite
with InstantClockTestSupport {

  override implicit lazy val app: Application = appBuilder.build()

  val moduleWithOverrides: AbstractModule =
    new AbstractModule() {
      override def configure(): Unit = {
        bind(classOf[Clock]).toInstance(clock)
      }
    }

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
    .configure("internal-auth-token-enabled-on-start" -> false)
    .overrides(moduleWithOverrides)
  override lazy val repository = new AmlsRepositoryImpl(mongoComponent)

  val arn = Arn("TARN0000001")
  val utr = Utr("1234567890")
  val today = LocalDate.now()
  val newUkAmlsDetails = UkAmlsDetails(
    supervisoryBody = "ICAEW",
    membershipNumber = Some("XX1234"),
    appliedOn = None,
    membershipExpiresOn = Some(LocalDate.parse(s"${today.getYear}-12-31"))
  )

  "createOrUpdate" should {
    "create a new record when none currently exists" in {

      val amlsEntity = UkAmlsEntity(
        utr = Some(utr),
        amlsDetails = newUkAmlsDetails,
        arn = Some(arn),
        createdOn = today,
        amlsSource = AmlsSource.Subscription
      )

      val result = repository.createOrUpdate(arn, amlsEntity).futureValue
      result mustBe None

      val checkResult = repository.collection.find().toFuture().futureValue
      checkResult.size mustBe 1
      checkResult.head mustBe amlsEntity.copy(createdOn = today)
    }

    "replace an existing record with the new AMLS details and return the old record" in {

      val oldUkAmlsDetails = UkAmlsDetails(
        supervisoryBody = "ACCA",
        membershipNumber = Some("ABC123"),
        appliedOn = None,
        membershipExpiresOn = Some(LocalDate.parse("2020-12-31"))
      )
      val newAmlsEntity = UkAmlsEntity(
        utr = Some(utr),
        amlsDetails = newUkAmlsDetails,
        arn = Some(arn),
        createdOn = today,
        amlsSource = AmlsSource.Subscription
      )
      val oldAmlsEntity = UkAmlsEntity(
        utr = Some(utr),
        amlsDetails = oldUkAmlsDetails,
        arn = Some(arn),
        createdOn = LocalDate.parse("2020-01-01"),
        amlsSource = AmlsSource.Subscription
      )

      repository.collection.find().toFuture().futureValue.size mustBe 0

      repository.collection.insertOne(oldAmlsEntity).toFuture().futureValue
      val result = repository.createOrUpdate(arn, newAmlsEntity).futureValue
      result mustBe Some(oldAmlsEntity)

      val checkResult = repository.collection.find().toFuture().futureValue
      checkResult.size mustBe 1
      checkResult.head mustBe newAmlsEntity.copy(createdOn = today)
    }

    "getUtr" should {
      "return a utr" in {
        val amlsEntity = UkAmlsEntity(
          utr = Some(utr),
          amlsDetails = newUkAmlsDetails,
          arn = Some(arn),
          createdOn = today,
          amlsSource = AmlsSource.Subscription
        )

        repository.collection.insertOne(amlsEntity).toFuture().futureValue

        val result = repository.getUtr(arn).futureValue

        result mustBe Some(utr)

      }

      "return None" in {
        val amlsEntity = UkAmlsEntity(
          utr = None,
          amlsDetails = newUkAmlsDetails,
          arn = Some(arn),
          createdOn = today,
          amlsSource = AmlsSource.Subscription
        )

        repository.collection.insertOne(amlsEntity).toFuture().futureValue

        val result = repository.getUtr(arn).futureValue

        result mustBe None
      }
    }
  }

  "updateExpiryDate" when {
    "given a new date" should {
      "update the record" in {
        val newExpiryDate = LocalDate.now().plusWeeks(2)
        val oldUkAmlsDetails = UkAmlsDetails(
          supervisoryBody = "ACCA",
          membershipNumber = Some("ABC123"),
          appliedOn = None,
          membershipExpiresOn = Some(LocalDate.parse("2020-12-31"))
        )
        val oldAmlsEntity = UkAmlsEntity(
          utr = Some(utr),
          amlsDetails = oldUkAmlsDetails,
          arn = Some(arn),
          createdOn = LocalDate.parse("2020-01-01"),
          amlsSource = AmlsSource.Subscription
        )

        repository.collection.find().toFuture().futureValue.size mustBe 0

        repository.collection.insertOne(oldAmlsEntity).toFuture().futureValue
        val setupCheck = repository.collection.find().toFuture().futureValue
        setupCheck.size mustBe 1
        setupCheck.head mustBe oldAmlsEntity

        repository.updateExpiryDate(oldAmlsEntity.arn.get, newExpiryDate).futureValue

        val checkResult = repository.collection.find().toFuture().futureValue
        checkResult.size mustBe 1
        checkResult.head mustBe oldAmlsEntity.copy(amlsDetails = oldUkAmlsDetails.copy(membershipExpiresOn = Some(newExpiryDate)))
      }
    }
  }

}
