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

package helpers

import com.google.inject.Inject
import models.subscription.{ContactInformation, ContactInformationForIndividual, ContactInformationForOrganisation, ResponseDetail}
import models.{GenericError, SubmissionHistory, UserAnswers}
import pages.DisplaySubscriptionDetailsPage
import pages.contactdetails._
import play.api.i18n.Messages
import play.api.libs.json.JsValue
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Table.Cell
import uk.gov.hmrc.viewmodels.{Html, MessageInterpolators, Table}

import java.time.format.DateTimeFormatter


class ViewHelper @Inject()() {

  def linkToHomePageText(href: JsValue)(implicit messages: Messages): Html = {
    Html(s"<a id='homepage-link' href=$href class='govuk-link'>${{ messages("confirmation.link.text") }}</a>.")
  }

  def surveyLinkText(href: JsValue)(implicit messages: Messages): Html = {
    Html(s"<a id='feedback-link' href=$href class='govuk-link'>${{ messages("confirmation.survey.link")}}</a> ${{ messages("confirmation.survey.text")}}")
  }

  def mapErrorsToTable(listOfErrors: Seq[GenericError])(implicit messages: Messages) : Table = {

    val rows: Seq[Seq[Cell]] =
      for {
        error <- listOfErrors.sorted
      } yield {
        Seq(
          Cell(msg"${error.lineNumber}", classes = Seq("govuk-table__cell", "govuk-table__cell--numeric"), attributes = Map("id" -> s"lineNumber_${error.lineNumber}")),
          Cell(msg"${error.messageKey}", classes = Seq("govuk-table__cell"), attributes = Map("id" -> s"errorMessage_${error.lineNumber}"))
        )
      }

    Table(
      head = Seq(
        Cell(msg"invalidXML.table.heading1", classes = Seq("govuk-!-width-one-quarter", "govuk-table__header")),
        Cell(msg"invalidXML.table.heading2", classes = Seq("govuk-!-font-weight-bold"))),
      rows = rows,
      caption = Some(msg"invalidXML.h3"),
      attributes = Map("id" -> "errorTable"))
  }

  def buildDisclosuresTable(retrievedHistory: SubmissionHistory)(implicit messages: Messages) : Table = {

    val submissionDateFormat = DateTimeFormatter.ofPattern("h:mma 'on' d MMMM yyyy")

    val rows = retrievedHistory.details.zipWithIndex.map {
      case (submission, count) =>
        Seq(
          Cell(msg"${submission.arrangementID.get}", attributes = Map("id" -> s"arrangementID_$count")),
          Cell(msg"${submission.disclosureID.get}", attributes = Map("id" -> s"disclosureID_$count")),
          Cell(msg"${submission.submissionTime.format(submissionDateFormat)
                    .replace("AM", "am")
                    .replace("PM","pm")}", attributes = Map("id" -> s"submissionTime_$count")),
          Cell(msg"${submission.messageRefId}", attributes = Map("id" -> s"messageRef_$count"), classes = Seq("govuk-!-width-one-third", "breakString")),
          Cell(msg"${submission.importInstruction}", attributes = Map("id" -> s"disclosureType_$count"))
        )
    }

    Table(
      head = Seq(
        Cell(msg"submissionHistory.arn.label"),
        Cell(msg"submissionHistory.disclosureID.label"),
        Cell(msg"submissionHistory.submissionDate.label"),
        Cell(msg"submissionHistory.messageRef.label", classes = Seq("govuk-!-width-one-third")),
        Cell(msg"submissionHistory.disclosureType.label")
      ),
      rows = rows,
      caption = Some(msg"submissionHistory.caption"),
      attributes = Map("id" -> "disclosuresTable"))
  }

  def buildDisplaySubscription(responseDetail: ResponseDetail, hasSecondContact: Boolean): Table = {
    val rows =
      Seq(
        Seq(
          Cell(msg"displaySubscriptionForDAC.subscriptionID", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"${responseDetail.subscriptionID}", classes = Seq("govuk-!-width-one-third"),
            attributes = Map("id" -> "subscriptionID"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.tradingName", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"${responseDetail.tradingName.getOrElse("None")}", classes = Seq("govuk-!-width-one-third"),
            attributes = Map("id" -> "tradingName"))
        ),
        Seq(
          Cell(msg"displaySubscriptionForDAC.isGBUser", classes = Seq("govuk-!-width-one-third")),
          Cell(msg"${responseDetail.isGBUser}", classes = Seq("govuk-!-width-one-third"),
            attributes = Map("id" -> "isGBUser"))
        )
      ) ++ buildContactDetails(responseDetail.primaryContact.contactInformation)

    val updateRows = if (hasSecondContact) {
      rows ++ buildContactDetails(
        responseDetail.secondaryContact.fold(Seq[ContactInformation]())(p => p.contactInformation)
      )
    } else {
      rows
    }

    Table(
      head = Seq(
        Cell(msg"Information", classes = Seq("govuk-!-width-one-third")),
        Cell(msg"Value", classes = Seq("govuk-!-width-one-third"))
      ),
      rows = updateRows)
  }

  private def buildContactDetails(contactInformation: Seq[ContactInformation]): Seq[Seq[Cell]] = {
    contactInformation.head match {
      case ContactInformationForIndividual(individual, email, phone, mobile) =>
        Seq(
          Seq(
            Cell(msg"displaySubscriptionForDAC.individualContact", classes = Seq("govuk-!-width-one-third")),
            Cell(msg"${individual.firstName} ${individual.middleName.fold("")(mn => s"$mn ")}${individual.lastName}",
              classes = Seq("govuk-!-width-one-third"),
              attributes = Map("id" -> "individualContact"))
          ),
          Seq(
            Cell(msg"displaySubscriptionForDAC.individualEmail", classes = Seq("govuk-!-width-one-third")),
            Cell(msg"$email", classes = Seq("govuk-!-width-one-third"),
              attributes = Map("id" -> "individualEmail"))
          ),
          Seq(
            Cell(msg"displaySubscriptionForDAC.individualPhone", classes = Seq("govuk-!-width-one-third")),
            Cell(msg"${phone.getOrElse("None")}", classes = Seq("govuk-!-width-one-third"),
              attributes = Map("id" -> "individualPhone"))
          ),
          Seq(
            Cell(msg"displaySubscriptionForDAC.individualMobile", classes = Seq("govuk-!-width-one-third")),
            Cell(msg"${mobile.getOrElse("None")}", classes = Seq("govuk-!-width-one-third"),
              attributes = Map("id" -> "individualMobile"))
          )
        )
      case ContactInformationForOrganisation(organisation, email, phone, mobile) =>
        Seq(
          Seq(
            Cell(msg"displaySubscriptionForDAC.organisationContact", classes = Seq("govuk-!-width-one-third")),
            Cell(msg"${organisation.organisationName}",
              classes = Seq("govuk-!-width-one-third"),
              attributes = Map("id" -> "organisationContact"))
          ),
          Seq(
            Cell(msg"displaySubscriptionForDAC.organisationEmail", classes = Seq("govuk-!-width-one-third")),
            Cell(msg"$email", classes = Seq("govuk-!-width-one-third"),
              attributes = Map("id" -> "organisationEmail"))
          ),
          Seq(
            Cell(msg"displaySubscriptionForDAC.organisationPhone", classes = Seq("govuk-!-width-one-third")),
            Cell(msg"${phone.getOrElse("None")}", classes = Seq("govuk-!-width-one-third"),
              attributes = Map("id" -> "organisationPhone"))
          ),
          Seq(
            Cell(msg"displaySubscriptionForDAC.organisationMobile", classes = Seq("govuk-!-width-one-third")),
            Cell(msg"${mobile.getOrElse("None")}", classes = Seq("govuk-!-width-one-third"),
              attributes = Map("id" -> "organisationMobile"))
          )
        )
    }
  }

  def primaryContactPhoneExists(contactInformation: Seq[ContactInformation], userAnswers: UserAnswers): Boolean = {
    userAnswers.get(HaveContactPhonePage) match {
      case Some(value) => value
      case None =>
        contactInformation.head match {
          case ContactInformationForIndividual(_, _, phone, _) if phone.isDefined => true
          case ContactInformationForOrganisation(_, _, phone, _) if phone.isDefined => true
          case _ => false
        }
    }
  }

  def secondaryContactPhoneExists(contactInformation: Seq[ContactInformation], userAnswers: UserAnswers): Boolean = {
    userAnswers.get(HaveSecondaryContactPhonePage) match {
      case Some(value) => value
      case None if contactInformation.nonEmpty => true
      case None => false
    }
  }

  def isOrganisation(contactInformation: Seq[ContactInformation]): Boolean = {
    contactInformation.head match {
      case ContactInformationForIndividual(_, _, _, _) => false
      case ContactInformationForOrganisation(_, _, _, _) => true
    }
  }

  def retrieveContactName(contactInformation: Seq[ContactInformation]): String = {
    contactInformation.head match {
      case ContactInformationForIndividual(individual, _, _, _) =>
        s"${individual.firstName} ${individual.middleName.fold("")(mn => s"$mn ")}${individual.lastName}"
      case ContactInformationForOrganisation(organisation, _, _, _) =>
        s"${organisation.organisationName}"
    }
  }

  def retrieveContactEmail(contactInformation: Seq[ContactInformation]): String = {
    contactInformation.head match {
      case ContactInformationForIndividual(_, email, _, _) => email
      case ContactInformationForOrganisation(_, email, _, _) => email
    }
  }

  def retrieveContactPhone(contactInformation: Seq[ContactInformation]): String = {
    contactInformation.head match {
      case ContactInformationForIndividual(_, _, phone, _) => s"${phone.getOrElse("")}"
      case ContactInformationForOrganisation(_, _, phone, _) => s"${phone.getOrElse("")}"
    }
  }

  def getPrimaryContactName(userAnswers: UserAnswers): String = {
    (userAnswers.get(ContactNamePage), userAnswers.get(DisplaySubscriptionDetailsPage)) match {
      case (Some(contactName), _) => contactName
      case (None, Some(displaySubscription)) =>
        retrieveContactName(
          displaySubscription.displaySubscriptionForDACResponse.responseDetail.primaryContact.contactInformation)
      case _ => "your first contact"
    }
  }

  def getSecondaryContactName(userAnswers: UserAnswers): String = {
    (userAnswers.get(SecondaryContactNamePage), userAnswers.get(DisplaySubscriptionDetailsPage)) match {
      case (Some(secondaryContactName), _) => secondaryContactName
      case (None, Some(displaySubscription)) =>
        retrieveContactName(displaySubscription.displaySubscriptionForDACResponse.responseDetail.secondaryContact
          .fold(Seq[ContactInformation]())(_.contactInformation))
      case _ => "your second contact"
    }
  }


  def primaryContactName(responseDetail: ResponseDetail, userAnswers: UserAnswers): Option[Row] = {
    val contactName = userAnswers.get(ContactNamePage) match {
      case Some(contactName) => contactName
      case None => retrieveContactName(responseDetail.primaryContact.contactInformation)
    }

    if (isOrganisation(responseDetail.primaryContact.contactInformation)) {
      Some(
        Row(
          key = Key(msg"contactDetails.primaryContactName.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
          value = Value(lit"$contactName"),
          actions = List(
            Action(
              content = msg"site.edit",
              href = controllers.contactdetails.routes.ContactNameController.onPageLoad().url,
              visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.primaryContactName.checkYourAnswersLabel")),
              attributes = Map("id" -> "change-primary-contact-name")
            )
          )
        )
      )
    } else {
      None
    }

  }

  def primaryContactEmail(responseDetail: ResponseDetail, userAnswers: UserAnswers): Row = {
    val contactEmail = userAnswers.get(ContactEmailAddressPage) match {
      case Some(email) => email
      case None => retrieveContactEmail(responseDetail.primaryContact.contactInformation)
    }

    Row(
      key = Key(msg"contactDetails.primaryContactEmail.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
      value = Value(lit"$contactEmail"),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.contactdetails.routes.ContactEmailAddressController.onPageLoad().url,
          visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.primaryContactEmail.checkYourAnswersLabel")),
          attributes = Map("id" -> "change-primary-contact-email")
        )
      )
    )
  }

  def haveContactPhoneNumber(responseDetail: ResponseDetail, userAnswers: UserAnswers): Row = {
    val haveContactPhoneNumber =
      if (primaryContactPhoneExists(responseDetail.primaryContact.contactInformation, userAnswers)) {
        "site.yes"
      } else {
        "site.no"
      }

    Row(
      key = Key(msg"contactDetails.haveContactPhone.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
      value = Value(msg"$haveContactPhoneNumber"),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.contactdetails.routes.HaveContactPhoneController.onPageLoad().url,
          visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.haveContactPhone.checkYourAnswersLabel")),
          attributes = Map("id" -> "change-have-contact-phone-number")
        )
      )
    )
  }

  def primaryPhoneNumber(responseDetail: ResponseDetail, userAnswers: UserAnswers): Option[Row] = {
    if (primaryContactPhoneExists(responseDetail.primaryContact.contactInformation, userAnswers)) {
      val phoneNumber = userAnswers.get(ContactTelephoneNumberPage) match {
        case Some(telephone) => telephone
        case None => retrieveContactPhone(responseDetail.primaryContact.contactInformation)
      }

      Some(
        Row(
          key = Key(msg"contactDetails.primaryPhoneNumber.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
          value = Value(lit"$phoneNumber"),
          actions = List(
            Action(
              content = msg"site.edit",
              href = controllers.contactdetails.routes.ContactTelephoneNumberController.onPageLoad().url,
              visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.primaryPhoneNumber.checkYourAnswersLabel")),
              attributes = Map("id" -> "change-primary-phone-number")
            )
          )
        )
      )
    } else {
      None
    }
  }

  def haveSecondaryContact(responseDetail: ResponseDetail, userAnswers: UserAnswers): Row = {
    val contactInformationList = responseDetail.secondaryContact.fold(Seq[ContactInformation]())(sc => sc.contactInformation)

    val haveSecondaryContact = userAnswers.get(HaveSecondContactPage) match {
      case Some(true) => "site.yes"
      case None if contactInformationList.nonEmpty => "site.yes"
      case _ => "site.no"
    }

    Row(
      key = Key(msg"contactDetails.haveSecondContact.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
      value = Value(msg"$haveSecondaryContact"),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.contactdetails.routes.HaveSecondContactController.onPageLoad().url,
          visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.haveSecondContact.checkYourAnswersLabel")),
          attributes = Map("id" -> "change-have-second-contact")
        )
      )
    )
  }

  def secondaryContactName(responseDetail: ResponseDetail, userAnswers: UserAnswers): Row = {
    //Note: Secondary contact name is only one field in the registration journey. It's always ContactInformationForOrganisation
    val contactName = userAnswers.get(SecondaryContactNamePage) match {
      case Some(contactName) => contactName
      case None =>
        val contactInformationList =
          responseDetail.secondaryContact.fold(Seq[ContactInformation]())(_.contactInformation)

        retrieveContactName(contactInformationList)
    }

    Row(
      key = Key(msg"contactDetails.secondaryContactName.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
      value = Value(lit"$contactName"),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.contactdetails.routes.SecondaryContactNameController.onPageLoad().url,
          visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.secondaryContactName.checkYourAnswersLabel")),
          attributes = Map("id" -> "change-secondary-contact-name")
        )
      )
    )
  }

  def secondaryContactEmail(responseDetail: ResponseDetail, userAnswers: UserAnswers): Row = {
    val contactEmail = userAnswers.get(SecondaryContactEmailAddressPage) match {
      case Some(email) => email
      case None =>
        val contactInformationList =
          responseDetail.secondaryContact.fold(Seq[ContactInformation]())(sc => sc.contactInformation)

        retrieveContactEmail(contactInformationList)
    }

    Row(
      key = Key(msg"contactDetails.secondaryContactEmail.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
      value = Value(lit"$contactEmail"),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.contactdetails.routes.SecondaryContactEmailAddressController.onPageLoad().url,
          visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.secondaryContactEmail.checkYourAnswersLabel")),
          attributes = Map("id" -> "change-secondary-contact-email")
        )
      )
    )
  }

  def haveSecondaryContactPhone(responseDetail: ResponseDetail, userAnswers: UserAnswers): Row = {
    val contactInformationList = responseDetail.secondaryContact.fold(Seq[ContactInformation]())(sc => sc.contactInformation)

    val haveSecondaryContactPhone =
      if (secondaryContactPhoneExists(contactInformationList, userAnswers)) {
        "site.yes"
      } else {
        "site.no"
      }

    Row(
      key = Key(msg"contactDetails.haveSecondContactPhone.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
      value = Value(msg"$haveSecondaryContactPhone"),
      actions = List(
        Action(
          content = msg"site.edit",
          href = controllers.contactdetails.routes.HaveSecondaryContactPhoneController.onPageLoad().url,
          visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.haveSecondContactPhone.checkYourAnswersLabel")),
          attributes = Map("id" -> "change-have-second-contact-phone")
        )
      )
    )
  }

  def secondaryPhoneNumber(responseDetail: ResponseDetail, userAnswers: UserAnswers): Option[Row] = {
    val contactInformationList = responseDetail.secondaryContact.fold(Seq[ContactInformation]())(sc => sc.contactInformation)

    if (secondaryContactPhoneExists(contactInformationList, userAnswers)) {
      val phoneNumber = userAnswers.get(SecondaryContactTelephoneNumberPage) match {
        case Some(telephone) => telephone
        case None =>
          retrieveContactPhone(contactInformationList)
      }

      Some(
        Row(
          key = Key(msg"contactDetails.secondaryContactPhoneNumber.checkYourAnswersLabel", classes = Seq("govuk-!-width-one-third")),
          value = Value(lit"$phoneNumber"),
          actions = List(
            Action(
              content = msg"site.edit",
              href = controllers.contactdetails.routes.SecondaryContactTelephoneNumberController.onPageLoad().url,
              visuallyHiddenText = Some(msg"site.edit.hidden".withArgs(msg"contactDetails.secondaryContactPhoneNumber.checkYourAnswersLabel")),
              attributes = Map("id" -> "change-secondary-phone-number")
            )
          )
        )
      )
    } else {
      None
    }
  }

}
