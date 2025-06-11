package backend.database

import cats.effect.IO
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.ci.CIStringSyntax
import backend.domain.interests.InterestRequest

object DbInterests {

  /** POST a new interest; returns Right(()) or Left(DbError) */
  def addInterest(reqBody: InterestRequest): IO[Either[DbError, Unit]] = {
    val uri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/interests")
    val req = Request[IO](Method.POST, uri, headers = commonHeaders)
      .withEntity(reqBody.asJson)

    // TODO can we use executeNoContent instead
    EmberClientBuilder
      .default[IO]
      .build
      .use { client =>
        client.fetch(req) { resp =>
            resp.as[String].flatMap { body =>
                IO.pure(
                  resp.status match {
                    case Status.Created | Status.Ok => Right(())
                    case Status.NotFound            => Left(DbError.NotFound("interest",""))
                    case other                      => Left(DbError.SqlError(other.code, body))
                  }
                )
            }
        }
      }
  }

}
