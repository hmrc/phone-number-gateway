/*
 * Copyright 2024 HM Revenue & Customs
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

/*
 * Copyright 2024 HM Revenue & Customs
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

import org.apache.pekko.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.{HeaderNames, MimeTypes}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results._
import play.api.routing.sird.{POST => SPOST, _}
import play.api.test.Helpers._
import play.core.server.{Server, ServerConfig}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class DownstreamConnectorSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {
  val insightsPort = 11222

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.address-insights-proxy.port" -> insightsPort)
    .build()

  private val connector = app.injector.instanceOf[DownstreamConnector]
  implicit val mat: Materializer = app.injector.instanceOf[Materializer]

  "Checking connectivity" should {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    "return true if the remote service returns a 200" in {
      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        { case r @ SPOST(p"/insights") =>
          Action(Ok("{}").withHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON))
        }
      } { _ =>
        val result = await(connector.checkConnectivity(s"http://localhost:${insightsPort}/insights", "1234"))
        result shouldBe true
      }
    }

    "return true if the remote service returns a 400" in {
      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        { case r @ SPOST(p"/insights") =>
          Action(BadRequest("{}").withHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON))
        }
      } { _ =>
        val result = await(connector.checkConnectivity(s"http://localhost:${insightsPort}/insights", "1234"))
        result shouldBe true
      }
    }

    "return false if the remote service returns a 401" in {
      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        { case r @ SPOST(p"/check/insights") =>
          Action(Unauthorized("{}").withHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON))
        }
      } { _ =>
        val result = await(connector.checkConnectivity(s"http://localhost:${insightsPort}/insights", "1234"))
        result shouldBe false
      }
    }

    "return false if the remote service returns a 404" in {
      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        { case r @ SPOST(p"/insights") =>
          Action(NotFound)
        }
      } { _ =>
        val result = await(connector.checkConnectivity(s"http://localhost:${insightsPort}/insights", "1234"))
        result shouldBe false
      }
    }

    "return false if the remote service returns a 500" in {
      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        { case r @ SPOST(p"/insights") =>
          Action(InternalServerError)
        }
      } { _ =>
        val result = await(connector.checkConnectivity(s"http://localhost:${insightsPort}/insights", "1234"))
        result shouldBe false
      }
    }

    "return false if the remote service returns a 502" in {
      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        { case r @ SPOST(p"/insights") =>
          Action(BadGateway)
        }
      } { _ =>
        val result = await(connector.checkConnectivity(s"http://localhost:${insightsPort}/insights", "1234"))
        result shouldBe false
      }
    }
  }
}
