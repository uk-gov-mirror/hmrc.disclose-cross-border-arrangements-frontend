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

import com.google.inject.ImplementedBy
import javax.inject.Inject
import models.upscan._
import play.api.Logger
import reactivemongo.bson.BSONObjectID
import repositories.UploadSessionRepository

import scala.concurrent.Future

@ImplementedBy(classOf[MongoBackedUploadProgressTracker])
trait UploadProgressTracker {

  def requestUpload(uploadId : UploadId, fileReference : Reference) : Future[Boolean]

  def registerUploadResult(reference : Reference, uploadStatus : UploadStatus): Future[Boolean]

  def getUploadResult(id : UploadId): Future[Option[UploadStatus]]

}

import scala.concurrent.{ExecutionContext, Future}

class MongoBackedUploadProgressTracker @Inject()(repository : UploadSessionRepository)(implicit ec : ExecutionContext) extends UploadProgressTracker {

  override def requestUpload(uploadId : UploadId, fileReference : Reference) : Future[Boolean] = {

    repository.insert(UploadSessionDetails(BSONObjectID.generate(), uploadId, fileReference, InProgress))
  }

  override def registerUploadResult(fileReference: Reference, uploadStatus: UploadStatus): Future[Boolean] = {
    Logger.debug("In the register " + fileReference.toString + "   " + uploadStatus.toString)
    repository.updateStatus(fileReference, uploadStatus)
  }


  override def getUploadResult(id: UploadId): Future[Option[UploadStatus]] = {
    Logger.debug("Getting the upload result from the database")
    for (result <- repository.findByUploadId(id)) yield {
      result map {x =>
          Logger.debug("The status is " + x.status)
           x.status
      }
    }
  }

}
