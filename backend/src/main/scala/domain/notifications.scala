package backend.domain

import java.util.UUID
import java.time.Instant
import io.circe.{Encoder, Decoder, Json}
import io.circe.syntax.EncoderOps

object notifications {

  case class Notification(
    notificationId: UUID,
    userId: UUID,
    isRead: Boolean,
    createdAt: Instant,
    message: String,
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
    
    def create(userId: UUID, message: String) = 
      NotificationRequest(
        userId = userId,
        message = message
      )
  }
}