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

????????????????????????????

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

class PhoneNumberControllerIntegrationSpec
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
        "microservice.services.address-insights-proxy.port" -> externalWireMockPort
      )
      .build()

  "PhoneNumberController" should {
    "respond with OK status" when {
      "valid json payload is provided" in {
        externalWireMockServer.stubFor(
          post(urlEqualTo(s"/insights"))
            .withRequestBody(equalToJson("""{"address":{"line1":"1 High Street", "country":"United Kingdom"}}"""))
            .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
            .willReturn(
              aResponse()
                .withBody(
                  """{"correlationId":"220967234589763549876", "address":{}, "insights":  {"risk":{ "riskScore": 0, "reason": "ADDRESS_NOT_ON_WATCHLIST"}, "insights": {"occurrences": []}}}"""
                )
                .withStatus(OK)
            )
        )
        val response =
          wsClient
            .url(s"$baseUrl/insights")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
            .post("""{"address":{"line1":"1 High Street", "country":"United Kingdom"}}""")
            .futureValue

        response.status shouldBe OK
        response.json shouldBe Json.parse(
          """{"correlationId":"220967234589763549876", "address":{}, "insights":  {"risk":{ "riskScore": 0, "reason": "ADDRESS_NOT_ON_WATCHLIST"}, "insights": {"occurrences": []}}}"""
        )
      }
    }

    "respond with BAD_REQUEST status" when {
      "invalid json payload is provided" in {
        externalWireMockServer.stubFor(
          post(urlEqualTo(s"/insights"))
            .withRequestBody(equalToJson("""{"address":{"line1":"1 High Street", "country":"United Kingdom"}}"""))
            .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MediaTypes.`application/json`.value))
            .willReturn(
              aResponse()
                .withBody(
                  """{"correlationId":"220967234589763549876", "address":{}, "insights":  {"risk":{ "riskScore": 0, "reason": "ADDRESS_NOT_ON_WATCHLIST"}, "insights": {"occurrences": []}}}"""
                )
                .withStatus(OK)
            )
        )
        val response =
          wsClient
            .url(s"$baseUrl/insights")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
            .post("""{"address":{"line1":"1 High Street", "country":"United Kingdom"}""")
            .futureValue

        response.status shouldBe BAD_REQUEST
        response.json shouldBe Json.parse("""{"statusCode":400,"message":"bad request, cause: invalid json"}""")
      }
    }
  }
}