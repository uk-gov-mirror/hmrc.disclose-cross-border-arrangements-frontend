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

package services

import java.text.SimpleDateFormat
import java.util.{Calendar, Date, GregorianCalendar}

import cats.data.ReaderT
import cats.implicits._
import connectors.CrossBorderArrangementsConnector
import javax.inject.Inject
import models.{Dac6MetaData, Validation}
import org.slf4j.LoggerFactory
import services.BusinessRuleValidationService.disclosureID
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}
import scala.xml.NodeSeq

class BusinessRuleValidationService @Inject()(crossBorderArrangementsConnector: CrossBorderArrangementsConnector) {

  import BusinessRuleValidationService._

  private val logger = LoggerFactory.getLogger(getClass)

    def validateInitialDisclosureHasRelevantTaxPayer()
     (implicit hc: HeaderCarrier, ec: ExecutionContext): ReaderT[Option, NodeSeq, Future[Validation]] = {

    for {
      disclosureImportInstruction <- disclosureImportInstruction
      isInitialDisclosureMA <- isInitialDisclosureMA
      noOfRelevantTaxPayers <- noOfRelevantTaxPayers
      arrangementID <- arrangementID
      disclosureID <- disclosureID
    } yield {

      val initialDisclosureMaValue = if (disclosureImportInstruction.equals("DAC6REP")) {

        crossBorderArrangementsConnector.retrieveFirstDisclosureForArrangementID(arrangementID).map(
          submissionDetails => {

            if(submissionDetails.disclosureID.contains(disclosureID)) {
              submissionDetails.initialDisclosureMA
            } else isInitialDisclosureMA
          }
        ).recover {
          case _ => false
        }
      } else
        if(disclosureImportInstruction.equals("DAC6DEL")){
          Future(true)
        }else Future(isInitialDisclosureMA)

      initialDisclosureMaValue.map { shouldBeTreatedAsMa =>
        Validation(
          key = "businessrules.initialDisclosure.needRelevantTaxPayer",
          value = if (!shouldBeTreatedAsMa) noOfRelevantTaxPayers > 0 else true
        )
      }
    }
  }

  def validateRelevantTaxpayerDiscloserHasRelevantTaxPayer(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      hasRelevantTaxpayerDiscloser <- hasRelevantTaxpayerDiscloser
      noOfRelevantTaxPayers <- noOfRelevantTaxPayers
    } yield
      Validation(
        key = "businessrules.relevantTaxpayerDiscloser.needRelevantTaxPayer",
        value = if (hasRelevantTaxpayerDiscloser) noOfRelevantTaxPayers > 0 else true
      )
  }

  def validateIntermediaryDiscloserHasIntermediary(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      hasIntermediaryDiscloser <- hasIntermediaryDiscloser
      noOfIntermediaries <- noOfIntermediaries
    } yield
      Validation(
        key = "businessrules.intermediaryDiscloser.needIntermediary",
        value = if (hasIntermediaryDiscloser) noOfIntermediaries > 0 else true
      )
  }

  def validateAllTaxpayerImplementingDatesAreAfterStart(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      taxPayerImplementingDates <- taxPayerImplementingDates
    } yield
      Validation(
        key = "businessrules.taxPayerImplementingDates.needToBeAfterStart",
        value = taxPayerImplementingDates.forall(implDate => !implDate.before(implementationStartDate))
      )
  }

  def validateAllImplementingDatesAreAfterStart(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      disclosureInformationImplementingDates <- disclosureInformationImplementingDates
    } yield
      Validation(
        key = "businessrules.implementingDates.needToBeAfterStart",
        value = disclosureInformationImplementingDates.forall(implDate => !implDate.before(implementationStartDate))
      )
  }

  def validateDisclosureImportInstruction(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      disclosureImportInstruction <- disclosureImportInstruction
      arrangementID <- arrangementID
      disclosureID <- disclosureID
      messageRefID <- messageRefID
    } yield
      disclosureImportInstruction match {
        case "DAC6NEW" => Validation(
          key = "businessrules.newDisclosure.mustNotHaveArrangementIDOrDisclosureID",
          value = arrangementID.isEmpty && disclosureID.isEmpty)
        case "DAC6ADD" => Validation(
          key = "businessrules.addDisclosure.mustHaveArrangementIDButNotDisclosureID",
          value = arrangementID.nonEmpty && disclosureID.isEmpty)
        case "DAC6REP" => Validation(
          key = "businessrules.repDisclosure.mustHaveArrangementIDDisclosureIDAndMessageRefID",
          value = arrangementID.nonEmpty && disclosureID.nonEmpty && messageRefID.nonEmpty)
        case "DAC6DEL" => Validation(
          key = "businessrules.delDisclosure.mustHaveArrangementIDDisclosureIDAndMessageRefID",
          value = arrangementID.nonEmpty && disclosureID.nonEmpty && messageRefID.nonEmpty)
        case _ => Validation(
          key = "businessrules.disclosure.notAValidDisclosureInstruction",
          value = false) //TODO: This is because I haven't used an enum
      }
  }

  def validateDisclosureImportInstructionAndInitialDisclosureFlag(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      disclosureImportInstruction <- disclosureImportInstruction
      initialDisclosureMA <- isInitialDisclosureMA
    } yield
      disclosureImportInstruction match {
        case "DAC6ADD" => Validation(
          key = "businessrules.addDisclosure.mustNotBeInitialDisclosureMA",
          value = !initialDisclosureMA)
        case _ => Validation(
          key = "businessrules.addDisclosure.mustHaveArrangementIDButNotDisclosureID",
          value = true)

      }
  }

  def validateHallmarks(): ReaderT[Option, NodeSeq, Validation] = {

    val valuesForHallmarkD = Set("DAC6D1Other", "DAC6D1a", "DAC6D1b", "DAC6D1c",
      "DAC6D1d", "DAC6D1e", "DAC6D1f", "DAC6D2")

    for {
      hallmarks <- hallmarks
    } yield {
      val nonHallMarkDExists = hallmarks.exists(value => !valuesForHallmarkD.contains(value))
      val hallmarkDValues = hallmarks.toSet.intersect(valuesForHallmarkD)
      if (nonHallMarkDExists && hallmarkDValues.nonEmpty) {
        Validation(key = "businessrules.hallmarks.dHallmarkWithOtherHallmarks",
          value = false)
      }
      else {
        Validation(
          key = "businessrules.hallmarks.dHallmarkNotProvided",
          value = if (hallmarks.nonEmpty) {
            hallmarkDValues.nonEmpty
          } else true)
      }
    }
  }
  def validateDAC6D1OtherInfoHasNecessaryHallmark(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      hasDAC6D1OtherInfo <- hasDAC6D1OtherInfo
      hallmarks <- hallmarks
    } yield Validation(
      key = "businessrules.dac6D10OtherInfo.needHallMarkToProvideInfo",
      value = if (hasDAC6D1OtherInfo)
        hallmarks.contains("DAC6D1Other")
      else true
    )
  }

  def validateRelevantTaxPayerDatesOfBirths(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      datesOfBirth <- relevantTaxPayerDatesOfBirth
    } yield
      Validation(
        key = "businessrules.RelevantTaxPayersBirthDates.maxDateOfBirthExceeded",
        value = !datesOfBirth.exists(date => date.before(maxBirthDate))
      )
  }

  def validateDisclosingDatesOfBirth(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      datesOfBirth <- disclosingDatesOfBirth
    } yield
      Validation(
        key = "businessrules.DisclosingBirthDates.maxDateOfBirthExceeded",
        value = !datesOfBirth.exists(date => date.before(maxBirthDate))
      )
  }

  def validateAssociatedEnterprisesDatesOfBirth(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      datesOfBirth <- associatedEnterprisesDatesOfBirth
    } yield
      Validation(
        key = "businessrules.AssociatedEnterprisesBirthDates.maxDateOfBirthExceeded",
        value = !datesOfBirth.exists(date => date.before(maxBirthDate))
      )
  }

  def validateIntermediaryDatesOfBirth(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      datesOfBirth <- intermediaryDatesOfBirth
    } yield
      Validation(
        key = "businessrules.IntermediaryBirthDates.maxDateOfBirthExceeded",
        value = !datesOfBirth.exists(date => date.before(maxBirthDate))
      )
  }

  def validateAffectedPersonsDatesOfBirth(): ReaderT[Option, NodeSeq, Validation] = {
    for {
      datesOfBirth <- affectedPersonsDatesOfBirth
    } yield
      Validation(
        key = "businessrules.AffectedPersonsBirthDates.maxDateOfBirthExceeded",
        value = !datesOfBirth.exists(date => date.before(maxBirthDate))
      )
  }

  def validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()
     (implicit hc: HeaderCarrier, ec: ExecutionContext): ReaderT[Option, NodeSeq, Future[Validation]] = {
    for {
      disclosureImportInstruction <- disclosureImportInstruction
      relevantTaxPayers <- noOfRelevantTaxPayers
      taxPayerImplementingDate <- taxPayerImplementingDates
      arrangementID <- arrangementID
      isInitialDisclosureMA <- isInitialDisclosureMA

    } yield {
      if ((disclosureImportInstruction == "DAC6ADD") || (disclosureImportInstruction == "DAC6REP")) {
        crossBorderArrangementsConnector.retrieveFirstDisclosureForArrangementID(arrangementID).map {
          submissionDetails =>
            submissionDetails.initialDisclosureMA match {
              case true => Validation(
                key = "businessrules.initialDisclosureMA.firstDisclosureHasInitialDisclosureMAAsTrue",
                value = if (submissionDetails.initialDisclosureMA && relevantTaxPayers > 0) {
                  relevantTaxPayers == taxPayerImplementingDate.length
                }
                else {
                  true
                }
              )
              case false => Validation(
                key = "businessrules.nonMA.cantHaveRelevantTaxPayer",
                value = if (relevantTaxPayers > 0) {
                  taxPayerImplementingDate.isEmpty
                } else true
              )
            }

        }.recover {
          case _ =>
            logger.info("No first disclosure found")
            Validation(
              key = "businessrules.initialDisclosureMA.firstDisclosureHasInitialDisclosureMAAsTrue",
              value = true)
        }
      } else {
        val key = if (isInitialDisclosureMA) {
          "businessrules.initialDisclosureMA.missingRelevantTaxPayerDates"
        } else "businessrules.nonMA.cantHaveRelevantTaxPayer"

        val value = if (disclosureImportInstruction == "DAC6NEW" && relevantTaxPayers > 0 && isInitialDisclosureMA) {
          taxPayerImplementingDate.length == relevantTaxPayers
        } else if (disclosureImportInstruction == "DAC6NEW" && relevantTaxPayers > 0) {
          taxPayerImplementingDate.isEmpty
        } else true


        Future(Validation(
          key = key,
          value = value
        ))
      }
    }
  }

  def extractDac6MetaData(): ReaderT[Option, NodeSeq, Dac6MetaData] = {
    for {
      disclosureImportInstruction <- disclosureImportInstruction
      arrangementID <- arrangementID
      disclosureID <- disclosureID
      disclosureInformation <- disclosureInformation
      isInitialDisclosureMA <- isInitialDisclosureMA
      messageRefId <- messageRefID
    } yield {

      val infoPresent = disclosureInformation.nonEmpty

      disclosureImportInstruction match {
        case "DAC6NEW" => Dac6MetaData(disclosureImportInstruction, disclosureInformationPresent = infoPresent, initialDisclosureMA = isInitialDisclosureMA, messageRefId = messageRefId)
        case "DAC6ADD" => Dac6MetaData(disclosureImportInstruction, Some(arrangementID), disclosureInformationPresent = infoPresent, initialDisclosureMA = isInitialDisclosureMA, messageRefId = messageRefId)
        case "DAC6REP" => Dac6MetaData(disclosureImportInstruction, Some(arrangementID), Some(disclosureID), disclosureInformationPresent = infoPresent, initialDisclosureMA = isInitialDisclosureMA, messageRefId = messageRefId)
        case "DAC6DEL" => Dac6MetaData(disclosureImportInstruction, Some(arrangementID), Some(disclosureID), disclosureInformationPresent = infoPresent, initialDisclosureMA = isInitialDisclosureMA, messageRefId = messageRefId)
        case _ => throw new RuntimeException("XML Data extraction failed - disclosure import instruction Missing")
      }
    }
  }

  def validateFile()(implicit hc: HeaderCarrier, ec: ExecutionContext): ReaderT[Option, NodeSeq, Future[Seq[Validation]]] = {
    for {
      v1 <- validateInitialDisclosureHasRelevantTaxPayer()
      v2 <- validateRelevantTaxpayerDiscloserHasRelevantTaxPayer()
      v3 <- validateIntermediaryDiscloserHasIntermediary()
      v4 <- validateAllTaxpayerImplementingDatesAreAfterStart()
      v5 <- validateAllImplementingDatesAreAfterStart()
      v6 <- validateDisclosureImportInstruction()
      v7 <- validateDAC6D1OtherInfoHasNecessaryHallmark()
      v8 <- validateDisclosureImportInstructionAndInitialDisclosureFlag()
      v9 <- validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()
      v10 <- validateRelevantTaxPayerDatesOfBirths()
      v11 <- validateDisclosingDatesOfBirth()
      v12 <- validateIntermediaryDatesOfBirth()
      v13 <- validateAffectedPersonsDatesOfBirth()
      v14 <- validateAssociatedEnterprisesDatesOfBirth()
      v15 <- validateHallmarks()
    } yield {
      v1.flatMap { v1Validation =>
        v9.map { v9Validation =>
          Seq(v1Validation, v2, v3, v4, v5, v6, v7, v8, v9Validation, v10, v11, v12, v13, v14, v15).filterNot(_.value)
        }
      }
    }
  }
}
object BusinessRuleValidationService {
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  val implementationStartDate: Date = new GregorianCalendar(2018, Calendar.JUNE, 25).getTime
  val maxBirthDate: Date = new GregorianCalendar(1900, Calendar.JANUARY, 1).getTime

  val isInitialDisclosureMA: ReaderT[Option, NodeSeq, Boolean] =
    ReaderT[Option, NodeSeq, Boolean](xml => {
      (xml \\ "InitialDisclosureMA").text match {
        case "true" => Some(true)
        case "false" => Some(false)
        case _ => Some(false)
      }
    })

  val hasRelevantTaxpayerDiscloser: ReaderT[Option, NodeSeq, Boolean] =
    ReaderT[Option, NodeSeq, Boolean](xml =>
      Some((xml \\ "RelevantTaxpayerDiscloser").length > 0)
    )

  val hasIntermediaryDiscloser: ReaderT[Option, NodeSeq, Boolean] =
    ReaderT[Option, NodeSeq, Boolean](xml =>
      Some((xml \\ "IntermediaryDiscloser").length > 0)
    )

  val noOfIntermediaries: ReaderT[Option, NodeSeq, Int] =
    ReaderT[Option, NodeSeq, Int](xml =>
      Some((xml \\ "Intermediary").length)
    )

  val noOfRelevantTaxPayers: ReaderT[Option, NodeSeq, Int] =
    ReaderT[Option, NodeSeq, Int](xml => {
      Some((xml \\ "RelevantTaxpayer").length)
    })

  val relevantTaxPayerDatesOfBirth: ReaderT[Option, NodeSeq, Seq[Date]] =
    ReaderT[Option, NodeSeq, Seq[Date]](xml => {
      Some {
        (xml \\ "RelevantTaxPayers" \\ "RelevantTaxpayer"\\ "BirthDate")
          .map(_.text)
          .map(parseDate _)
      }
    })

  val disclosingDatesOfBirth: ReaderT[Option, NodeSeq, Seq[Date]] =
    ReaderT[Option, NodeSeq, Seq[Date]](xml => {
      Some {
        (xml \\ "Disclosing"\\ "BirthDate")
          .map(_.text)
          .map(parseDate _)
      }
    })

  val associatedEnterprisesDatesOfBirth: ReaderT[Option, NodeSeq, Seq[Date]] =
    ReaderT[Option, NodeSeq, Seq[Date]](xml => {
      Some {
        (xml \\ "AssociatedEnterprise"\\ "BirthDate")
          .map(_.text)
          .map(parseDate _)
      }
    })

  val intermediaryDatesOfBirth: ReaderT[Option, NodeSeq, Seq[Date]] =
    ReaderT[Option, NodeSeq, Seq[Date]](xml => {
      Some {
        (xml \\ "Intermediaries" \\ "Intermediary"\\ "BirthDate")
          .map(_.text)
          .map(parseDate _)
      }
    })

  val affectedPersonsDatesOfBirth: ReaderT[Option, NodeSeq, Seq[Date]] =
    ReaderT[Option, NodeSeq, Seq[Date]](xml => {
      Some {
        (xml \\ "AffectedPersons"\\ "BirthDate")
          .map(_.text)
          .map(parseDate _)
      }
    })


  val taxPayerImplementingDates: ReaderT[Option, NodeSeq, Seq[Date]] =
    ReaderT[Option, NodeSeq, Seq[Date]](xml => {
      Some {
        (xml \\ "TaxpayerImplementingDate")
          .map(_.text)
          .map(parseDate _)
      }
    })

  def parseDate(dateString: String): Date = {
    Try(dateFormat.parse(dateString)) match {
      case Success(date) => date
      case _ => new Date()
    }

  }

  val disclosureInformationImplementingDates: ReaderT[Option, NodeSeq, Seq[Date]] = {
  ReaderT[Option, NodeSeq, Seq[Date]](xml => {
      Some {
        (xml \\ "ImplementingDate")
          .map(_.text)
          .map(parseDate _)
      }
    }
    )
  }
  val disclosureImportInstruction: ReaderT[Option, NodeSeq, String] =
    ReaderT[Option, NodeSeq, String](xml => {
      Some((xml \\ "DisclosureImportInstruction").text)
    })

  val disclosureInformation: ReaderT[Option, NodeSeq, String] =
    ReaderT[Option, NodeSeq, String](xml => {
      Some((xml \\ "DisclosureInformation").text)
    })

  val disclosureID: ReaderT[Option, NodeSeq, String] =
    ReaderT[Option, NodeSeq, String](xml => {
      Some((xml \\ "DisclosureID").text)
    })

  val arrangementID: ReaderT[Option, NodeSeq, String] =
    ReaderT[Option, NodeSeq, String](xml => {
      Some((xml \\ "ArrangementID").text)
    })

  val messageRefID: ReaderT[Option, NodeSeq, String] =
    ReaderT[Option, NodeSeq, String](xml => {
      Some((xml \\ "MessageRefId").text)
    })

  val hallmarks: ReaderT[Option, NodeSeq, Seq[String]] =
    ReaderT[Option, NodeSeq, Seq[String]](xml => {
      Some((xml \\ "Hallmark").map(_.text))
    })

  val hasDAC6D1OtherInfo: ReaderT[Option, NodeSeq, Boolean] =
    ReaderT[Option, NodeSeq, Boolean](xml => {
      Some((xml \\ "DAC6D1OtherInfo").length > 0)
    })

}
