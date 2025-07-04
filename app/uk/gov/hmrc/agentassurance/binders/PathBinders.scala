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

package uk.gov.hmrc.agentassurance.binders

import play.api.mvc.PathBindable
import uk.gov.hmrc.agentassurance.models.utrcheck.CollectionName
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.domain.SaAgentReference

object PathBinders {

  implicit object NinoBinder
  extends SimpleObjectBinder[Nino](Nino.apply, _.value)
  implicit object SaAgentReferenceBinder
  extends SimpleObjectBinder[SaAgentReference](SaAgentReference.apply, _.value)

  implicit val utrBinder: PathBindable[Utr] =
    new PathBindable[Utr] {
      override def bind(
        key: String,
        value: String
      ): Either[String, Utr] = {
        if (Utr.isValid(value))
          Right(Utr(value))
        else
          Left("Invalid UTR")
      }

      override def unbind(
        key: String,
        utr: Utr
      ): String = utr.value
    }

  implicit val arnBinder: PathBindable[Arn] =
    new PathBindable[Arn] {
      override def bind(
        key: String,
        value: String
      ): Either[String, Arn] = {
        if (Arn.isValid(value))
          Right(Arn(value))
        else
          Left("Invalid ARN")
      }

      override def unbind(
        key: String,
        arn: Arn
      ): String = arn.value
    }

  implicit val utrCheckTypeBinder: PathBindable[CollectionName] =
    new PathBindable[CollectionName] {
      override def bind(
        key: String,
        value: String
      ): Either[String, CollectionName] = {
        CollectionName.fromString(value).toRight(s"Invalid UTR access control: $value")
      }

      override def unbind(
        key: String,
        utrAc: CollectionName
      ): String = utrAc.toString
    }

}
