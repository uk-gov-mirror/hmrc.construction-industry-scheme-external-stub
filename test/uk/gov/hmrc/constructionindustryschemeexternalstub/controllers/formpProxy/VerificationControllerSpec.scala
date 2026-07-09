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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.freespec.AnyFreeSpec
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.constructionindustryschemeexternalstub.actions.FakeAuthAction
import uk.gov.hmrc.constructionindustryschemeexternalstub.base.SpecBase
import uk.gov.hmrc.constructionindustryschemeexternalstub.models.{CreateVerifications, DeleteVerifications, EmployerReference}
import uk.gov.hmrc.constructionindustryschemeexternalstub.utils.{EnrolmentsHelper, ResourceHelper}
import uk.gov.hmrc.constructionindustryschemeexternalstub.models.requests.*
import scala.concurrent.Future
import java.time.LocalDateTime

class VerificationControllerSpec extends AnyFreeSpec with SpecBase {

  private val instanceId = "123"
  private val url        = s"/cis/verification-batch/newest/$instanceId"
  private val postUrl    = "/cis/verification-batch/create"

  private val validJson: JsValue =
    Json.toJson(
      CreateVerificationBatchAndVerificationsRequest(
        instanceId = instanceId,
        verificationResourceReferences = Seq(1L, 2L),
        actionIndicator = Some("A")
      )
    )

  ".getNewestVerificationBatch" - {

    "returns 200 OK with JSON body on success (contractor enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("200", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val responseJson: JsValue = Json.parse(
        s"""
           |{
           |  "subcontractors": [
           |    { "subcontractorId": 1 }
           |  ],
           |  "verificationBatch": { "verificationBatchId": 99 },
           |  "verifications": [
           |    { "verificationId": 1001 }
           |  ],
           |  "submission": { "submissionId": 555 },
           |  "monthlyReturn": { "monthlyReturnId": 777 }
           |}
           |""".stripMargin
      )

      when(mockResourceHelper.resourceAsString(any()))
        .thenReturn(responseJson.toString())

      val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
      val res: Future[Result]                      = controller.getNewestVerificationBatch(instanceId)(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)

      val body = contentAsJson(res)

      body mustBe responseJson

      (body \ "subcontractors")(0).\("subcontractorId").as[Long] mustBe 1L

      (body \ "verificationBatch").\("verificationBatchId").as[Long] mustBe 99L
      (body \ "verifications")(0).\("verificationId").as[Long] mustBe 1001L

      (body \ "submission").\("submissionId").as[Long] mustBe 555L
      (body \ "monthlyReturn").\("monthlyReturnId").as[Long] mustBe 777L
    }

    "returns 200 OK with JSON body on success (agent enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(Some("IRAgentReference-123"))

      val responseJson: JsValue = Json.parse(
        s"""
           |{
           |  "subcontractors": [
           |    { "subcontractorId": 1 }
           |  ],
           |  "verificationBatch": { "verificationBatchId": 99 },
           |  "verifications": [
           |    { "verificationId": 1001 }
           |  ],
           |  "submission": { "submissionId": 555 },
           |  "monthlyReturn": { "monthlyReturnId": 777 }
           |}
           |""".stripMargin
      )

      when(mockResourceHelper.resourceAsString(any()))
        .thenReturn(responseJson.toString())

      val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
      val res: Future[Result]                      = controller.getNewestVerificationBatch(instanceId)(req)

      status(res) mustBe OK
      contentType(res) mustBe Some(JSON)
      contentAsJson(res) mustBe responseJson
    }

    "returns 502 BadGateway for taxOfficeNumber = 502 (contractor enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("502", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
      val res: Future[Result]                      = controller.getNewestVerificationBatch(instanceId)(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("formp failed")
    }

    "returns 500 InternalServerError for taxOfficeNumber = 500 (contractor enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("500", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
      val res: Future[Result]                      = controller.getNewestVerificationBatch(instanceId)(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }

    "returns 500 InternalServerError when no contractor enrolment and no agent enrolment found" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
      val res: Future[Result]                      = controller.getNewestVerificationBatch(instanceId)(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
    }

    ".getCurrentVerificationBatch" - {

      val instanceId = "123"
      val url        = s"/cis/verification-batch/current/$instanceId"

      "returns 200 OK with JSON body (instanceId not 1) on success (contractor enrolment)" in new Setup {
        when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
          .thenReturn(Some(EmployerReference("200", "")))
        when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
          .thenReturn(None)

        val responseJson: JsValue = Json.parse(
          s"""
             |{
             |  "subcontractors": [
             |    { "subcontractorId": 1 }
             |  ],
             |  "verificationBatch": { "verificationBatchId": 99 },
             |  "verifications": [
             |    { "verificationId": 1001 }
             |  ]
             |}
             |""".stripMargin
        )

        when(mockResourceHelper.resourceAsString(any()))
          .thenReturn(responseJson.toString())

        val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
        val res: Future[Result]                      = controller.getCurrentVerificationBatch(instanceId)(req)

        status(res) mustBe OK
        contentType(res) mustBe Some(JSON)

        val body = contentAsJson(res)

        body mustBe responseJson

        (body \ "subcontractors")(0).\("subcontractorId").as[Long] mustBe 1L

        (body \ "verificationBatch").\("verificationBatchId").as[Long] mustBe 99L
        (body \ "verifications")(0).\("verificationId").as[Long] mustBe 1001L

      }

      "returns 200 OK with JSON body (instanceId equal 1) on success (contractor enrolment)" in new Setup {
        val instanceId = "1"

        when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
          .thenReturn(Some(EmployerReference("200", "")))
        when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
          .thenReturn(None)

        val responseJson: JsValue = Json.parse(
          s"""
             |{
             |  "subcontractors": [
             |    { "subcontractorId": 1 }
             |  ],
             |  "verificationBatch": { "verificationBatchId": 99 },
             |  "verifications": [
             |    { "verificationId": 1001 }
             |  ]
             |}
             |""".stripMargin
        )

        when(mockResourceHelper.resourceAsString(any()))
          .thenReturn(responseJson.toString())

        val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
        val res: Future[Result]                      = controller.getCurrentVerificationBatch(instanceId)(req)

        status(res) mustBe OK
        contentType(res) mustBe Some(JSON)

        val body = contentAsJson(res)

        body mustBe responseJson

        (body \ "subcontractors")(0).\("subcontractorId").as[Long] mustBe 1L

        (body \ "verificationBatch").\("verificationBatchId").as[Long] mustBe 99L
        (body \ "verifications")(0).\("verificationId").as[Long] mustBe 1001L

      }

      "returns 200 OK with JSON body (instanceId not 1) on success (agent enrolment)" in new Setup {
        when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
          .thenReturn(None)
        when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
          .thenReturn(Some("IRAgentReference-123"))

        val responseJson: JsValue = Json.parse(
          s"""
             |{
             |  "subcontractors": [
             |    { "subcontractorId": 1 }
             |  ],
             |  "verificationBatch": { "verificationBatchId": 99 },
             |  "verifications": [
             |    { "verificationId": 1001 }
             |  ]
             |}
             |""".stripMargin
        )

        when(mockResourceHelper.resourceAsString(any()))
          .thenReturn(responseJson.toString())

        val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
        val res: Future[Result]                      = controller.getCurrentVerificationBatch(instanceId)(req)

        status(res) mustBe OK
        contentType(res) mustBe Some(JSON)
        contentAsJson(res) mustBe responseJson
      }

      "returns 200 OK with JSON body (instanceId equal 1) on success (agent enrolment)" in new Setup {
        val instanceId = "1"

        when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
          .thenReturn(None)
        when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
          .thenReturn(Some("IRAgentReference-123"))

        val responseJson: JsValue = Json.parse(
          s"""
             |{
             |  "subcontractors": [
             |    { "subcontractorId": 1 }
             |  ],
             |  "verificationBatch": { "verificationBatchId": 99 },
             |  "verifications": [
             |    { "verificationId": 1001 }
             |  ]
             |}
             |""".stripMargin
        )

        when(mockResourceHelper.resourceAsString(any()))
          .thenReturn(responseJson.toString())

        val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
        val res: Future[Result]                      = controller.getCurrentVerificationBatch(instanceId)(req)

        status(res) mustBe OK
        contentType(res) mustBe Some(JSON)
        contentAsJson(res) mustBe responseJson
      }

      "returns 502 BadGateway for taxOfficeNumber = 502 (contractor enrolment)" in new Setup {
        when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
          .thenReturn(Some(EmployerReference("502", "")))
        when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
          .thenReturn(None)

        val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
        val res: Future[Result]                      = controller.getCurrentVerificationBatch(instanceId)(req)

        status(res) mustBe BAD_GATEWAY
        (contentAsJson(res) \ "message").as[String] must include("formp failed")
      }

      "returns 500 InternalServerError for taxOfficeNumber = 500 (contractor enrolment)" in new Setup {
        when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
          .thenReturn(Some(EmployerReference("500", "")))
        when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
          .thenReturn(None)

        val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
        val res: Future[Result]                      = controller.getCurrentVerificationBatch(instanceId)(req)

        status(res) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
      }

      "returns 500 InternalServerError when no contractor enrolment and no agent enrolment found" in new Setup {
        when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
          .thenReturn(None)
        when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
          .thenReturn(None)

        val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
        val res: Future[Result]                      = controller.getCurrentVerificationBatch(instanceId)(req)

        status(res) mustBe INTERNAL_SERVER_ERROR
      }
    }

  }

  ".createVerificationBatchAndVerifications" - {

    "returns 201 Created with JSON body on success (contractor enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("200", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val responseJson = Json.obj("verifBatchResourceRef" -> 10)

      when(mockResourceHelper.resourceAsString(any()))
        .thenReturn(responseJson.toString())

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validJson)

      val res: Future[Result] = controller.createVerificationBatchAndVerifications()(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      contentAsJson(res) mustBe responseJson
    }

    "returns 201 Created with JSON body on success (agent enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(Some("IRAgentReference-123"))

      val responseJson = Json.obj("verifBatchResourceRef" -> 10)

      when(mockResourceHelper.resourceAsString(any()))
        .thenReturn(responseJson.toString())

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validJson)

      val res: Future[Result] = controller.createVerificationBatchAndVerifications()(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      contentAsJson(res) mustBe responseJson
    }

    "returns 400 BadRequest when JSON is invalid" in new Setup {
      val invalidJson = Json.obj("instanceId" -> instanceId) // missing required fields

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(invalidJson)

      val res: Future[Result] = controller.createVerificationBatchAndVerifications()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      (contentAsJson(res) \ "errors").isDefined mustBe true
    }

    "returns 502 BadGateway for taxOfficeNumber = 502 (contractor enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("502", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validJson)

      val res: Future[Result] = controller.createVerificationBatchAndVerifications()(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("formp failed")
    }

    "returns 500 InternalServerError for taxOfficeNumber = 500 (contractor enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("500", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validJson)

      val res: Future[Result] = controller.createVerificationBatchAndVerifications()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }

    "returns 500 InternalServerError when no contractor enrolment and no agent enrolment found" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validJson)

      val res: Future[Result] = controller.createVerificationBatchAndVerifications()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
    }
  }

  ".modifyVerifications" - {

    val postUrl = "/cis/verification-batch/modify"

    val validJson: JsValue =
      Json.toJson(
        ModifyVerificationsRequest(
          instanceId = "abc-123",
          deleteVerifications = Some(
            DeleteVerifications(
              verificationResourceReferences = Seq(111L, 222L)
            )
          ),
          createVerifications = Some(
            CreateVerifications(
              verificationBatchResourceRef = 10L,
              verificationResourceReferences = Seq(333L, 444L),
              actionIndicator = Some("A")
            )
          )
        )
      )

    "returns 204 NO_CONTENT on success delete and create verifications (contractor enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("200", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe NO_CONTENT
      contentAsString(res) mustBe ""
    }

    "returns 204 NO_CONTENT on success delete and create verifications (agent enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(Some("IRAgentReference-123"))

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe NO_CONTENT
      contentAsString(res) mustBe ""
    }

    "returns 204 NO_CONTENT on success delete verifications (contractor enrolment)" in new Setup {
      val deleteVerificationsJson: JsValue =
        Json.toJson(
          ModifyVerificationsRequest(
            instanceId = "abc-123",
            deleteVerifications = Some(
              DeleteVerifications(
                verificationResourceReferences = Seq(111L, 222L)
              )
            ),
            createVerifications = None
          )
        )

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("200", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(deleteVerificationsJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe NO_CONTENT
      contentAsString(res) mustBe ""
    }

    "returns 204 NO_CONTENT on success delete verifications (agent enrolment)" in new Setup {
      val deleteVerificationsJson: JsValue =
        Json.toJson(
          ModifyVerificationsRequest(
            instanceId = "abc-123",
            deleteVerifications = Some(
              DeleteVerifications(
                verificationResourceReferences = Seq(111L, 222L)
              )
            ),
            createVerifications = None
          )
        )

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(Some("IRAgentReference-123"))

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(deleteVerificationsJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe NO_CONTENT
      contentAsString(res) mustBe ""
    }

    "returns 204 NO_CONTENT on success create verifications with actionIndicator (contractor enrolment)" in new Setup {
      val createVerificationsJson: JsValue =
        Json.toJson(
          ModifyVerificationsRequest(
            instanceId = "abc-123",
            deleteVerifications = None,
            createVerifications = Some(
              CreateVerifications(
                verificationBatchResourceRef = 10L,
                verificationResourceReferences = Seq(333L, 444L),
                actionIndicator = Some("A")
              )
            )
          )
        )

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("200", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(createVerificationsJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe NO_CONTENT
      contentAsString(res) mustBe ""
    }

    "returns 204 NO_CONTENT on success create verifications with actionIndicator (agent enrolment)" in new Setup {
      val createVerificationsJson: JsValue =
        Json.toJson(
          ModifyVerificationsRequest(
            instanceId = "abc-123",
            deleteVerifications = None,
            createVerifications = Some(
              CreateVerifications(
                verificationBatchResourceRef = 10L,
                verificationResourceReferences = Seq(333L, 444L),
                actionIndicator = Some("A")
              )
            )
          )
        )

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(Some("IRAgentReference-123"))

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(createVerificationsJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe NO_CONTENT
      contentAsString(res) mustBe ""
    }

    "returns 204 NO_CONTENT on success create verifications without actionIndicator (contractor enrolment)" in new Setup {
      val createVerificationsJson: JsValue =
        Json.toJson(
          ModifyVerificationsRequest(
            instanceId = "abc-123",
            deleteVerifications = None,
            createVerifications = Some(
              CreateVerifications(
                verificationBatchResourceRef = 10L,
                verificationResourceReferences = Seq(333L, 444L),
                actionIndicator = None
              )
            )
          )
        )

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("200", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(createVerificationsJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe NO_CONTENT
      contentAsString(res) mustBe ""
    }

    "returns 204 NO_CONTENT on success create verifications without actionIndicator (agent enrolment)" in new Setup {
      val createVerificationsJson: JsValue =
        Json.toJson(
          ModifyVerificationsRequest(
            instanceId = "abc-123",
            deleteVerifications = None,
            createVerifications = Some(
              CreateVerifications(
                verificationBatchResourceRef = 10L,
                verificationResourceReferences = Seq(333L, 444L),
                actionIndicator = None
              )
            )
          )
        )

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(Some("IRAgentReference-123"))

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(createVerificationsJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe NO_CONTENT
      contentAsString(res) mustBe ""
    }

    "returns 400 BadRequest when JSON is invalid" in new Setup {
      val invalidJson = Json.obj() // missing required fields

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(invalidJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
      (contentAsJson(res) \ "errors").isDefined mustBe true
    }

    "returns 502 BadGateway for taxOfficeNumber = 502 (contractor enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("502", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("formp failed")
    }

    "returns 500 InternalServerError for taxOfficeNumber = 500 (contractor enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("500", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }

    "returns 500 InternalServerError when no contractor enrolment and no agent enrolment found" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
    }

    "returns 500 InternalServerError when only instanceId is present (contractor enrolment)" in new Setup {
      val missingRequiredFieldsJson: JsValue =
        Json.toJson(
          ModifyVerificationsRequest(
            instanceId = "abc-123",
            deleteVerifications = None,
            createVerifications = None
          )
        )

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("200", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(missingRequiredFieldsJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }

    "returns 500 InternalServerError when only instanceId is present (agent enrolment)" in new Setup {
      val missingRequiredFieldsJson: JsValue =
        Json.toJson(
          ModifyVerificationsRequest(
            instanceId = "abc-123",
            deleteVerifications = None,
            createVerifications = None
          )
        )

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(Some("IRAgentReference-123"))

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(missingRequiredFieldsJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }

    "returns 500 InternalServerError when deleteVerifications is provided but verificationResourceReferences is empty (contractor enrolment)" in new Setup {
      val invalidDeleteVerificationsJson: JsValue =
        Json.toJson(
          ModifyVerificationsRequest(
            instanceId = "abc-123",
            deleteVerifications = Some(
              DeleteVerifications(
                verificationResourceReferences = Seq.empty
              )
            ),
            createVerifications = Some(
              CreateVerifications(
                verificationBatchResourceRef = 10L,
                verificationResourceReferences = Seq(333L, 444L),
                actionIndicator = Some("A")
              )
            )
          )
        )

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("200", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(invalidDeleteVerificationsJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }

    "returns 500 InternalServerError when deleteVerifications is provided but verificationResourceReferences is empty (agent enrolment)" in new Setup {
      val invalidDeleteVerificationsJson: JsValue =
        Json.toJson(
          ModifyVerificationsRequest(
            instanceId = "abc-123",
            deleteVerifications = Some(
              DeleteVerifications(
                verificationResourceReferences = Seq.empty
              )
            ),
            createVerifications = Some(
              CreateVerifications(
                verificationBatchResourceRef = 10L,
                verificationResourceReferences = Seq(333L, 444L),
                actionIndicator = Some("A")
              )
            )
          )
        )

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(Some("IRAgentReference-123"))

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(invalidDeleteVerificationsJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }

    "returns 500 InternalServerError when createVerifications is provided but verificationResourceReferences is empty (contractor enrolment)" in new Setup {
      val invalidCreateVerificationsJson: JsValue =
        Json.toJson(
          ModifyVerificationsRequest(
            instanceId = "abc-123",
            deleteVerifications = Some(
              DeleteVerifications(
                verificationResourceReferences = Seq(111L, 222L)
              )
            ),
            createVerifications = Some(
              CreateVerifications(
                verificationBatchResourceRef = 10L,
                verificationResourceReferences = Seq.empty,
                actionIndicator = Some("A")
              )
            )
          )
        )

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("200", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(invalidCreateVerificationsJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }

    "returns 500 InternalServerError when when createVerifications is provided but verificationResourceReferences is empty (agent enrolment)" in new Setup {
      val invalidCreateVerificationsJson: JsValue =
        Json.toJson(
          ModifyVerificationsRequest(
            instanceId = "abc-123",
            deleteVerifications = Some(
              DeleteVerifications(
                verificationResourceReferences = Seq(111L, 222L)
              )
            ),
            createVerifications = Some(
              CreateVerifications(
                verificationBatchResourceRef = 10L,
                verificationResourceReferences = Seq.empty,
                actionIndicator = Some("A")
              )
            )
          )
        )

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(Some("IRAgentReference-123"))

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(invalidCreateVerificationsJson)

      val res: Future[Result] = controller.modifyVerifications()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }
  }

  ".createSubmissionForVerification" - {

    val postUrl = "/cis/verification-batch/submission/create"

    val validSubmissionJson: JsValue =
      Json.toJson(
        CreateSubmissionAndUpdateVerificationsRequest(
          instanceId = instanceId,
          verificationBatchId = 99L,
          verificationBatchResourceRef = 10L,
          emailRecipient = "ops@example.com",
          irMarkGenerated = Some("IR_MARK"),
          verifications = Seq(
            uk.gov.hmrc.constructionindustryschemeexternalstub.models.requests.VerificationToUpdate(
              subcontractorName = "ACME",
              verificationResourceRef = 111L,
              proceedVerification = "Y"
            ),
            uk.gov.hmrc.constructionindustryschemeexternalstub.models.requests.VerificationToUpdate(
              subcontractorName = "BETA",
              verificationResourceRef = 222L,
              proceedVerification = "N"
            )
          ),
          agentId = None
        )
      )

    "returns 201 Created with JSON body on success (contractor enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("200", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val responseJson = Json.obj("submissionId" -> 555)

      when(mockResourceHelper.resourceAsString(any()))
        .thenReturn(responseJson.toString())

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validSubmissionJson)

      val res: Future[Result] = controller.createSubmissionAndUpdateVerifications()(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      contentAsJson(res) mustBe responseJson
    }

    "returns 201 Created with JSON body on success (agent enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(Some("IRAgentReference-123"))

      val responseJson = Json.obj("submissionId" -> 555)

      when(mockResourceHelper.resourceAsString(any()))
        .thenReturn(responseJson.toString())

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validSubmissionJson)

      val res: Future[Result] = controller.createSubmissionAndUpdateVerifications()(req)

      status(res) mustBe CREATED
      contentType(res) mustBe Some(JSON)
      contentAsJson(res) mustBe responseJson
    }

    "returns 400 BadRequest when JSON is invalid" in new Setup {
      val invalidJson = Json.obj(
        "instanceId" -> instanceId
      )

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(invalidJson)

      val res: Future[Result] = controller.createSubmissionAndUpdateVerifications()(req)

      status(res) mustBe BAD_REQUEST
      contentType(res) mustBe Some(JSON)

      val body = contentAsJson(res)
      (body \ "message").as[String] mustBe "Invalid payload"
      (body \ "errors").isDefined mustBe true
    }

    "returns 502 BadGateway for taxOfficeNumber = 502 (contractor enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("502", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validSubmissionJson)

      val res: Future[Result] = controller.createSubmissionAndUpdateVerifications()(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("formp failed")
    }

    "returns 500 InternalServerError for taxOfficeNumber = 500 (contractor enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("500", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validSubmissionJson)

      val res: Future[Result] = controller.createSubmissionAndUpdateVerifications()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }

    "returns 500 InternalServerError when no contractor enrolment and no agent enrolment found" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validSubmissionJson)

      val res: Future[Result] = controller.createSubmissionAndUpdateVerifications()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
    }
  }

  ".updateVerificationSubmission" - {

    val updateVerificationSubmissionUrl = "/cis/verification/submission/update"

    val validUpdateJson: JsValue = Json.toJson(
      UpdateVerificationSubmissionRequest(
        instanceId = instanceId,
        verificationBatchId = 99L,
        verificationBatchResourceRef = 77L,
        submittableStatus = "FATAL_ERROR",
        govtalkErrorCode = Some("500"),
        govtalkErrorType = Some("timeOut"),
        govtalkErrorMessage = Some("timeOut")
      )
    )

    "returns 204 NoContent on valid payload (contractor enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("200", "")))

      val req = FakeRequest(POST, updateVerificationSubmissionUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validUpdateJson)

      val res: Future[Result] = controller.updateVerificationSubmission()(req)

      status(res) mustBe NO_CONTENT
    }

    "returns 204 NoContent on valid payload (agent enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(Some("IRAgentReference-123"))

      val req = FakeRequest(POST, updateVerificationSubmissionUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validUpdateJson)

      val res: Future[Result] = controller.updateVerificationSubmission()(req)

      status(res) mustBe NO_CONTENT
    }

    "returns 400 BadRequest when JSON is invalid" in new Setup {
      val invalidJson = Json.obj("bad" -> "data")

      val req = FakeRequest(POST, updateVerificationSubmissionUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(invalidJson)

      val res: Future[Result] = controller.updateVerificationSubmission()(req)

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "message").as[String] mustBe "Invalid payload"
    }

    "returns 500 InternalServerError when taxOfficeNumber is 500" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("500", "")))

      val req = FakeRequest(POST, updateVerificationSubmissionUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validUpdateJson)

      val res: Future[Result] = controller.updateVerificationSubmission()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }

    "returns 500 InternalServerError when no enrolments found" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, updateVerificationSubmissionUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validUpdateJson)

      val res: Future[Result] = controller.updateVerificationSubmission()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
    }

    "returns 502 BadGateway for taxOfficeNumber = 502 (contractor enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("502", "")))

      val req = FakeRequest(POST, updateVerificationSubmissionUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validUpdateJson)

      val res: Future[Result] = controller.updateVerificationSubmission()(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("formp failed")
    }
  }

  ".processVerificationResponseFromChris" - {

    val postUrl = "/cis/verification/response/process"

    val validProcessResponseJson: JsValue =
      Json.toJson(
        ProcessVerificationResponseFromChrisRequest(
          instanceId = instanceId,
          verificationBatchResourceRef = 10L,
          acceptedTime = "2026-06-15T10:05:00Z",
          submissionStatus = "ACCEPTED",
          irMarkReceived = Some("IR_MARK_GGIS"),
          verificationResults = Seq(
            VerificationResult(
              resourceRef = 111L,
              matched = Some("Y"),
              verified = Some("Y"),
              verificationNumber = Some("V123456"),
              taxTreatment = "NET",
              verifiedDate = Some(LocalDateTime.of(2026, 6, 15, 10, 5, 0))
            ),
            VerificationResult(
              resourceRef = 222L,
              matched = Some("N"),
              verified = Some("N"),
              verificationNumber = Some("V654321"),
              taxTreatment = "GROSS",
              verifiedDate = Some(LocalDateTime.of(2026, 6, 15, 10, 6, 0))
            )
          )
        )
      )

    ".getSubmissionWithVerificationBatch" - {

      val instanceId                   = "abc-123"
      val verificationBatchResourceRef = 77L
      val url                          =
        s"/verification/submission-batch?instanceId=$instanceId&verificationBatchResourceRef=$verificationBatchResourceRef"

      "returns 200 OK with JSON body on success (contractor enrolment)" in new Setup {

        when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
          .thenReturn(Some(EmployerReference("200", "")))

        when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
          .thenReturn(None)

        val responseJson: JsValue = Json.parse(
          s"""
             |{
             |  "scheme": {
             |    "schemeId": 1,
             |    "instanceId": "$instanceId"
             |  },
             |  "subcontractors": [
             |    {
             |      "subcontractorId": 1,
             |      "subbieResourceRef": 10
             |    }
             |  ],
             |  "verifications": [
             |    {
             |      "verificationId": 1001,
             |      "verificationResourceRef": 201
             |    }
             |  ],
             |  "verificationBatch": {
             |    "verificationBatchId": 99,
             |    "verificationBatchResourceRef": $verificationBatchResourceRef
             |  },
             |  "submission": {
             |    "submissionId": 555,
             |    "submissionType": "CIS_VERIFY"
             |  }
             |}
             |""".stripMargin
        )

        when(mockResourceHelper.resourceAsString(any()))
          .thenReturn(responseJson.toString())

        val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)

        val res: Future[Result] =
          controller.getSubmissionWithVerificationBatch(
            instanceId,
            verificationBatchResourceRef
          )(req)

        status(res) mustBe OK
        contentType(res) mustBe Some(JSON)

        val body = contentAsJson(res)

        body mustBe responseJson
        (body \ "scheme" \ "schemeId").as[Long] mustBe 1L
        (body \ "scheme" \ "instanceId").as[String] mustBe instanceId
        (body \ "subcontractors")(0).\("subcontractorId").as[Long] mustBe 1L
        (body \ "verifications")(0).\("verificationId").as[Long] mustBe 1001L
        (body \ "verificationBatch" \ "verificationBatchResourceRef").as[Long] mustBe verificationBatchResourceRef
        (body \ "submission" \ "submissionId").as[Long] mustBe 555L
      }

      "returns 200 OK with JSON body on success (agent enrolment)" in new Setup {

        when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
          .thenReturn(None)

        when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
          .thenReturn(Some("IRAgentReference-123"))

        val responseJson: JsValue = Json.parse(
          s"""
             |{
             |  "scheme": {
             |    "schemeId": 1,
             |    "instanceId": "$instanceId"
             |  },
             |  "subcontractors": [
             |    {
             |      "subcontractorId": 1,
             |      "subbieResourceRef": 10
             |    }
             |  ],
             |  "verifications": [
             |    {
             |      "verificationId": 1001,
             |      "verificationResourceRef": 201
             |    }
             |  ],
             |  "verificationBatch": {
             |    "verificationBatchId": 99,
             |    "verificationBatchResourceRef": $verificationBatchResourceRef
             |  },
             |  "submission": {
             |    "submissionId": 555,
             |    "submissionType": "CIS_VERIFY"
             |  }
             |}
             |""".stripMargin
        )

        when(mockResourceHelper.resourceAsString(any()))
          .thenReturn(responseJson.toString())

        val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)

        val res: Future[Result] =
          controller.getSubmissionWithVerificationBatch(
            instanceId,
            verificationBatchResourceRef
          )(req)

        status(res) mustBe OK
        contentType(res) mustBe Some(JSON)
        contentAsJson(res) mustBe responseJson
      }

      "returns 502 BadGateway for taxOfficeNumber = 502 (contractor enrolment)" in new Setup {

        when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
          .thenReturn(Some(EmployerReference("502", "")))

        when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
          .thenReturn(None)

        val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)

        val res: Future[Result] =
          controller.getSubmissionWithVerificationBatch(
            instanceId,
            verificationBatchResourceRef
          )(req)

        status(res) mustBe BAD_GATEWAY
        (contentAsJson(res) \ "message").as[String] must include("formp failed")
      }

      "returns 500 InternalServerError for taxOfficeNumber = 500 (contractor enrolment)" in new Setup {

        when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
          .thenReturn(Some(EmployerReference("500", "")))

        when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
          .thenReturn(None)

        val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)

        val res: Future[Result] =
          controller.getSubmissionWithVerificationBatch(
            instanceId,
            verificationBatchResourceRef
          )(req)

        status(res) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
      }

      "returns 500 InternalServerError when no contractor enrolment and no agent enrolment found" in new Setup {

        when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
          .thenReturn(None)

        when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
          .thenReturn(None)

        val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)

        val res: Future[Result] =
          controller.getSubmissionWithVerificationBatch(
            instanceId,
            verificationBatchResourceRef
          )(req)

        status(res) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "returns 204 NO_CONTENT on success (contractor enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("200", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validProcessResponseJson)

      val res: Future[Result] = controller.processVerificationResponseFromChris()(req)

      status(res) mustBe NO_CONTENT
      contentAsString(res) mustBe ""
    }

    "returns 204 NO_CONTENT on success (agent enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(Some("IRAgentReference-123"))

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validProcessResponseJson)

      val res: Future[Result] = controller.processVerificationResponseFromChris()(req)

      status(res) mustBe NO_CONTENT
      contentAsString(res) mustBe ""
    }

    "returns 400 BadRequest when JSON is invalid" in new Setup {
      val invalidJson = Json.obj(
        "instanceId" -> instanceId
      )

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(invalidJson)

      val res: Future[Result] = controller.processVerificationResponseFromChris()(req)

      status(res) mustBe BAD_REQUEST
      contentType(res) mustBe Some(JSON)

      val body = contentAsJson(res)
      (body \ "message").as[String] mustBe "Invalid payload"
      (body \ "errors").isDefined mustBe true
    }

    "returns 502 BadGateway for taxOfficeNumber = 502 (contractor enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("502", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validProcessResponseJson)

      val res: Future[Result] = controller.processVerificationResponseFromChris()(req)

      status(res) mustBe BAD_GATEWAY
      (contentAsJson(res) \ "message").as[String] must include("formp failed")
    }

    "returns 500 InternalServerError for taxOfficeNumber = 500 (contractor enrolment)" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("500", "")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validProcessResponseJson)

      val res: Future[Result] = controller.processVerificationResponseFromChris()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(res) \ "message").as[String] mustBe "Unexpected error"
    }

    "returns 500 InternalServerError when no contractor enrolment and no agent enrolment found" in new Setup {
      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val req = FakeRequest(POST, postUrl)
        .withHeaders(CONTENT_TYPE -> JSON, ACCEPT -> JSON)
        .withBody(validProcessResponseJson)

      val res: Future[Result] = controller.processVerificationResponseFromChris()(req)

      status(res) mustBe INTERNAL_SERVER_ERROR
    }
  }

  private trait Setup {
    val mockResourceHelper: ResourceHelper     = mock[ResourceHelper]
    val mockEnrolmentsHelper: EnrolmentsHelper = mock[EnrolmentsHelper]

    val auth: FakeAuthAction = new FakeAuthAction(cc.parsers)
    lazy val controller      = new VerificationController(auth, mockResourceHelper, mockEnrolmentsHelper, cc)
  }
}
