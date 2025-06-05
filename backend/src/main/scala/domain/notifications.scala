package backend.domain

import java.util.UUID
import java.time.Instant
import io.circe.{Encoder, Decoder, Json}
import io.circe.syntax.EncoderOps

import java.time.Instant
import java.time.format.DateTimeParseException
import io.circe.Decoder

object notifications {

  case class Notification(
    notificationId: UUID,
    userId: UUID,
    isRead: Boolean,
    createdAt: Instant,
    message: String
  )

  case class NotificationRequest(
    userId: UUID,
    message: String
  )

  object Notification {

    given decoder: Decoder[Notification] = Decoder.instance { c =>
      for {
        notificationId <- c.downField("notification_id").as[UUID]
        userId <- c.downField("user_id").as[UUID]
        isRead <- c.downField("is_read").as[Boolean]
        createdAt <- c.downField("created_at").as[Instant]
        message <- c.downField("message").as[String]
      } yield Notification.create(
        notificationId = notificationId,
        userId = userId,
        isRead = isRead,
        createdAt = createdAt,
        message = message
      )
    }

    given encoder: Encoder[Notification] = Encoder.instance { s =>
      Json.obj(
        "notification_id" -> s.notificationId.asJson,
        "user_id" -> s.userId.asJson,
        "is_read" -> s.isRead.asJson,
        "created_at" -> s.createdAt.asJson,
        "message" -> s.message.asJson,
      )
    }



    def create(
      notificationId: UUID,
      userId: UUID,
      isRead: Boolean,
      createdAt: Instant,
      message: String
    ) = Notification(
      notificationId = notificationId,
      userId = userId,
      isRead = isRead,
      createdAt = createdAt,
      message = message
    )
  }

  object NotificationRequest {

    given encoder: Encoder[NotificationRequest] = Encoder.instance { n =>
      Json.obj(
        "user_id" -> n.userId.asJson,
        "message" -> n.message.asJson
      )
    }

    given decoder: Decoder[NotificationRequest] = Decoder.forProduct2(
      "user_id", "message"
    )(NotificationRequest.apply)

    def create(userId: UUID, message: String) = 
      NotificationRequest(
        userId = userId,
        message = message
      )
  }
}