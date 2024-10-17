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

import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.phonenumbergateway.ToggledAuthorisedFunctions
import uk.gov.hmrc.phonenumbergateway.config.AppConfig
import uk.gov.hmrc.phonenumbergateway.connector.DownstreamConnector
import uk.gov.hmrc.auth.core.AuthProvider.StandardApplication
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class VerifyController @Inject() (cc: ControllerComponents, config: AppConfig, connector: DownstreamConnector, val authConnector: AuthConnector)(implicit
  ec: ExecutionContext
) extends BackendController(cc)
    with ToggledAuthorisedFunctions:

  private val logger = Logger(this.getClass.getSimpleName)

  def any(): Action[AnyContent] = Action.async { implicit request =>
    toggledAuthorised(config.rejectInternalTraffic, AuthProviders(StandardApplication)):
      val path = request.target.uri.toString.replace("phone-number-gateway", "phone-number-verification")
      val url = s"${config.verifyBaseUrl}$path"

      connector.forward(request, url, config.internalAuthToken)
  }

  def checkConnectivity(): Unit =
    val url = s"${config.verifyBaseUrl}/phone-number/verify"
    connector.checkConnectivity(url, config.internalAuthToken).map { result =>
      if (result)
        logger.info("Connectivity to phone-number-verification established")
      else
        logger.error("ERROR: Could not connect to phone-number-verification")
    }

  checkConnectivity()
