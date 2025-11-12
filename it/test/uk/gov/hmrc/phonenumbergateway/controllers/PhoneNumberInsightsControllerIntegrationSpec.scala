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

package uk.gov.hmrc.phonenumbergateway.controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import org.apache.pekko.http.scaladsl.model.MediaTypes
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.test.ExternalWireMockSupport
import uk.gov.hmrc.phonenumbergateway.models.{Error, MissingCorrelationId}

class PhoneNumberInsightsControllerIntegrationSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with ExternalWireMockSupport {

  private val wsClient = app.injector.instanceOf[WSClient]
  private val baseUrl = s"http://localhost:$port/phone-number-gateway"

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "metrics.enabled" -> false,
        "microservice.services.phone-number-insights-proxy.port" -> externalWireMockPort
      )
      .build()

  private val CORRELATION_ID_HEADER_NAME = "X-Correlation-ID"
  private val testCorrelationId = "f0bd1f32-de51-45cc-9b18-0520d6e3ab1a"

  "POST /check/insights" should {

    "include the CorrelationId header" when {
      "present in the initial request" in {
        externalWireMockServer.stubFor(
          post(urlEqualTo(s"/phone-number-insights-proxy/check/insights"))
            .withRequestBody(equalToJson("""{"phone-number": "0123456789"}"""))
            .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MediaTypes.`application/json`.value))
            .withHeader(CORRELATION_ID_HEADER_NAME, equalTo(testCorrelationId)) // ensure correlation ID is passed to the downstream service
            .willReturn(
              aResponse()
                .withBody(Json.stringify(Json.obj("phone-number" -> "0123456789", "risk" -> "100")))
                .withStatus(OK)
            )
        )

        val response =
          wsClient
            .url(s"$baseUrl/check/insights")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
            .withHttpHeaders(CORRELATION_ID_HEADER_NAME -> testCorrelationId)
            .post(Json.stringify(Json.obj("phone-number" -> "0123456789")))
            .futureValue

        response.status shouldBe OK
        response.header(CORRELATION_ID_HEADER_NAME) shouldBe Some(testCorrelationId) // ensure correlation ID is echoed back on the response
        response.json shouldBe Json.obj("phone-number" -> "0123456789", "risk" -> "100")
      }
    }

    "return BadRequest" when {
      "Correlation ID is missing from the initial request" in {

        val response =
          wsClient
            .url(s"$baseUrl/check/insights")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
            .post(Json.stringify(Json.obj("phone-number" -> "0123456789")))
            .futureValue

        response.status shouldBe BAD_REQUEST
        response.json shouldBe Json.toJson[Error](MissingCorrelationId)
      }
    }
  }

}
