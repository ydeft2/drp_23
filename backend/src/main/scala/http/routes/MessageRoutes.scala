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

import java.time.Instant
import java.util.UUID
import backend.database.{DbError, DbMessages}




class MessageRoutes private extends Http4sDsl[IO] {

  
  private val sendMessageRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "send" =>
      for {
        messageReq <- req.as[MessageRequest]
        res <- DbMessages.sendMessage(messageReq)
        response <- res match {
          case Right(_) =>
            Created()
          case Left(error) =>
            IO.println(s"Error sending message: $error") *>
            BadRequest("Error sending message")
        }
      } yield response
  }

  private val getMessagesRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "fetch"/ UUIDVar(userId) =>
      for {
        messagesRes <- DbMessages.fetchMessages(userId)
        response <- messagesRes match {
          case Right(messages) =>
            Ok(messages.asJson)
          case Left(error) =>
            BadRequest("Error fetching messages")
        }
      } yield response
  }

  val routes = Router(
    "/messages" -> (sendMessageRoute <+> getMessagesRoute)
  )
}

object MessageRoutes {
  def apply(): MessageRoutes = new MessageRoutes()
}
