package backend.database

import cats.effect.IO
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.ci.CIStringSyntax
import backend.domain.interests.InterestRequest

object DbInterests {

  /** POST a new interest; returns Unit or error */
  def addInterest(reqBody: InterestRequest): IO[Either[String, Unit]] = {
    val uri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/interests")
    val req = Request[IO](Method.POST, uri, headers = commonHeaders)
      .withEntity(reqBody.asJson)

    EmberClientBuilder.default[IO].build.use { client =>
      client.fetch(req) { resp =>
        resp.status match {
          case Status.Created | Status.Ok =>
            IO.pure(Right(()))
          case s =>
            IO.pure(Left(s"Error adding interest: ${s.code}"))
        }
      }
    }
  }
  
  
}
