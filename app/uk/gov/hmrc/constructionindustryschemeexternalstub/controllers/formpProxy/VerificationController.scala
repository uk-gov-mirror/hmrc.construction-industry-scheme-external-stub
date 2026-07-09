/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.constructionindustryschemeexternalstub.controllers.formpProxy

import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.constructionindustryschemeexternalstub.models.requests.AuthenticatedRequest
import uk.gov.hmrc.constructionindustryschemeexternalstub.actions.AuthAction
import uk.gov.hmrc.constructionindustryschemeexternalstub.utils.{EnrolmentsHelper, ResourceHelper}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.constructionindustryschemeexternalstub.models.requests.{CreateSubmissionAndUpdateVerificationsRequest, CreateVerificationBatchAndVerificationsRequest, ModifyVerificationsRequest, ProcessVerificationResponseFromChrisRequest, UpdateVerificationSubmissionRequest}
import uk.gov.hmrc.constructionindustryschemeexternalstub.models.{CreateVerifications, DeleteVerifications}

import javax.inject.Inject

class VerificationController @Inject() (
  authorise: AuthAction,
  resourceHelper: ResourceHelper,
  enrolmentHelper: EnrolmentsHelper,
  cc: ControllerComponents
)() extends BackendController(cc)
    with Logging {

  private val verificationResponsePath                                                     = "/resources/verification"
  private val getNewestVerificationBatch_200_ResponsePath                                  =
    s"$verificationResponsePath/getNewestVerificationBatch-200-response.json"
  private val getCurrentVerificationBatch_200_verificationBatchStatus_none_ResponsePath    =
    s"$verificationResponsePath/getCurrentVerificationBatch-200-verificationBatchStatus-none-response.json"
  private val getCurrentVerificationBatch_200_verificationBatchStatus_started_ResponsePath =
    s"$verificationResponsePath/getCurrentVerificationBatch-200-verificationBatchStatus-started-response.json"
  private val createVerificationBatchAndVerifications_201_ResponsePath                     =
    s"$verificationResponsePath/createVerificationBatchAndVerifications-201-response.json"
  private val createSubmissionForVerification_201_ResponsePath                             =
    s"$verificationResponsePath/createSubmissionForVerification-201-response.json"
  private val getSubmissionWithVerificationBatch_200_ResponsePath                          =
    s"$verificationResponsePath/getSubmissionWithVerificationBatch-200-response.json"

  private def withEnrolmentDispatch(onSuccess: => Result)(implicit request: AuthenticatedRequest[_]): Result =
    enrolmentHelper.contractorEnrolmentsOpt(request) match {
      case Some(enrolmentReference) =>
        enrolmentReference.taxOfficeNumber match {
          case "500" => InternalServerError(Json.obj("message" -> "Unexpected error"))
          case "502" => BadGateway(Json.obj("message" -> "formp failed"))
          case _     => onSuccess
        }
      case None                     =>
        enrolmentHelper.agentEnrolmentsOpt(request) match {
          case Some(_) => onSuccess
          case None    => InternalServerError
        }
    }

  def getNewestVerificationBatch(instanceId: String): Action[AnyContent] =
    authorise { implicit request =>
      withEnrolmentDispatch(
        Ok(Json.parse(resourceHelper.resourceAsString(getNewestVerificationBatch_200_ResponsePath)))
      )
    }

  def getCurrentVerificationBatch(instanceId: String): Action[AnyContent] =
    authorise { implicit request =>
      withEnrolmentDispatch(
        if (instanceId == "1")
          Ok(
            Json.parse(
              resourceHelper.resourceAsString(
                getCurrentVerificationBatch_200_verificationBatchStatus_started_ResponsePath
              )
            )
          )
        else
          Ok(
            Json.parse(
              resourceHelper.resourceAsString(getCurrentVerificationBatch_200_verificationBatchStatus_none_ResponsePath)
            )
          )
      )
    }

  def createVerificationBatchAndVerifications(): Action[JsValue] =
    authorise(parse.json) { implicit request =>
      request.body
        .validate[CreateVerificationBatchAndVerificationsRequest]
        .fold(
          errs => BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs))),
          _ =>
            withEnrolmentDispatch(
              Created(
                Json.parse(resourceHelper.resourceAsString(createVerificationBatchAndVerifications_201_ResponsePath))
              )
            )
        )
    }

  def modifyVerifications(): Action[JsValue] =
    authorise(parse.json) { implicit request =>
      request.body
        .validate[ModifyVerificationsRequest]
        .fold(
          errs => BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs))),
          req => withEnrolmentDispatch(modifyVerificationsResult(req))
        )
    }

  private def modifyVerificationsResult(request: ModifyVerificationsRequest): Result =
    validateModifyVerificationsRequest(request) match {
      case Left(errorMessage) => InternalServerError(Json.obj("message" -> "Unexpected error"))

      case Right(validRequest) =>
        if (hasNoSubcontractorToModify(validRequest)) {
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        } else {
          NoContent
        }
    }

  private def validateModifyVerificationsRequest(
    request: ModifyVerificationsRequest
  ): Either[String, ModifyVerificationsRequest] =
    request match {
      case ModifyVerificationsRequest(_, Some(DeleteVerifications(verificationResourceReferences)), _)
          if verificationResourceReferences.isEmpty =>
        Left("verificationResourceReferences must not be empty when deleteVerifications is provided")

      case ModifyVerificationsRequest(_, _, Some(CreateVerifications(_, verificationResourceReferences, _)))
          if verificationResourceReferences.isEmpty =>
        Left("verificationResourceReferences must not be empty when createVerifications is provided")

      case _ =>
        Right(request)
    }

  private def hasNoSubcontractorToModify(request: ModifyVerificationsRequest): Boolean =
    request.deleteVerifications.isEmpty && request.createVerifications.isEmpty

  def createSubmissionAndUpdateVerifications(): Action[JsValue] =
    authorise(parse.json) { implicit request =>
      request.body
        .validate[CreateSubmissionAndUpdateVerificationsRequest]
        .fold(
          errs => BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs))),
          _ =>
            withEnrolmentDispatch(
              Created(Json.parse(resourceHelper.resourceAsString(createSubmissionForVerification_201_ResponsePath)))
            )
        )
    }

  def updateVerificationSubmission(): Action[JsValue] =
    authorise(parse.json) { implicit request =>
      request.body
        .validate[UpdateVerificationSubmissionRequest]
        .fold(
          errs => BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs))),
          _ => withEnrolmentDispatch(NoContent)
        )
    }

  def processVerificationResponseFromChris(): Action[JsValue] =
    authorise(parse.json) { implicit request =>
      request.body
        .validate[ProcessVerificationResponseFromChrisRequest]
        .fold(
          errs => BadRequest(Json.obj("message" -> "Invalid payload", "errors" -> JsError.toJson(errs))),
          _ => withEnrolmentDispatch(NoContent)
        )
    }

  def getSubmissionWithVerificationBatch(
    instanceId: String,
    verificationBatchResourceRef: Long
  ): Action[AnyContent] =
    authorise { implicit request =>
      enrolmentHelper.contractorEnrolmentsOpt(request) match {
        case Some(enrolmentReference) =>
          (enrolmentReference.taxOfficeNumber, enrolmentReference.taxOfficeReference) match {
            case ("500", _) =>
              InternalServerError(Json.obj("message" -> "Unexpected error"))

            case ("502", _) =>
              BadGateway(Json.obj("message" -> "formp failed"))

            case _ =>
              Ok(
                Json.parse(
                  resourceHelper.resourceAsString(
                    getSubmissionWithVerificationBatch_200_ResponsePath
                  )
                )
              )
          }

        case None =>
          enrolmentHelper.agentEnrolmentsOpt(request) match {
            case Some(_) =>
              Ok(
                Json.parse(
                  resourceHelper.resourceAsString(
                    getSubmissionWithVerificationBatch_200_ResponsePath
                  )
                )
              )

            case None =>
              InternalServerError
          }
      }
    }

}
