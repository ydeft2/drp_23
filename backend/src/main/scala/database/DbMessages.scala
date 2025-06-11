package backend.database

import cats.effect.IO
import java.util.UUID
import backend.domain.messages.{Message, MessageRequest}
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder

object DbMessages {

  def sendMessage(messageRequest: MessageRequest): IO[Either[DbError, Unit]] = {
    val msgJson = messageRequest.asJson
    
    val uri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/messages"
    )

    val req = Request[IO](
      method = Method.POST,
      uri = uri,
      headers = commonHeaders
    ).withEntity(msgJson)
    IO.println(s"Message JSON: $msgJson") *>
    IO.println(s"Just about to execute") *>
    executeNoContent(req, "message", messageRequest.toString)
  }

  def fetchMessages(userId: UUID): IO[Either[DbError, List[Message]]] = {
    val selectQuery = "?select=message_id,sender_id,receiver_id,message,sent_at,sender_name,receiver_name"
    val filterClause = s"&or=(sender_id.eq.$userId,receiver_id.eq.$userId)"

    val fullUri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/messages$selectQuery$filterClause"
    )

    val req = Request[IO](
      method = Method.GET,
      uri = fullUri,
      headers = commonHeaders
    )
    fetchAndDecode[List[Message]](req, "messages")
  }

}