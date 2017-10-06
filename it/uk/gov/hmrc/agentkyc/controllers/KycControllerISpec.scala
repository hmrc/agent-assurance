package uk.gov.hmrc.agentkyc.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Ignore
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Ignore
class KycControllerISpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite {
  val authConnector =  mock[AuthConnector]
  val controller = new KycController(authConnector)

  implicit val hc = new HeaderCarrier

  val agentEnrolment = Set(
    Enrolment("IR-SA-AGENT", Seq(EnrolmentIdentifier("IRAgentReference", "123")), state = "activated", delegatedAuthRule = None)
  )

  val enr = Enrolments(agentEnrolment)

  "KycController" should {
    "return Status: OK Body: empty" in {
      when(authConnector.authorise(any[Predicate],any[Retrieval[Enrolments]])(any(), any())).thenReturn(Future.successful(enr))

      val response = controller.enrolledForIrSAAgent()(FakeRequest())

      status(response) mustBe NO_CONTENT
    }
  }
}
