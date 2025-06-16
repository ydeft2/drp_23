package backend.database

import cats.effect.IO
import java.util.UUID
import backend.domain.messages.{Message, MessageRequest}
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import io.circe.syntax._
import org.typelevel.ci.CIString

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
    val selectQuery  = "?select=message_id,sender_id,receiver_id,message,sent_at,sender_name,receiver_name"
    val filterClause = s"&or=(sender_id.eq.$userId,receiver_id.eq.$userId)"
    val uri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/messages$selectQuery$filterClause"
    )
    val req = Request[IO](method = Method.GET, uri = uri, headers = commonHeaders)
    fetchAndDecode[List[Message]](req, "messages")
  }
}