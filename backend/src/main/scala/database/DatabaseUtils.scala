package backend.database

import cats.effect.IO
import org.http4s.*
import org.http4s.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.ci.CIStringSyntax
import io.circe.{Decoder, Json}
import org.http4s.dsl.io.*

import java.util.UUID

case class AuthRequest(
    uid: String,
    accessToken: String
)

val supabaseUrl: String = sys.env("SUPABASE_URL")
val supabaseKey: String = sys.env("SUPABASE_API_KEY")

/** A simple ADT for database-level errors */
sealed trait DbError
object DbError {
  final case class NotFound(entity: String, id: String) extends DbError
  final case class DecodeError(message: String) extends DbError
  final case class SqlError(status: Int, body: String) extends DbError
  final case class Unknown(message: String) extends DbError
}

/** Reusable headers so we don't repeat them in each call */
private val commonHeaders = Headers(
  Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
  Header.Raw(ci"apikey", supabaseKey),
  Header.Raw(ci"Content-Type", "application/json")
)

/**
 * Common “fetch a request and decode it into A” logic
 * so we don’t repeat headers + error‐mapping everywhere.
 */
private def fetchAndDecode[A: Decoder](
                                        req: Request[IO],
                                        notFoundMsg: String
                                      ): IO[Either[DbError, A]] = {
  EmberClientBuilder
    .default[IO]
    .build
    .use { client =>
      client.fetch(req) { resp =>
        resp.status match {
          case Status.Ok =>
            resp.as[A].attempt.flatMap {
              case Right(a) => IO.pure(Right(a))
              case Left(decErr) => IO.pure(Left(DbError.DecodeError(decErr.getMessage)))
            }
          case Status.NotFound =>
            IO.pure(Left(DbError.NotFound(notFoundMsg, "")))
          case other =>
            resp.as[String].map(body => Left(DbError.SqlError(other.code, body)))
        }
      }
    }
}

/**
 * A helper for write‐operations that expect no JSON body in response, only status codes.
 *  - onSuccess → Right(())
 *  - on 404    → Left(NotFound(entityName, id))
 *  - on other → Left(SqlError)
 */
private def executeNoContent(
                              req: Request[IO],
                              entityName: String,
                              entityId: String
                            ): IO[Either[DbError, Unit]] =
  EmberClientBuilder
    .default[IO]
    .build
    .use { client =>
      client.fetch(req) { resp =>
        resp.status match {
          case Status.NoContent => IO.pure(Right(()))
          case Status.Created | Status.Ok =>
            // for POST which returns 201 or 200
            IO.pure(Right(()))
          case Status.NotFound => IO.pure(Left(DbError.NotFound(entityName, entityId)))
          case other =>
            resp.as[String].map(body => Left(DbError.SqlError(other.code, body)))
        }
      }
    }
