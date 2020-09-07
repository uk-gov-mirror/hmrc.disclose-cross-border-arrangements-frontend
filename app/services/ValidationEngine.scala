/*
 * Copyright 2020 HM Revenue & Customs
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

package services

import helpers.{BusinessRulesErrorMessageHelper, XmlErrorMessageHelper}
import javax.inject.Inject
import models.{Dac6MetaData, ValidationFailure, ValidationSuccess, XMLValidationStatus}
import models.{GenericError, SaxParseError, ValidationFailure, ValidationSuccess, XMLValidationStatus}
import org.scalactic.ErrorMessage
import uk.gov.hmrc.http.HeaderCarrier
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}
import scala.xml.Elem


class ValidationEngine @Inject()(xmlValidationService: XMLValidationService,
                                 businessRuleValidationService: BusinessRuleValidationService,
                                 xmlErrorMessageHelper: XmlErrorMessageHelper,
                                 businessRulesErrorMessageHelper: BusinessRulesErrorMessageHelper,
                                 metaDataValidationService: MetaDataValidationService) {

  private val logger = LoggerFactory.getLogger(getClass)


  def validateFile(downloadUrl: String, enrolmentId: String, businessRulesCheckRequired: Boolean = true)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext ): Future[Either[Exception, XMLValidationStatus]] = {

    try {
      val xmlAndXmlValidationStatus: (Elem, XMLValidationStatus) = performXmlValidation(downloadUrl)

    val businessRulesValidationResult: XMLValidationStatus = performBusinessRulesValidation(downloadUrl, xmlAndXmlValidationStatus._1, businessRulesCheckRequired)
    val metaData = businessRuleValidationService.extractDac6MetaData()(xmlAndXmlValidationStatus._1)

    metaDataValidationService.verifyMetaData(downloadUrl, xmlAndXmlValidationStatus._1, metaData, enrolmentId) map { idVerificationResult =>

      combineResults(xmlAndXmlValidationStatus._2, businessRulesValidationResult, idVerificationResult) match {
        case ValidationFailure(errors) => Right(ValidationFailure(errors))

        case ValidationSuccess(_,_)=> Right(ValidationSuccess(downloadUrl, metaData))
      }
    }
    } catch {
      case e: Exception =>
        logger.warn(s"XML validation failed. The XML parser has thrown the exception: $e")
       Future(Left(e))

    }
  }

  private def combineResults(xmlResult: XMLValidationStatus, businessRulesResult: XMLValidationStatus,
                             idResult: XMLValidationStatus): XMLValidationStatus = {

      val xmlErrors = xmlResult match {
        case ValidationSuccess(_, _) => List()
        case ValidationFailure(errors) => errors
      }

    val businessRulesErrors = businessRulesResult match {
        case ValidationSuccess(_, _) => List()
        case ValidationFailure(errors) => errors
      }

    val idErrors = idResult match {
      case ValidationSuccess(_, _) => List()
      case ValidationFailure(errors) => errors
    }

    val combinedErrors = (xmlErrors ++ businessRulesErrors ++ idErrors).sortBy(_.lineNumber)

    if (combinedErrors.isEmpty){
      ValidationSuccess("", None)

    } else ValidationFailure(combinedErrors)


 }

  def performXmlValidation(source: String): (Elem, XMLValidationStatus) = {

    val xmlErrors = xmlValidationService.validateXml(source)
    if(xmlErrors._2.isEmpty) {
      (xmlErrors._1, ValidationSuccess(source))
    } else {

      val filteredErrors = xmlErrorMessageHelper.generateErrorMessages(xmlErrors._2)

      (xmlErrors._1,  ValidationFailure(filteredErrors))
    }
  }

  def performBusinessRulesValidation(source: String, elem: Elem, businessRulesCheckRequired: Boolean): XMLValidationStatus = {

    if (businessRulesCheckRequired) {
      businessRuleValidationService.validateFile()(elem) match {
        case Some(List()) => ValidationSuccess(source)
        case Some(errors) => ValidationFailure(businessRulesErrorMessageHelper.convertToGenericErrors(errors, elem))
        case None => ValidationSuccess(source)
      }
    } else {
      ValidationSuccess(source)
    }
  }

}
