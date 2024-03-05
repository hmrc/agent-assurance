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

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmlsDataController @Inject()(overseasAmlsRepository: OverseasAmlsRepository,
                                   cc: ControllerComponents,
                                   amlsRepository: AmlsRepository,
                                   override val authConnector: AuthConnector
                                  )(implicit ex: ExecutionContext) extends BackendController(cc) with AuthActions {

  case class AmlsDataRequest(
                              isUk: Boolean,
                              membershipNumber: String, // if pending membership number could be blank - isExpired would be None
                              isHmrc: Boolean, // ignored if isUK = false
                              isExpired: Option[Boolean] // ignored if isUK = false
                            )

  object AmlsDataRequest {
    implicit val format: Format[AmlsDataRequest] = Json.format[AmlsDataRequest]
  }

  def addAmlsData(): Action[AnyContent] = AuthorisedWithArn { request => arn: Arn =>
    request.body.asText.map(s => Json.parse(s).validate[AmlsDataRequest]) match {
      case Some(JsSuccess(amlsRequest, _)) =>
        if(amlsRequest.isUk) {
          createUkAmlsRecord(arn, amlsRequest.membershipNumber, amlsRequest.isHmrc, amlsRequest.isExpired).map {
            case Right(_) => Created
            case _ => InternalServerError
          }
        } else {
          createOverseasAmlsRecord(arn, amlsRequest.membershipNumber).map {
            case Right(_) => Created
            case _ => InternalServerError
          }
        }
      case _ => Future successful BadRequest
    }

  }

  private def createUkAmlsRecord(
                                  arn: Arn,
                                  membershipNumber: String,
                                  isHmrc: Boolean,
                                  isExpired: Option[Boolean]
                                ) = {
    val body = if(isHmrc) "HM Revenue and Customs (HMRC)" else "Law Society of Scotland"
    val maybeMembershipNumber = if(membershipNumber.isEmpty) None else Some(membershipNumber)
    val dateExpiredOn = isExpired match {
      case Some(true) => Some(LocalDate.now())
      case Some(false) => Some(LocalDate.now().plusMonths(12))
      case _ => None
    }
    val utr: String = Math.random().*(1000000000).toString.substring(2,12)

    amlsRepository.createOrUpdate(
      CreateAmlsRequest(
        Utr(utr),
        UkAmlsDetails(
          supervisoryBody = body,
          membershipNumber = maybeMembershipNumber,
          appliedOn = None,
          membershipExpiresOn = dateExpiredOn
        )
      )
    ).flatMap(_ => amlsRepository.updateArn(Utr(utr), arn))

  }

  private def createOverseasAmlsRecord(arn: Arn, membershipNumber: String) = {
    val maybeMembershipNumber = if(membershipNumber.isEmpty) None else Some(membershipNumber)

    overseasAmlsRepository.create(
      OverseasAmlsEntity(
        arn,
        OverseasAmlsDetails(
          supervisoryBody = "AMLS Body 101",
          membershipNumber = maybeMembershipNumber
        )
      )
    )
  }



}
