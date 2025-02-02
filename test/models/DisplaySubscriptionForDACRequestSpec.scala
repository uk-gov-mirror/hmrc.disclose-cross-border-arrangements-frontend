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

package models

import base.SpecBase
import helpers.JsonFixtures.{jsonForDisplaySubscriptionRequest, jsonPayloadForDisplaySubscriptionRequest}
import models.subscription.{DisplaySubscriptionDetails, DisplaySubscriptionForDACRequest, RequestCommon, RequestDetail}
import play.api.libs.json.Json

import scala.util.matching.Regex

class DisplaySubscriptionForDACRequestSpec extends SpecBase {

  val requestCommon: RequestCommon =
    RequestCommon(
      regime = "DAC",
      conversationID = Some("bffaa447-b500-49e0-9c73-bfd81db9242f"),
      receiptDate = "2020-09-23T16:12:11Z",
      acknowledgementReference = "Abc12345",
      originatingSystem = "MDTP",
      requestParameters = None
    )

  val requestDetail: RequestDetail = RequestDetail(IDType = "DAC", IDNumber = "1234567890")

  val displaySubscriptionForDACRequest: DisplaySubscriptionForDACRequest =
    DisplaySubscriptionForDACRequest(
      DisplaySubscriptionDetails(requestCommon = requestCommon, requestDetail = requestDetail)
    )

  "DisplaySubscriptionForDACRequest" - {

    "must deserialise DisplaySubscriptionForDACRequest" in {
      Json.parse(jsonPayloadForDisplaySubscriptionRequest).validate[DisplaySubscriptionForDACRequest].get mustBe displaySubscriptionForDACRequest
    }

    "must serialise DisplaySubscriptionForDACRequest" in {
      Json.toJson(displaySubscriptionForDACRequest) mustBe jsonForDisplaySubscriptionRequest
    }

    "must comply with request common in spec" in {
      val requestCommon = RequestCommon.createRequestCommon
      val ackRefLength = requestCommon.acknowledgementReference.length
      ackRefLength >= 1 && ackRefLength <= 32 mustBe true

      requestCommon.regime mustBe "DAC"

      val date: Regex = raw"[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z".r
      date.findAllIn(requestCommon.receiptDate).toList.nonEmpty mustBe true

      requestCommon.originatingSystem mustBe "MDTP"
    }
  }
}
