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
import connectors.SubscriptionConnector
import controllers.actions._
import handlers.ErrorHandler
import helpers.ViewHelper
import models.subscription.{ContactInformationForIndividual, ContactInformationForOrganisation}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContactDetailsController @Inject()(
    override val messagesApi: MessagesApi,
    subscriptionConnector: SubscriptionConnector,
    appConfig: FrontendAppConfig,
    errorHandler: ErrorHandler,
    viewHelper: ViewHelper,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    val controllerComponents: MessagesControllerComponents,
    renderer: Renderer
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      subscriptionConnector.displaySubscriptionDetails(request.enrolmentID).flatMap {
        details =>

          if (details.isLocked) {
            Future.successful(Redirect(routes.DetailsAlreadyUpdatedController.onPageLoad()))
          } else {
            if (details.subscriptionDetails.isDefined) {
              val responseDetail = details.subscriptionDetails.get.displaySubscriptionForDACResponse.responseDetail

              val isOrganisation = responseDetail.primaryContact.contactInformation.head match {
                case ContactInformationForIndividual(_, _, _, _) => false
                case ContactInformationForOrganisation(_, _, _, _) => true
              }

              val contactDetailsList =
                if (responseDetail.secondaryContact.isDefined) {
                  Seq(
                    viewHelper.primaryContactName(responseDetail, request.userAnswers),
                    viewHelper.primaryContactEmail(responseDetail, request.userAnswers),
                    viewHelper.haveContactPhoneNumber(responseDetail, request.userAnswers),
                    viewHelper.primaryPhoneNumber(responseDetail, request.userAnswers)
                  )
                } else {
                  Seq(
                    viewHelper.primaryContactName(responseDetail, request.userAnswers),
                    viewHelper.primaryContactEmail(responseDetail, request.userAnswers),
                    viewHelper.primaryPhoneNumber(responseDetail, request.userAnswers)
                  )
                }

              val secondaryContactDetailsList =
                if (responseDetail.secondaryContact.isDefined) {
                  Some(
                    Seq(
                      viewHelper.haveSecondaryContact(responseDetail, request.userAnswers),
                      viewHelper.secondaryContactName(responseDetail, request.userAnswers),
                      viewHelper.secondaryContactEmail(responseDetail, request.userAnswers),
                      viewHelper.haveSecondaryContactPhone(responseDetail, request.userAnswers),
                      viewHelper.secondaryPhoneNumber(responseDetail, request.userAnswers)
                    )
                  )
                } else {
                  None
                }

              val json = {
                if (secondaryContactDetailsList.isDefined) {
                  Json.obj(
                    "contactDetails" -> contactDetailsList,
                    "additionalContact" -> true,
                    "secondaryContactDetails" -> secondaryContactDetailsList.get,
                    "isOrganisation" -> isOrganisation,
                    "homePageLink" -> appConfig.discloseArrangeLink,
                    "changeProvided" -> false
                  )
                } else {
                  Json.obj(
                    "contactDetails" -> contactDetailsList,
                    "additionalContact" -> false,
                    "isOrganisation" -> isOrganisation,
                    "homePageLink" -> appConfig.discloseArrangeLink,
                    "changeProvided" -> false
                  )
                }

              }

              renderer.render("contactDetails.njk", json).map(Ok(_))
            } else {
              errorHandler.onServerError(request, new Exception("Conversion of display subscription payload failed"))
            }
          }
      }
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      subscriptionConnector.displaySubscriptionDetails(request.enrolmentID).flatMap {
        details =>
          if (details.subscriptionDetails.isDefined) {
            subscriptionConnector.updateSubscription(details.subscriptionDetails.get.displaySubscriptionForDACResponse, request.userAnswers)
              .map { _ =>
                Redirect(routes.IndexController.onPageLoad())
              }
          } else {
            errorHandler.onServerError(request, new Exception("Conversion of display/update subscription payload failed"))
          }
      }
  }
}
