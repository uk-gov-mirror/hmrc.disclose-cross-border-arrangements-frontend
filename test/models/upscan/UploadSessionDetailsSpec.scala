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

package models.upscan

import base.SpecBase
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID

class UploadSessionDetailsSpec extends SpecBase {

  "Upload Session Details" - {
    "must be able to be marshalled correctly for status: NotStarted" in {
      val json =
        """{
          |"_id": { "$oid": "111111111111111111111111"},
          |"uploadId": { "value": "121" },
          |"reference": { "value": "ref" },
          |"status": {
          |     "_type": "NotStarted"
          |   }
          |}""".stripMargin

      val expectedUploadSessionDetails = UploadSessionDetails(
        BSONObjectID.parse("111111111111111111111111").get,
        UploadId("121"),
        Reference("ref"),
        NotStarted
      )

      Json.parse(json).as[UploadSessionDetails] mustBe expectedUploadSessionDetails
    }

    "must be able to be written correctly for status: NotStarted" in {
      val uploadSessionDetails = UploadSessionDetails(
        BSONObjectID.parse("111111111111111111111111").get,
        UploadId("121"),
        Reference("ref"),
        NotStarted
      )

      val expectedUploadSessionDetails =
        """{
          |"_id": { "$oid": "111111111111111111111111"},
          |"uploadId": { "value": "121" },
          |"reference": { "value": "ref" },
          |"status": {
          |     "_type": "NotStarted"
          |   }
          |}""".stripMargin

      Json.toJson(uploadSessionDetails) mustBe Json.parse(expectedUploadSessionDetails)
    }

    "must be able to be marshalled correctly for status: InProgress" in {
      val json =
        """{
          |"_id": { "$oid": "111111111111111111111111"},
          |"uploadId": { "value": "121" },
          |"reference": { "value": "ref" },
          |"status": {
          |     "_type": "InProgress"
          |   }
          |}""".stripMargin

      val expectedUploadSessionDetails = UploadSessionDetails(
        BSONObjectID.parse("111111111111111111111111").get,
        UploadId("121"),
        Reference("ref"),
        InProgress
      )

      Json.parse(json).as[UploadSessionDetails] mustBe expectedUploadSessionDetails
    }

    "must be written correctly for status: InProgress" in {
      val expectedUploadSessionDetails =
        """{
          |"_id": { "$oid": "111111111111111111111111"},
          |"uploadId": { "value": "121" },
          |"reference": { "value": "ref" },
          |"status": {
          |     "_type": "InProgress"
          |   }
          |}""".stripMargin

      val uploadSessionDetails = UploadSessionDetails(
        BSONObjectID.parse("111111111111111111111111").get,
        UploadId("121"),
        Reference("ref"),
        InProgress
      )

      Json.toJson(uploadSessionDetails) mustBe Json.parse(expectedUploadSessionDetails)
    }

    "must be able to be marshalled correctly for status: Failed" in {
      val json =
        """{
          |"_id": { "$oid": "111111111111111111111111"},
          |"uploadId": { "value": "121" },
          |"reference": { "value": "ref" },
          |"status": {
          |     "_type": "Failed"
          |   }
          |}""".stripMargin

      val expectedUploadSessionDetails = UploadSessionDetails(
        BSONObjectID.parse("111111111111111111111111").get,
        UploadId("121"),
        Reference("ref"),
        Failed
      )

      Json.parse(json).as[UploadSessionDetails] mustBe expectedUploadSessionDetails
    }

    "must be written correctly for status: Failed" in {
      val expectedUploadSessionDetails =
        """{
          |"_id": { "$oid": "111111111111111111111111"},
          |"uploadId": { "value": "121" },
          |"reference": { "value": "ref" },
          |"status": {
          |     "_type": "Failed"
          |   }
          |}""".stripMargin

      val uploadSessionDetails = UploadSessionDetails(
        BSONObjectID.parse("111111111111111111111111").get,
        UploadId("121"),
        Reference("ref"),
        Failed
      )

      Json.toJson(uploadSessionDetails) mustBe Json.parse(expectedUploadSessionDetails)
    }

    "must written correctly for status: UploadedSuccessfully" in {
      val expectedUploadSessionDetails =
        """{
          |"_id": { "$oid": "111111111111111111111111"},
          |"uploadId": { "value": "121" },
          |"reference": { "value": "ref" },
          |"status": {
          |     "_type": "UploadedSuccessfully",
          |     "name": "name",
          |     "downloadUrl": "downloadUrl"
          |   }
          |}""".stripMargin

      val uploadSessionDetails = UploadSessionDetails(
        BSONObjectID.parse("111111111111111111111111").get,
        UploadId("121"),
        Reference("ref"),
        UploadedSuccessfully("name", "downloadUrl")
      )

      Json.toJson(uploadSessionDetails) mustBe Json.parse(expectedUploadSessionDetails)
    }

    "must be able to be marshalled correctly for status: UploadedSuccessfully" in {
      val json =
        """{
          |"_id": { "$oid": "111111111111111111111111"},
          |"uploadId": { "value": "121" },
          |"reference": { "value": "ref" },
          |"status": {
          |     "_type": "UploadedSuccessfully",
          |     "name": "name",
          |     "downloadUrl": "downloadUrl"
          |   }
          |}""".stripMargin

      val expectedUploadSessionDetails = UploadSessionDetails(
        BSONObjectID.parse("111111111111111111111111").get,
        UploadId("121"),
        Reference("ref"),
        UploadedSuccessfully("name", "downloadUrl")
      )

      Json.parse(json).as[UploadSessionDetails] mustBe expectedUploadSessionDetails
    }

    "must fail with error when status not recognised" in {
      val json =
        """{
          |"_id": { "$oid": "111111111111111111111111"},
          |"uploadId": { "value": "121" },
          |"reference": { "value": "ref" },
          |"status": {
          |     "_type": "nope"
          |   }
          |}""".stripMargin

      Json.parse(json).validate[UploadSessionDetails].toString must include("""Unexpected value of _type: "nope"""")
    }

    "must fail with error when _type not present" in {
      val json =
        """{
          |"_id": { "$oid": "111111111111111111111111"},
          |"uploadId": { "value": "121" },
          |"reference": { "value": "ref" },
          |"status": {
          |   }
          |}""".stripMargin

      Json.parse(json).validate[UploadSessionDetails].toString must include("Missing _type field")
    }
  }

}
