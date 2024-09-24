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

package uk.gov.hmrc.phonenumbergateway

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, equalToJson, post, urlEqualTo}
import org.apache.pekko.http.scaladsl.model.MediaTypes
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.{HeaderNames, MimeTypes}
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.test.ExternalWireMockSupport

class VerifyControllerIntegrationSpec
  extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with ExternalWireMockSupport {

  private val wsClient = app.injector.instanceOf[WSClient]
  private val baseUrl = s"http://localhost:$port"

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "metrics.enabled" -> false,
        "microservice.services.phone-number-verification.port" -> externalWireMockPort
      )
      .build()

// Phone number tests
"PhoneNumberController" should {
  "respond with OK status" when {
    "valid json payload is provided" in {
      externalWireMockServer.stubFor(
        post(urlEqualTo(s"/verify"))
          .withRequestBody(equalToJson("""{"phoneNumber": "12123123456"}"""))
          .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .willReturn(
            aResponse()
              .withBody(
                """{"phoneNumber": "12123123456"}"""
              )
              .withStatus(OK)
          )
      )
      val response =
        wsClient
          .url(s"$baseUrl/verify")
          .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
          .post("""{"phoneNumber": "12123123456"}""")
          .futureValue

      response.status shouldBe OK
      response.json shouldBe Json.parse(
        """{"phoneNumber": "12123123456"}"""
      )
    }
  }

  "respond with BAD_REQUEST status" when {
    "invalid json payload is provided" in {
      externalWireMockServer.stubFor(
        post(urlEqualTo(s"/verify"))
          .withRequestBody(equalToJson("""{"no-phoneNumber": "12123123456"}"""))
          .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MediaTypes.`application/json`.value))
          .willReturn(
            aResponse()
              .withBody(
                """{"statusCode":400,"message":"bad request, cause: invalid json"}"""
              )
              .withStatus(BAD_REQUEST)
          )
      )
      val response =
        wsClient
          .url(s"$baseUrl/verify")
          .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
          .post("""{"no-phoneNumber": "12123123456"}""")
          .futureValue

      response.status shouldBe BAD_REQUEST
      response.json shouldBe Json.parse("""{"statusCode":400,"message":"bad request, cause: invalid json"}""")
    }
  }

  "respond with BAD_REQUEST status" when {
    "malformed json payload is provided" in {
      val response =
        wsClient
          .url(s"$baseUrl/verify")
          .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
          .post("""{"phoneNumber"12123123456"}""")
          .futureValue

      response.status shouldBe BAD_REQUEST
      response.json shouldBe Json.parse("""{"statusCode":400,"message":"bad request, cause: invalid json"}""")
    }
  }
}

// Passcode tests
"PhoneNumberController" should {
  "respond with OK status for Passcode" when {
    "valid json payload is provided" in {
      externalWireMockServer.stubFor(
        post(urlEqualTo(s"/verify/passcode"))
          .withRequestBody(equalToJson("""{"phoneNumber": "12123123456",  "passcode": "ABCGED"}"""))
          .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .willReturn(
            aResponse()
              .withBody(
                """{"phoneNumber": "12123123456",  "passcode": "ABCGED"}"""
              )
              .withStatus(OK)
          )
      )
      val response =
        wsClient
          .url(s"$baseUrl/verify/passcode")
          .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
          .post("""{"phoneNumber": "12123123456",  "passcode": "ABCGED"}""")
          .futureValue

      response.status shouldBe OK
      response.json shouldBe Json.parse(
        """{"phoneNumber": "12123123456",  "passcode": "ABCGED"}"""
      )
    }
  }

  "respond with BAD_REQUEST status" when {
    "invalid json payload is provided for Passcode" in {
      externalWireMockServer.stubFor(
        post(urlEqualTo(s"/verify/passcode"))
          .withRequestBody(equalToJson("""{"phoneNumber": "12123123456",  "no-passcode": "ABCGED"}"""))
          .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MediaTypes.`application/json`.value))
          .willReturn(
            aResponse()
              .withBody(
                """{"statusCode":400,"message":"bad request, cause: invalid json"}"""
              )
              .withStatus(BAD_REQUEST)
          )
      )
      val response =
        wsClient
          .url(s"$baseUrl/verify/passcode")
          .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
          .post("""{"phoneNumber": "12123123456",  "no-passcode": "ABCGED"}""")
          .futureValue

      response.status shouldBe BAD_REQUEST
      response.json shouldBe Json.parse("""{"statusCode":400,"message":"bad request, cause: invalid json"}""")
    }
  }

  "respond with BAD_REQUEST status" when {
    "malformed json payload is provided for Passcode" in {
      val response =
        wsClient
          .url(s"$baseUrl/verify/passcode")
          .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
          .post("""{"phoneNumber": "12123123456",  "passcode"ABCGED"}""")
          .futureValue

      response.status shouldBe BAD_REQUEST
      response.json shouldBe Json.parse("""{"statusCode":400,"message":"bad request, cause: invalid json"}""")
    }
  }
}

}