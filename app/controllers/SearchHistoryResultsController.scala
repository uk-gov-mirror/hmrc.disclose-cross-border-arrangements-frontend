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
import controllers.actions._
import helpers.ViewHelper

import javax.inject.Inject
import pages.HistoryPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

class SearchHistoryResultsController @Inject()(
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    crossBorderArrangementsConnector: CrossBorderArrangementsConnector,
    val controllerComponents: MessagesControllerComponents,
    appConfig: FrontendAppConfig,
    viewHelper: ViewHelper,
    renderer: Renderer
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val searchCriteria = request.userAnswers.get(HistoryPage) match {
        case None => ""
        case Some(value) => value
      }

      {for {
        retrievedDetails <- crossBorderArrangementsConnector.searchDisclosures(searchCriteria)
        context = Json.obj(
          "disclosuresTable" -> viewHelper.buildDisclosuresTable(retrievedDetails),
          "searchAgainPageLink" -> appConfig.searchAgainLink,
          "homePageLink" -> appConfig.discloseArrangeLink
        )
      } yield {
        renderer.render("submissionHistorySearchResults.njk", context).map(Ok(_))
      }}.flatten
  }
}
