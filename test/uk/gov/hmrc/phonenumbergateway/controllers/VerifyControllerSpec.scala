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

package uk.gov.hmrc.phonenumbergateway.controllers

import org.apache.pekko.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.{HeaderNames, MimeTypes, Status}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Results._
import play.api.routing.sird.{POST => SPOST, _}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.core.server.{Server, ServerConfig}
import uk.gov.hmrc.http.HeaderCarrier

class VerifyControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {
  val insightsPort = 11222

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.phone-number-verification.port" -> insightsPort)
    .build()

  private val controller = app.injector.instanceOf[VerifyController]
  implicit val mat: Materializer = app.injector.instanceOf[Materializer]

  // Phone number tests
  "POST /send-code" should {

    "forward a 200 response from the downstream service" in {
      val response = """{"status":"CODE_SENT", "message":"Phone verification code successfully sent"}""".stripMargin

      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        { case r @ SPOST(p"/send-code") =>
          Action(Ok(response).withHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON))
        }
      } { _ =>
        val requestAddressJson = Json
          .parse("""{"phoneNumber": "12123123456"}""")
          .as[JsObject]
        val fakeRequest = FakeRequest("POST", "/send-code")
          .withJsonBody(requestAddressJson)
          .withHeaders("True-Calling-Client" -> "example-service", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)

        val result = controller.any()(fakeRequest)
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe response
      }
    }

    "forward a 400 response from the downstream service" in {
      val errorResponse = """{"code": "MALFORMED_JSON", "path.missing: phoneNumber"}""".stripMargin

      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        { case r @ SPOST(p"/send-code") =>
          Action(
            BadRequest(errorResponse).withHeaders(
              HeaderNames.CONTENT_TYPE -> MimeTypes.JSON
            )
          )
        }
      } { _ =>
        val fakeRequest = FakeRequest("POST", "/send-code")
          .withJsonBody(Json.parse("""{"no-phoneNumber": "12123123456"}"""))
          .withHeaders("True-Calling-Client" -> "example-service", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)

        val result = controller.any()(fakeRequest)
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe errorResponse
      }
    }

    "handle a malformed json payload" in {
      val errorResponse = """{"code": "MALFORMED_JSON", "path.missing: phoneNumber"}""".stripMargin

      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        { case r @ SPOST(p"/send-code") =>
          Action(BadRequest(errorResponse).withHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON))
        }
      } { _ =>
        val fakeRequest = FakeRequest("POST", "/send-code")
          .withTextBody("""{""")
          .withHeaders("True-Calling-Client" -> "example-service", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)

        val result = controller.any()(fakeRequest)
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe errorResponse
      }
    }

    "return bad gateway if there is no connectivity to the downstream service" in {
      val errorResponse = """{"code": "REQUEST_DOWNSTREAM", "desc": "An issue occurred when the downstream service tried to handle the request"}""".stripMargin

      val fakeRequest = FakeRequest("POST", "/phone-number-gateway/send-code")
        .withJsonBody(Json.parse("""{"phoneNumber": "12123123456"}"""))
        .withHeaders("True-Calling-Client" -> "example-service", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)

      val result = controller.any()(fakeRequest)
      status(result) shouldBe Status.BAD_GATEWAY
      contentAsString(result) shouldBe errorResponse
    }

  }

  "POST /verify-code" should {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    "forward a 200 response from the downstream service" in {
      val response = """{"status":"CODE_VERIFIED", "message":"Phone verification code successfully sent"}""".stripMargin

      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        { case r @ SPOST(p"/verify-code") =>
          Action(Ok(response).withHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON))
        }
      } { _ =>
        val requestAddressJson = Json
          .parse("""{"phoneNumber": "12123123456",  "verificationCode": "ABCGED"}""".stripMargin)
          .as[JsObject]
        val fakeRequest = FakeRequest("POST", "/verify-code")
          .withJsonBody(requestAddressJson)
          .withHeaders("True-Calling-Client" -> "example-service", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)

        val result = controller.any()(fakeRequest)
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe response
      }
    }

    "forward a 400 response from the downstream service" in {
      val errorResponse = """{"code": "MALFORMED_JSON", "path.missing: verificationCode"}""".stripMargin

      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        { case r @ SPOST(p"/verify-code") =>
          Action(
            BadRequest(errorResponse).withHeaders(
              HeaderNames.CONTENT_TYPE -> MimeTypes.JSON
            )
          )
        }
      } { _ =>
        val fakeRequest = FakeRequest("POST", "/verify-code")
          .withJsonBody(Json.parse("""{"phoneNumber": "12123123456",  "no-verification-code": "ABCGED"}"""))
          .withHeaders("True-Calling-Client" -> "example-service", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)

        val result = controller.any()(fakeRequest)
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe errorResponse
      }
    }

    "handle a malformed json payload" in {
      val errorResponse = """{"code": "MALFORMED_JSON", "path.missing: verificationCode"}""".stripMargin

      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        { case r @ SPOST(p"/verify-code") =>
          Action(BadRequest(errorResponse).withHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON))
        }
      } { _ =>
        val fakeRequest = FakeRequest("POST", "/verify-code")
          .withTextBody("""{""")
          .withHeaders("True-Calling-Client" -> "example-service", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)

        val result = controller.any()(fakeRequest)
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe errorResponse
      }
    }

    "return bad gateway if there is no connectivity to the downstream service" in {
      val errorResponse = """{"code": "REQUEST_DOWNSTREAM", "desc": "An issue occurred when the downstream service tried to handle the request"}""".stripMargin

      val fakeRequest = FakeRequest("POST", "/phone-number-gateway/verify-code")
        .withJsonBody(Json.parse("""{"phoneNumber": "12123123456",  "no-verification-code": "ABCGED"}"""))
        .withHeaders("True-Calling-Client" -> "example-service", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)

      val result = controller.any()(fakeRequest)
      status(result) shouldBe Status.BAD_GATEWAY
      contentAsString(result) shouldBe errorResponse
    }

  }
}
