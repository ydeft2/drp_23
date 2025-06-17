package backend.database

import cats.effect.IO
import java.util.UUID
import backend.domain.messages.{Message, MessageRequest}
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import io.circe.syntax._
import org.typelevel.ci.CIString
import org.typelevel.ci.CIStringSyntax
import org.http4s.ember.client.EmberClientBuilder

object DbMessages {

  def sendMessage(messageRequest: MessageRequest): IO[Either[DbError, Message]] = {
    val msgJson = messageRequest.asJson
    
    val uri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/messages"
    )
    val req = Request[IO](
      method  = Method.POST,
      uri     = uri,
      headers = commonHeaders ++ Headers.of(
        Header.Raw(CIString("Prefer"), "return=representation")
      )
    ).withEntity(messageRequest.asJson)

    
    fetchAndDecode[List[Message]](req, "messages").map {
      case Right(msg :: Nil)  => Right(msg)
      case Right(Nil)         => Left(DbError.NotFound("message", messageRequest.message))
      case Left(err)          => Left(err)
    }
  }

  def fetchMessages(
      userId: UUID
  ): IO[Either[DbError, List[Message]]] = {
    val selectQuery  = "?select=message_id,sender_id,receiver_id,message,sent_at,sender_name,receiver_name,is_read"
    val filterClause = s"&or=(sender_id.eq.$userId,receiver_id.eq.$userId)"
    val uri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/messages$selectQuery$filterClause"
    )
    val req = Request[IO](method = Method.GET, uri = uri, headers = commonHeaders)
    fetchAndDecode[List[Message]](req, "messages")
  }

  def markMessageRead(messageId: UUID): IO[Either[String, Unit]] = {
  val updateUri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/messages?message_id=eq.$messageId")
  val payload = io.circe.Json.obj("is_read" := true)
  val request = Request[IO](
    method = Method.PATCH,
    uri = updateUri,
    headers = Headers(
      Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
      Header.Raw(ci"apikey", s"$supabaseKey"),
      Header.Raw(ci"Content-Type", "application/json"),
      Header.Raw(ci"Prefer", "return=minimal")
    )
    ).withEntity(payload)
    EmberClientBuilder.default[IO].build.use { httpClient =>
      httpClient.fetch(request) { response =>
        response.status match {
          case Status.Ok | Status.NoContent => IO.pure(Right(()))
          case _ => response.as[String].flatMap(body => IO.pure(Left(s"Error: ${response.status.code} - $body")))
        }
      }
    }
  }

  def countUnread(userId: UUID): IO[Either[String, Int]] =
    fetchMessages(userId).map {
      case Left(err) =>
        Left(s"Failed to get unread count: $err")

      case Right(msgs) =>
        // Collect all senders of unread messages to this user
        val unreadSenders = msgs.collect {
          case m if m.receiverId == userId && !m.isRead => m.senderId
        }

        println(s"Unread messages from senders: ${unreadSenders.mkString(", ")}")

        println("countUnread: " + unreadSenders.distinct.size)
        // Count distinct chats
        Right(unreadSenders.distinct.size)
    }

}