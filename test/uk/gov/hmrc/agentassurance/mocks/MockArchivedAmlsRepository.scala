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

package uk.gov.hmrc.agentassurance.mocks

import scala.concurrent.Future

import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.agentassurance.models.AmlsError
import uk.gov.hmrc.agentassurance.models.ArchivedAmlsEntity
import uk.gov.hmrc.agentassurance.repositories.ArchivedAmlsRepository

trait MockArchivedAmlsRepository extends MockFactory { this: TestSuite =>

  val mockArchivedAmlsRepository = mock[ArchivedAmlsRepository]
  def mockCreate(entity: ArchivedAmlsEntity)(response: Either[AmlsError, Unit]) = {
    (mockArchivedAmlsRepository
      .create(_: ArchivedAmlsEntity))
      .expects(entity)
      .returning(Future.successful(response))
  }

}
