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
    message: String
  )

  case class NotificationRequest(
    userId: UUID,
    message: String
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
        notificationId <- c.downField("notification_id").as[UUID].map { id =>
          println(s"Decoded notification_id: $id")
          id
        }
        userId <- c.downField("user_id").as[UUID].map { uid =>
          println(s"Decoded user_id: $uid")
          uid
        }
        isRead <- c.downField("is_read").as[Boolean].map { read =>
          println(s"Decoded is_read: $read")
          read
        }
        createdAt <- c.downField("created_at").as[Instant].map { ts =>
          println(s"Decoded created_at: $ts")
          ts
        }
        message <- c.downField("message").as[String].map { msg =>
          println(s"Decoded message: $msg")
          msg
        }
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
        "message" -> s.message.asJson
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