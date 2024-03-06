package uk.gov.hmrc.agentassurance.repositories

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.agentassurance.models.{UkAmlsEntity, UkAmlsDetails}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class AmlsRepositoryISpec extends PlaySpec with DefaultPlayMongoRepositorySupport[UkAmlsEntity] {

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

      val amlsEntity = UkAmlsEntity(Some(utr), newUkAmlsDetails, Some(arn), today)

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
      val newAmlsEntity = UkAmlsEntity(Some(utr), newUkAmlsDetails, Some(arn), today)
      val oldAmlsEntity = UkAmlsEntity(Some(utr), oldUkAmlsDetails, Some(arn), LocalDate.parse("2020-01-01"))

      repository.collection.find().toFuture().futureValue.size mustBe 0

      repository.collection.insertOne(oldAmlsEntity).toFuture().futureValue
      val result = repository.createOrUpdate(arn, newAmlsEntity).futureValue
      result mustBe Some(oldAmlsEntity)

      val checkResult = repository.collection.find().toFuture().futureValue
      checkResult.size mustBe 1
      checkResult.head mustBe newAmlsEntity.copy(createdOn = today)
    }
  }


}
