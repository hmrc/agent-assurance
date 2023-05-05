/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentassurance.models

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import play.api.libs.json.{Format, Json, OFormat, _}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}

case class RegisteredDetails(
                              membershipNumber: String,
                              membershipExpiresOn: Option[LocalDate], // optional due to APB-5382
                              amlsSafeId: Option[String] = None,
                              agentBPRSafeId: Option[String] = None){
  val safeIdsMatch: Option[Boolean] = amlsSafeId.flatMap(amls => agentBPRSafeId.map(_ == amls))
}

object RegisteredDetails {
  implicit val format = Json.format[RegisteredDetails]
}

case class PendingDetails(applicationReferenceNumber: Option[String], appliedOn: Option[LocalDate]) // optional due to APB-5382

object PendingDetails {
  implicit val format = Json.format[PendingDetails]
}

case class AmlsDetails(supervisoryBody: String, details: Either[PendingDetails, RegisteredDetails])

object AmlsDetails {

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  implicit val format = new Format[AmlsDetails] {
    override def reads(json: JsValue): JsResult[AmlsDetails] = {
      val supervisoryBody = (json \ "supervisoryBody").as[String]

      val mayBeMembershipNumber = (json \ "membershipNumber").asOpt[String]

      val amlsSafeId = (json \ "amlsSafeId").asOpt[String]

      val agentBPRSafeId = (json \ "agentBPRSafeId").asOpt[String]

      mayBeMembershipNumber match {

        case Some(membershipNumber) =>
          val membershipExpiresOn = (json \ "membershipExpiresOn").asOpt[String].map(LocalDate.parse(_, formatter))
          JsSuccess(AmlsDetails(supervisoryBody, Right(RegisteredDetails(membershipNumber, membershipExpiresOn, amlsSafeId, agentBPRSafeId))))

        case None =>
          val applicationReferenceNumber = (json \ "applicationReferenceNumber").asOpt[String]
          val appliedOn = (json \ "appliedOn").asOpt[String].map(LocalDate.parse(_, formatter))
          JsSuccess(AmlsDetails(supervisoryBody, Left(PendingDetails(applicationReferenceNumber, appliedOn))))
      }
    }

    override def writes(amlsDetails: AmlsDetails): JsValue = {

      val detailsJson = amlsDetails.details match {
        case Right(registeredDetails) => Json.toJson(registeredDetails)
        case Left(pendingDetails) => Json.toJson(pendingDetails)
      }

      Json.obj("supervisoryBody" -> amlsDetails.supervisoryBody).deepMerge(detailsJson.as[JsObject])
    }
  }
}

case class CreateAmlsRequest(utr: Utr, amlsDetails: AmlsDetails)

object CreateAmlsRequest {
  implicit val format: Format[CreateAmlsRequest] = Json.format[CreateAmlsRequest]
}

case class AmlsEntity(utr: Utr, amlsDetails: AmlsDetails, arn: Option[Arn] = None, createdOn: LocalDate, updatedArnOn: Option[LocalDate] = None)

object AmlsEntity {
  implicit val amlsEntityFormat: OFormat[AmlsEntity] = Json.format[AmlsEntity]
}