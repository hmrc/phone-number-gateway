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

import play.api.Logging
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.{PrivilegedApplication, StandardApplication}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders}
import uk.gov.hmrc.phonenumbergateway.ToggledAuthorisedFunctions
import uk.gov.hmrc.phonenumbergateway.config.AppConfig
import uk.gov.hmrc.phonenumbergateway.connector.DownstreamConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class PhoneNumberInsightsController @Inject() (
  cc: ControllerComponents,
  config: AppConfig,
  connector: DownstreamConnector,
  val authConnector: AuthConnector
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with ToggledAuthorisedFunctions
    with Logging {

  private val CORRELATION_ID_HEADER = "CorrelationId"

  def checkInsights(): Action[AnyContent] = Action.async { implicit request =>
    toggledAuthorised(config.rejectInternalTraffic, AuthProviders(StandardApplication, PrivilegedApplication)) {
      val correlationId: Option[String] = request.headers.get(CORRELATION_ID_HEADER)
      val path = request.target.uri.toString.replace("phone-number-gateway", "phone-number-insights-proxy")
      val url = s"${config.phoneNumberInsightsProxyBaseUrl}$path"

      connector
        .forward(request, url, config.internalAuthToken)
        .map { result =>
          correlationId match {
            case Some(id) => result.withHeaders(CORRELATION_ID_HEADER -> id)
            case None     => result
          }
        }
    }
  }
}
