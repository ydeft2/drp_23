package backend.domain

import java.util.UUID
import java.time.{Instant, OffsetDateTime}
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax.EncoderOps

object messages {

  case class Message(
    messageId: UUID,
    senderId:  UUID,
    receiverId: UUID,
    message: String,
    sentAt: Instant,
    senderName: String,   
    receiverName: String,
    isRead: Boolean
  )

  object Message {

    given Decoder[Instant] = Decoder.decodeString.emap { str =>
      try {
        val odt = OffsetDateTime.parse(str)
        Right(odt.toInstant)
      } catch {
        case e: Exception => Left(s"Could not parse Instant: $str - ${e.getMessage}")
      }
    }

    given decoder: Decoder[Message] = Decoder.forProduct8(
      "message_id", "sender_id", "receiver_id", "message", "sent_at", "sender_name", "receiver_name", "is_read"
    )(Message.apply)

    given encoder: Encoder[Message] = Encoder.forProduct8(
      "message_id", "sender_id", "receiver_id", "message", "sent_at", "sender_name", "receiver_name",  "is_read"
    )(m => (m.messageId, m.senderId, m.receiverId, m.message, m.sentAt, m.senderName, m.receiverName, m.isRead))
  }

  case class MessageRequest(
    senderId: UUID,
    receiverId: UUID,
    message: String,
    senderName   : String,   
    receiverName : String 
  )

  object MessageRequest {
    given encoder: Encoder[MessageRequest] = Encoder.instance { m =>
      Json.obj(
        "sender_id"   -> m.senderId.asJson,
        "receiver_id" -> m.receiverId.asJson,
        "message"     -> m.message.asJson,
        "sender_name"   -> m.senderName.asJson,
        "receiver_name" -> m.receiverName.asJson
      )
    }

    given decoder: Decoder[MessageRequest] = Decoder.forProduct5(
      "sender_id", "receiver_id", "message", "sender_name", "receiver_name"
    )(MessageRequest.apply)
  }

}