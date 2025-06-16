package backend.domain

import java.util.UUID
import java.time.Instant
import io.circe.{Encoder, Decoder, Json}
import io.circe.syntax.EncoderOps

import java.time.{Instant, OffsetDateTime}
import java.time.format.DateTimeParseException
import io.circe.Decoder

object notifications {

  case class Notification(
    notificationId: UUID,
    userId: UUID,
    isRead: Boolean,
    createdAt: Instant,
    message: String,
    metadata: Option[Json]
  )

  case class NotificationRequest(
    userId: UUID,
    message: String,
    metadata: Option[Json]
  )

  object Notification {

    given Decoder[Instant] = Decoder.decodeString.emap { str =>
      try {
        val odt = OffsetDateTime.parse(str)
        Right(odt.toInstant)
      } catch {
        case e: Exception => Left(s"Could not parse Instant: $str - ${e.getMessage}")
      }
    }

    given decoder: Decoder[Notification] = Decoder.instance { c =>
      for {
        notificationId <- c.downField("notification_id").as[UUID]
        userId <- c.downField("user_id").as[UUID]
        isRead <- c.downField("is_read").as[Boolean]
        createdAt <- c.downField("created_at").as[Instant]
        message <- c.downField("message").as[String]
        metadata <- c.downField("metadata").as[Option[Json]]
      } yield Notification.create(
        notificationId = notificationId,
        userId = userId,
        isRead = isRead,
        createdAt = createdAt,
        message = message,
        metadata = metadata
      )
    }


    given encoder: Encoder[Notification] = Encoder.instance { s =>
      val base = Json.obj(
        "notification_id" -> s.notificationId.asJson,
        "user_id" -> s.userId.asJson,
        "is_read" -> s.isRead.asJson,
        "created_at" -> s.createdAt.asJson,
        "message" -> s.message.asJson
      )
      s.metadata.fold(base)(meta => base.deepMerge(Json.obj("metadata" -> meta)))
    }



    def create(
      notificationId: UUID,
      userId: UUID,
      isRead: Boolean,
      createdAt: Instant,
      message: String,
      metadata: Option[Json]
    ) = Notification(
      notificationId = notificationId,
      userId = userId,
      isRead = isRead,
      createdAt = createdAt,
      message = message,
      metadata = metadata
    )
  }

  object NotificationRequest {

    given encoder: Encoder[NotificationRequest] = Encoder.instance { n =>
      val base = Json.obj(
        "user_id" -> n.userId.asJson,
        "message" -> n.message.asJson
      )
      n.metadata.fold(base)(meta => base.deepMerge(Json.obj("metadata" -> meta)))
    }

    given decoder: Decoder[NotificationRequest] = Decoder.forProduct3(
      "user_id", "message", "metadata"
    )(NotificationRequest.apply)

    def create(userId: UUID, message: String, metadata: Option[Json]) = 
      NotificationRequest(
        userId = userId,
        message = message,
        metadata = metadata
      )
  }
}