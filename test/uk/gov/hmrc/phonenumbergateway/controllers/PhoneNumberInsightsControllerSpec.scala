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

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.phonenumbergateway.config.AppConfig
import uk.gov.hmrc.phonenumbergateway.connector.DownstreamConnector

import scala.concurrent.{ExecutionContextExecutor, Future}

class PhoneNumberInsightsControllerSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar {

  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  "PhoneNumberInsightsController" should {

    "forward the request and preserve CorrelationId header when present" in {
      val cc = stubControllerComponents()
      val config = mock[AppConfig]
      val connector = mock[DownstreamConnector]
      val authConnector = mock[AuthConnector]
      val controller = new PhoneNumberInsightsController(cc, config, connector, authConnector)(cc.executionContext)

      when(config.rejectInternalTraffic).thenReturn(false)
      when(config.phoneNumberInsightsProxyBaseUrl).thenReturn("http://proxy/")
      when(config.internalAuthToken).thenReturn("token")

      val fakeRequest = FakeRequest("GET", "/phone-number-gateway/insights")
        .withHeaders("CorrelationId" -> "abc-123")

      val resultStub = Results.Ok("forwarded").withHeaders("SomeHeader" -> "value")
      when(connector.forward(any(), any(), any())(any())).thenReturn(Future.successful(resultStub))

      val result = controller.checkInsights()(fakeRequest)
      status(result) shouldBe OK
      contentAsString(result) shouldBe "forwarded"
      headers(result) should contain("CorrelationId" -> "abc-123")
    }

    "forward the request and not add CorrelationId header when absent" in {
      val cc = stubControllerComponents()
      val config = mock[AppConfig]
      val connector = mock[DownstreamConnector]
      val authConnector = mock[AuthConnector]
      val controller = new PhoneNumberInsightsController(cc, config, connector, authConnector)(cc.executionContext)

      when(config.rejectInternalTraffic).thenReturn(false)
      when(config.phoneNumberInsightsProxyBaseUrl).thenReturn("http://proxy/")
      when(config.internalAuthToken).thenReturn("token")

      val fakeRequest = FakeRequest("GET", "/phone-number-gateway/insights")

      val resultStub = Results.Ok("forwarded")
      when(connector.forward(any(), any(), any())(any())).thenReturn(Future.successful(resultStub))

      val result = controller.checkInsights()(fakeRequest)
      status(result) shouldBe OK
      contentAsString(result) shouldBe "forwarded"
      headers(result).get("CorrelationId") shouldBe None
    }

  }
}
