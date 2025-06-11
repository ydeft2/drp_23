package backend.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceEntityEncoder.*
import io.circe.syntax.*
import io.circe.Json
import cats.effect.IO
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.*
import backend.database.{DbError, DbInterests}
import backend.domain.interests.InterestRequest

class InterestRoutes private extends Http4sDsl[IO] {

  private val create: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root =>
      req.as[InterestRequest].flatMap { ir =>
        DbInterests.addInterest(ir).flatMap {
          case Right(_) =>
            Created(Json.obj("message" -> Json.fromString("Interest recorded")))

          case Left(DbError.SqlError(409, _)) =>
            Conflict(Json.obj(
              "error" -> Json.fromString("You’ve already registered interest in this clinic")
            ))

          case Left(DbError.SqlError(code, body)) if code >= 400 && code < 500 =>
            BadRequest(Json.obj(
              "error"   -> Json.fromString("Invalid interest request"),
              "details" -> Json.fromString(body)
            ))

          case Left(DbError.DecodeError(msg)) =>
            BadRequest(Json.obj("error" -> Json.fromString(s"Decode error: $msg")))

          case Left(DbError.NotFound(_, _)) =>
            NotFound(Json.obj("error" -> Json.fromString("Referenced resource not found")))

          case Left(DbError.Unknown(msg)) =>
            InternalServerError(Json.obj("error" -> Json.fromString(msg)))

          case Left(DbError.SqlError(code, body)) =>
            InternalServerError(Json.obj(
              "error"   -> Json.fromString(s"Database error $code"),
              "details" -> Json.fromString(body)
            ))
        }
      }
  }

  val routes: HttpRoutes[IO] = Router("/interests" -> create)
}
object InterestRoutes { def apply() = new InterestRoutes }
