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

package models

import scala.xml.Elem

case class Validation(key: String, value: Boolean, lineNumber: Option[Int] = None) {

  def toSaxParseError: SaxParseError = SaxParseError(1, key)

  def setLineNumber(xmlArray: Array[String]) ={
    val index = xmlArray.indexWhere(str => str.contains(path)) + 1
    copy(lineNumber = Some(index))
  }

  def path: String = {
    key match {

      case "businessrules.initialDisclosure.needRelevantTaxPayer" => "InitialDisclosureMA"
      case "businessrules.relevantTaxpayerDiscloser.needRelevantTaxPayer" => "RelevantTaxpayerDiscloser"
      case "businessrules.intermediaryDiscloser.needIntermediary" => "IntermediaryDiscloser"
      case "businessrules.taxPayerImplementingDates.needToBeAfterStart" => "TaxpayerImplementingDate"
      case "businessrules.implementingDates.needToBeAfterStart" => "ImplementingDate"
      case "businessrules.initialDisclosureMA.allRelevantTaxPayersHaveTaxPayerImplementingDate" => "InitialDisclosureMA"
      case  "businessrules.mainBenefitTest1.oneOfSpecificHallmarksMustBePresent" => "MainBenefitTest1"
      case "businessrules.dac6D10OtherInfo.needHallMarkToProvideInfo" => "DAC6D1OtherInfo"

      case  _ => "DisclosureImportInstruction"




    }
  }



  }





