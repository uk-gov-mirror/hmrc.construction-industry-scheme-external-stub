/*
 * Copyright 2025 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.constructionindustryschemeexternalstub.actions.FakeAuthAction
import uk.gov.hmrc.constructionindustryschemeexternalstub.models.EmployerReference
import uk.gov.hmrc.constructionindustryschemeexternalstub.utils.{EnrolmentsHelper, ResourceHelper}

class ContractorSchemeControllerSpec extends AnyWordSpec with Matchers with MockitoSugar {

  private val cc: ControllerComponents = stubControllerComponents()

  "ContractorSchemeController#getScheme" should {

    "return 200 and default JSON when enrolments present with normal taxOfficeNumber" in {
      val mockResourceHelper   = mock[ResourceHelper]
      val mockEnrolmentsHelper = mock[EnrolmentsHelper]

      when(
        mockResourceHelper.resourceAsString(
          eqTo("/resources/contractorSchemes/getScheme-200-sub1-response.json")
        )
      ).thenReturn("""{ "schemeId": 1000 }""")

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("123", "AB1234")))

      val authAction = new FakeAuthAction(cc.parsers)

      val controller = new ContractorSchemeController(
        authorise = authAction,
        resourceHelper = mockResourceHelper,
        enrolmentHelper = mockEnrolmentsHelper,
        cc = cc
      )

      val result =
        controller.getScheme("some-instance-id")(FakeRequest(GET, "/formp-proxy/scheme/some-instance-id"))

      status(result) mustBe OK
      (contentAsJson(result) \ "schemeId").as[Int] mustBe 1000

      verify(mockResourceHelper)
        .resourceAsString(eqTo("/resources/contractorSchemes/getScheme-200-sub1-response.json"))
    }

    "return 500 when enrolments are missing" in {
      val mockResourceHelper   = mock[ResourceHelper]
      val mockEnrolmentsHelper = mock[EnrolmentsHelper]

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(None)
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val authAction = new FakeAuthAction(cc.parsers)

      val controller = new ContractorSchemeController(
        authorise = authAction,
        resourceHelper = mockResourceHelper,
        enrolmentHelper = mockEnrolmentsHelper,
        cc = cc
      )

      val result = controller.getScheme("any")(FakeRequest(GET, "/formp-proxy/scheme/any"))

      status(result) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "message").as[String] mustBe "Missing enrolments"

      verify(mockResourceHelper, never()).resourceAsString(any[String])
    }
  }

  "ContractorSchemeController#createScheme" should {

    "return 201 and stub JSON on valid payload and enrolments" in {
      val mockResourceHelper   = mock[ResourceHelper]
      val mockEnrolmentsHelper = mock[EnrolmentsHelper]

      when(
        mockResourceHelper.resourceAsString(
          eqTo("/resources/contractorSchemes/createScheme-201-response.json")
        )
      ).thenReturn("""{ "schemeId": 999 }""")

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("123", "AB1234")))

      val authAction = new FakeAuthAction(cc.parsers)

      val controller = new ContractorSchemeController(
        authorise = authAction,
        resourceHelper = mockResourceHelper,
        enrolmentHelper = mockEnrolmentsHelper,
        cc = cc
      )

      val jsonBody =
        """
          |{
          |  "instanceId": "CIS-123456",
          |  "accountsOfficeReference": "123PA00123456",
          |  "taxOfficeNumber": "123",
          |  "taxOfficeReference": "AB1234"
          |}
          |""".stripMargin

      val request = FakeRequest(POST, "/formp-proxy/scheme")
        .withBody(Json.parse(jsonBody))
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.createScheme(request)

      status(result) mustBe CREATED
      contentAsString(result) mustBe """{ "schemeId": 999 }"""
    }

    "return 400 on invalid JSON" in {
      val mockResourceHelper   = mock[ResourceHelper]
      val mockEnrolmentsHelper = mock[EnrolmentsHelper]

      val authAction = new FakeAuthAction(cc.parsers)

      val controller = new ContractorSchemeController(
        authorise = authAction,
        resourceHelper = mockResourceHelper,
        enrolmentHelper = mockEnrolmentsHelper,
        cc = cc
      )

      val badJson = Json.obj("foo" -> "bar")
      val request = FakeRequest(POST, "/formp-proxy/scheme")
        .withBody(badJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.createScheme(request)

      status(result) mustBe BAD_REQUEST
    }
  }

  "ContractorSchemeController#updateScheme" should {

    "return 200 and stub JSON on valid payload and enrolments" in {
      val mockResourceHelper   = mock[ResourceHelper]
      val mockEnrolmentsHelper = mock[EnrolmentsHelper]

      when(
        mockResourceHelper.resourceAsString(
          eqTo("/resources/contractorSchemes/updateScheme-200-response.json")
        )
      ).thenReturn("""{ "version": 2 }""")

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("123", "AB1234")))

      val authAction = new FakeAuthAction(cc.parsers)

      val controller = new ContractorSchemeController(
        authorise = authAction,
        resourceHelper = mockResourceHelper,
        enrolmentHelper = mockEnrolmentsHelper,
        cc = cc
      )

      val jsonBody =
        """
          |{
          |  "schemeId": 1000,
          |  "instanceId": "CIS-123456",
          |  "accountsOfficeReference": "123PA00123456",
          |  "taxOfficeNumber": "123",
          |  "taxOfficeReference": "AB1234"
          |}
          |""".stripMargin

      val request = FakeRequest(POST, "/formp-proxy/scheme/update")
        .withBody(Json.parse(jsonBody))
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.updateScheme(request)

      status(result) mustBe OK
      contentAsString(result) mustBe """{ "version": 2 }"""
    }
  }

  "ContractorSchemeController#updateSchemeVersion" should {

    "return 200 and increment version on valid payload and enrolments" in {
      val mockResourceHelper   = mock[ResourceHelper]
      val mockEnrolmentsHelper = mock[EnrolmentsHelper]

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("123", "AB1234")))

      val authAction = new FakeAuthAction(cc.parsers)

      val controller = new ContractorSchemeController(
        authorise = authAction,
        resourceHelper = mockResourceHelper,
        enrolmentHelper = mockEnrolmentsHelper,
        cc = cc
      )

      val jsonBody =
        """
          |{
          |  "instanceId": "CIS-123456",
          |  "version": 2
          |}
          |""".stripMargin

      val request = FakeRequest(POST, "/formp-proxy/scheme/update-version")
        .withBody(Json.parse(jsonBody))
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.updateSchemeVersion(request)

      status(result) mustBe OK
      (contentAsJson(result) \ "version").as[Int] mustBe 3
    }

    "return 400 on invalid JSON" in {
      val mockResourceHelper   = mock[ResourceHelper]
      val mockEnrolmentsHelper = mock[EnrolmentsHelper]

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("123", "AB1234")))

      val authAction = new FakeAuthAction(cc.parsers)

      val controller = new ContractorSchemeController(
        authorise = authAction,
        resourceHelper = mockResourceHelper,
        enrolmentHelper = mockEnrolmentsHelper,
        cc = cc
      )

      val request = FakeRequest(POST, "/formp-proxy/scheme/update-version")
        .withBody(Json.obj("foo" -> "bar"))
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.updateSchemeVersion(request)

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] mustBe "Invalid JSON body"
    }
  }

  "ContractorSchemeController#applyPrepopulation" should {

    "return 200 and increment version on valid payload and enrolments" in {
      val mockResourceHelper   = mock[ResourceHelper]
      val mockEnrolmentsHelper = mock[EnrolmentsHelper]

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("123", "AB1234")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val controller = new ContractorSchemeController(
        authorise = new FakeAuthAction(cc.parsers),
        resourceHelper = mockResourceHelper,
        enrolmentHelper = mockEnrolmentsHelper,
        cc = cc
      )

      val request =
        FakeRequest(POST, "/formp-proxy/scheme/apply-prepopulation")
          .withBody(
            Json.parse("""
                |{
                |  "schemeId": 1000,
                |  "instanceId": "CIS-123456",
                |  "accountsOfficeReference": "123PA00123456",
                |  "taxOfficeNumber": "123",
                |  "taxOfficeReference": "AB1234",
                |  "utr": "1234567890",
                |  "name": "Test Ltd",
                |  "emailAddress": "test@example.com",
                |  "displayWelcomePage": "Y",
                |  "prePopCount": 1,
                |  "prePopSuccessful": "Y",
                |  "version": 2,
                |  "subcontractors": [
                |    {
                |      "subcontractorType": "soletrader",
                |      "utr": "1111111111",
                |      "verificationNumber": "V1",
                |      "firstName": "Ann",
                |      "surname": "Smith",
                |      "verified": "Y",
                |      "autoVerified": "Y"
                |    }
                |  ]
                |}
                |""".stripMargin)
          )
          .withHeaders("Content-Type" -> "application/json")

      val result = controller.applyPrepopulation(request)

      status(result) mustBe OK
      (contentAsJson(result) \ "version").as[Int] mustBe 3
    }

    "return 400 on invalid JSON when enrolments exist" in {
      val mockResourceHelper   = mock[ResourceHelper]
      val mockEnrolmentsHelper = mock[EnrolmentsHelper]

      when(mockEnrolmentsHelper.contractorEnrolmentsOpt(any()))
        .thenReturn(Some(EmployerReference("123", "AB1234")))
      when(mockEnrolmentsHelper.agentEnrolmentsOpt(any()))
        .thenReturn(None)

      val controller = new ContractorSchemeController(
        authorise = new FakeAuthAction(cc.parsers),
        resourceHelper = mockResourceHelper,
        enrolmentHelper = mockEnrolmentsHelper,
        cc = cc
      )

      val request =
        FakeRequest(POST, "/formp-proxy/scheme/apply-prepopulation")
          .withBody(Json.obj("foo" -> "bar"))
          .withHeaders("Content-Type" -> "application/json")

      val result = controller.applyPrepopulation(request)

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] mustBe "Invalid JSON body"
    }
  }
}
