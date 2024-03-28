package uk.gov.hmrc.agentassurance.repositories

import com.google.inject.AbstractModule
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentassurance.models.{AmlsSource, UkAmlsDetails, UkAmlsEntity}
import uk.gov.hmrc.agentassurance.support.InstantClockTestSupport
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Clock, LocalDate}
import scala.concurrent.ExecutionContext.Implicits.global

class AmlsRepositoryISpec extends PlaySpec with DefaultPlayMongoRepositorySupport[UkAmlsEntity] with GuiceOneServerPerSuite with InstantClockTestSupport {
  override implicit lazy val app: Application = appBuilder.build()

  val moduleWithOverrides: AbstractModule = new AbstractModule() {
    override def configure(): Unit = {
      bind(classOf[Clock]).toInstance(clock)
    }
  }

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(moduleWithOverrides)
  override lazy val repository = new AmlsRepositoryImpl(mongoComponent)

  val arn = Arn("TARN0000001")
  val utr = Utr("1234567890")
  val today = LocalDate.now()
  val newUkAmlsDetails = UkAmlsDetails(
    supervisoryBody = "ICAEW",
    membershipNumber = Some("XX1234"),
    appliedOn = None,
    membershipExpiresOn = Some(LocalDate.parse(s"${today.getYear}-12-31")))


  "createOrUpdate" should {
    "create a new record when none currently exists" in {

      val amlsEntity = UkAmlsEntity(utr = Some(utr), amlsDetails = newUkAmlsDetails, arn = Some(arn), createdOn = today, amlsSource = AmlsSource.Subscription)

      val result = repository.createOrUpdate(arn, amlsEntity).futureValue
      result mustBe None

      val checkResult =  repository.collection.find().toFuture().futureValue
      checkResult.size mustBe 1
      checkResult.head mustBe amlsEntity.copy(createdOn = today)
    }

    "replace an existing record with the new AMLS details and return the old record" in {

      val oldUkAmlsDetails = UkAmlsDetails(
        supervisoryBody = "ACCA",
        membershipNumber = Some("ABC123"),
        appliedOn = None,
        membershipExpiresOn = Some(LocalDate.parse("2020-12-31")))
      val newAmlsEntity = UkAmlsEntity(utr = Some(utr), amlsDetails = newUkAmlsDetails, arn = Some(arn), createdOn = today, amlsSource = AmlsSource.Subscription)
      val oldAmlsEntity = UkAmlsEntity(utr = Some(utr), amlsDetails = oldUkAmlsDetails, arn = Some(arn),
        createdOn = LocalDate.parse("2020-01-01"), amlsSource = AmlsSource.Subscription)

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
        val amlsEntity = UkAmlsEntity(utr = Some(utr), amlsDetails = newUkAmlsDetails, arn = Some(arn), createdOn = today, amlsSource = AmlsSource.Subscription)

        repository.collection.insertOne(amlsEntity).toFuture().futureValue

        val result = repository.getUtr(arn).futureValue

        result mustBe Some(utr)

      }

      "return None" in {
        val amlsEntity = UkAmlsEntity(utr = None, amlsDetails = newUkAmlsDetails, arn = Some(arn), createdOn = today, amlsSource = AmlsSource.Subscription)

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
          membershipExpiresOn = Some(LocalDate.parse("2020-12-31")))
        val oldAmlsEntity = UkAmlsEntity(utr = Some(utr), amlsDetails = oldUkAmlsDetails, arn = Some(arn),
          createdOn = LocalDate.parse("2020-01-01"), amlsSource = AmlsSource.Subscription)

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
