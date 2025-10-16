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

package uk.gov.hmrc.phonenumbergateway

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ToggledAuthorisedFunctionsSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {

  trait Setup {
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val toggledAuthorisedFunctions: ToggledAuthorisedFunctions = new ToggledAuthorisedFunctions {
      override val authConnector: AuthConnector = mockAuthConnector
    }
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "toggledAuthorised" should {

    "execute the body when feature is enabled and authorisation succeeds" in new Setup {
      when(mockAuthConnector.authorise[Unit](any[Predicate], any())(any(), any()))
        .thenReturn(Future.successful(()))

      val result: Future[String] = toggledAuthorisedFunctions
        .toggledAuthorised(enabled = true, mock[Predicate]) {
          Future.successful("Success")
        }

      whenReady(result) { res =>
        res shouldBe "Success"
      }
    }

    "execute the body when feature is disabled without calling authorisation" in new Setup {
      val result: Future[String] = toggledAuthorisedFunctions
        .toggledAuthorised(enabled = false, mock[Predicate]) {
          Future.successful("Success")
        }

      whenReady(result) { res =>
        res shouldBe "Success"
      }

      verify(mockAuthConnector, never()).authorise(any[Predicate], any())(any(), any())
    }

    "fail when feature is enabled and authorisation fails" in new Setup {
      when(mockAuthConnector.authorise(any[Predicate], any())(any(), any()))
        .thenReturn(Future.failed(new AuthorisationException("Auth failed") {}))

      val result: Future[String] = toggledAuthorisedFunctions
        .toggledAuthorised(enabled = true, mock[Predicate]) {
          Future.successful("Success")
        }

      whenReady(result.failed) { ex =>
        ex shouldBe a[AuthorisationException]
        ex.getMessage should include("Auth failed")
      }
    }
  }
}
