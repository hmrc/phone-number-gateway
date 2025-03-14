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

package uk.gov.hmrc.phonenumbergateway.connector

import com.github.tomakehurst.wiremock.client.WireMock._
import org.apache.pekko.stream.Materializer
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.phonenumbergateway.IntegrationBaseSpec
import uk.gov.hmrc.phonenumbergateway.support.WireMockHelper

import scala.concurrent.Future

class DownstreamConnectorIntegrationSpec extends IntegrationBaseSpec with ScalaFutures with IntegrationPatience {
  implicit lazy val materializer: Materializer = app.materializer

  override def serviceConfig: Map[String, Any] = Map(
    "microservice.services.phone-number-verification.port" -> WireMockHelper.wireMockPort
  )

  class Test {
    lazy val connector: DownstreamConnector = app.injector.instanceOf[DownstreamConnector]
  }

  "DownstreamConnector" should {

    "forward a POST request with JSON body" in new Test {
      stubFor(
        post(urlEqualTo("/test-endpoint"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "application/json")
              .withBody("""{"message": "success"}""")
          )
      )

      val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, "/test-endpoint")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(Json.parse("""{"key": "value"}"""))

      val result: Future[Result] = connector.forward(request, s"http://localhost:${WireMockHelper.wireMockPort}/test-endpoint", "authToken")

      play.api.test.Helpers.status(result) shouldBe (200)
      contentType(result) shouldBe Some("application/json")
      contentAsString(result) should include("success")
    }

    "return a MethodNotAllowed when the request isn't an expected POST" in new Test {
      val request: FakeRequest[AnyContentAsJson] = FakeRequest(GET, "/test-endpoint")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(Json.parse("""{"key": "value"}"""))

      val result: Future[Result] = connector.forward(request, s"http://localhost:${WireMockHelper.wireMockPort}/test-endpoint", "authToken")

      play.api.test.Helpers.status(result) shouldBe (405)
    }
  }
}
