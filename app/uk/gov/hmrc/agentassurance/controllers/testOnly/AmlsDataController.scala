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

package uk.gov.hmrc.agentassurance.controllers.testOnly

import play.api.libs.json.{Format, JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentassurance.auth.AuthActions
import uk.gov.hmrc.agentassurance.models.{CreateAmlsRequest, OverseasAmlsDetails, OverseasAmlsEntity, UkAmlsDetails}
import uk.gov.hmrc.agentassurance.repositories.{AmlsRepository, OverseasAmlsRepository}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.math.random
class AmlsDataController @Inject()(overseasAmlsRepository: OverseasAmlsRepository,
                                   cc: ControllerComponents,
                                   amlsRepository: AmlsRepository,
                                   override val authConnector: AuthConnector
                                  )(implicit ex: ExecutionContext) extends BackendController(cc) with AuthActions {
//I want to be able to update:
// - UK or overseas
// - supervisoryBody (HMRC or other)
// - membershipNumber - affects stubbed status (so supply status  approved "XAML00000200000" pending "XAML00000100000"
// - expiry date:
//  -no date (overseas or pending)
//  -expired
//  -6 months future
//
// - ARN matches agent user

// supply the status we want eg. NoAMLSDetailsUK, NoAMLSDetailsNonUK, ExpiredAMLSDetailsUK, ValidAMLSDetailsUK
// HMRC or other


// source / created date for overseas
  case class AmlsDataRequest(isUk: Boolean, status: String, isHmrc: Boolean)

  object AmlsDataRequest {
    implicit val format: Format[AmlsDataRequest] = Json.format[AmlsDataRequest]
  }
  /*
  * {
  *   isUk: true
  *   status: "thing",
  *   isHmrc: true
  * }
  *
  * */

  def addAmlsData(): Action[AnyContent] = AuthorisedWithArn { request =>arn: Arn =>
    request.body.asText.map(s => Json.parse(s).validate[AmlsDataRequest]) match {
      case Some(JsSuccess(amlsRequest, _)) =>
        if(amlsRequest.isUk) {
          amlsRepository.createOrUpdate(
            CreateAmlsRequest(
              Utr(random.toString),
              UkAmlsDetails(
                supervisoryBody = "test",
                membershipNumber = Some("XAML00000200000"),
                appliedOn = None,
                membershipExpiresOn = None)
            )
          ).map(_ => Created)
        } else {
          overseasAmlsRepository.create(
            OverseasAmlsEntity(
              arn,
              OverseasAmlsDetails(
                supervisoryBody = "AMLS Body 101",
                membershipNumber = Some("12345")
              )
            )
          ).map(_ => Created)
        }
      case _ => Future successful BadRequest
    }

  }

}
