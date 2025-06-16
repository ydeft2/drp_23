package backend.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceEntityEncoder.*
import io.circe.syntax.*
import io.circe.Json
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.Logger
import backend.http.routes.*
import cats.*
import cats.data.*
import cats.syntax.*
import backend.domain.messages.*
import org.http4s.Header

import java.time.Instant
import java.util.UUID
import backend.database.{DbError, DbMessages}
import backend.domain.messages.{Message, MessageRequest}
import fs2.Stream
import fs2.concurrent.Topic
import org.typelevel.ci.CIString

import java.util.UUID

class MessageRoutes(topic: Topic[IO, Message]) extends Http4sDsl[IO] {
  private val sendMessageRoute = HttpRoutes.of[IO] {
    case req @ POST -> Root / "send" =>
      for {
        msgReq <- req.as[MessageRequest]
        res    <- DbMessages.sendMessage(msgReq)
        resp   <- res match {
                    case Left(err)    =>
                      IO.println(s"Error sending message: $err") *>
                      BadRequest("Error sending message")
                    case Right(dbMsg) =>
                      topic.publish1(dbMsg) *>
                      Created(dbMsg.asJson)
                  }
      } yield resp
  }

  private val getMessagesRoute = HttpRoutes.of[IO] {
    case GET -> Root / "fetch" / UUIDVar(userId) =>
      for {
        msgs <- DbMessages.fetchMessages(userId)
        resp <- msgs match {
                  case Left(err)       => BadRequest("Error fetching messages")
                  case Right(messages) => Ok(messages.asJson)
                }
      } yield resp
  }

  private val sseRoute = HttpRoutes.of[IO] {
    case GET -> Root / "stream" / UUIDVar(userId) =>
      val eventStream: Stream[IO, ServerSentEvent] =
        topic
          .subscribe(128)
          .filter(m => m.senderId == userId || m.receiverId == userId)
          .map(m => ServerSentEvent(data = Some(m.asJson.noSpaces)))
      Ok(eventStream)
        .map(_.putHeaders(Header.Raw(CIString("Content-Type"), "text/event-stream")))
  }

  val routes: HttpRoutes[IO] = Router(
    "/messages"       -> (sendMessageRoute <+> getMessagesRoute <+> sseRoute),
  )
}

object MessageRoutes {
  def apply(topic: Topic[IO, Message]): MessageRoutes =
    new MessageRoutes(topic)
}
