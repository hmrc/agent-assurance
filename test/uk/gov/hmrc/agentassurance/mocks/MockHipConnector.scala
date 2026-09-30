package uk.gov.hmrc.agentassurance.mocks

import scala.concurrent.Future
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq as equal
import org.mockito.Mockito.when
import org.scalatest.TestSuite
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.agentassurance.connectors.HipConnector
import uk.gov.hmrc.http.HeaderCarrier

trait MockHipConnector
  extends MockitoSugar { this: TestSuite =>

  val mockHipConnector: HipConnector = mock[HipConnector]

  def mockHipGetBusinessNameRecord(
                                    utr: String
                                  )(response: Option[String]): Unit =
    when(
      mockHipConnector
        .getBusinessName(equal(utr))(using any[HeaderCarrier])
    ).thenReturn(Future.successful(response))

}
