/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import config.FrontendAppConfig
import connectors.CrossBorderArrangementsConnector
import controllers.actions.{DataRetrievalAction, IdentifierAction}
import javax.inject.Inject
import models.UserAnswers
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

class IndexController @Inject()(
                                 sessionRepository: SessionRepository,
                                 identify: IdentifierAction,
                                 getData: DataRetrievalAction,
                                 frontendAppConfig: FrontendAppConfig,
                                 crossBorderArrangementsConnector: CrossBorderArrangementsConnector,
                                 val controllerComponents: MessagesControllerComponents,
                                 renderer: Renderer
                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData).async { implicit request =>
    {

      val userAnswers = request.userAnswers.getOrElse[UserAnswers](UserAnswers(request.internalId))

      for {
        noOfPreviousSubmissions <- crossBorderArrangementsConnector.findNoOfPreviousSubmissions(request.enrolmentID)
        _                       <- sessionRepository.set(userAnswers)
      } yield {

        val enterUrl = if (frontendAppConfig.contactUsToggle) {
          routes.ContactUsToUseManualServiceController.onPageLoad().url
        } else {
          frontendAppConfig.dacManualUrl
        }

        val context = Json.obj(
          "hasSubmissions" -> (noOfPreviousSubmissions > 0),
          "contactDetailsToggle" -> frontendAppConfig.contactDetailsToggle,
          "enterUrl" -> enterUrl,
          "manualJourneyToggle" -> frontendAppConfig.manualJourneyToggle,
          "recruitmentBannerToggle" -> frontendAppConfig.recruitmentBannerToggle
        )
        renderer.render("index.njk", context).map(Ok(_))
      }
    }.flatten
  }
}
