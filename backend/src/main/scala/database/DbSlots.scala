package backend.database

import cats.effect.IO
import org.http4s._
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.ci.CIStringSyntax
import io.circe.Json
import org.http4s.dsl.io._
import backend.domain.slots._
import backend.domain.slots.given

def addSlot(slotReq: SlotRequest):IO[Either[String, Unit]]= {

  val payload = slotReq.asJson

  println(s"Payload: $payload")

  val slotUri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/slots")
  println(s"Supabase URL: $supabaseUrl")
  println(s"Supabase Key: $supabaseKey")

  val slotRequest = Request[IO](
    method = Method.POST,
    uri = slotUri,
    headers = Headers(
      Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
      Header.Raw(ci"apikey", supabaseKey),
      Header.Raw(ci"Content-Type", "application/json")
    )
  ).withEntity(payload)

  EmberClientBuilder.default[IO].build.use { httpClient =>
    httpClient.fetch(slotRequest) { response =>
      response.status match {
        case Status.Ok | Status.Created =>
          IO.println(s"Slot added successfully") *> IO.pure(Right(()))
        case _ =>
          IO.pure(Left(s"Failed to add slot: ${response.status}"))
      }
    }
  }
}
