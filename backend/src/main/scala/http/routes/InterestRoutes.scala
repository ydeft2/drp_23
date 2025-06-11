package backend.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceEntityEncoder.*
import io.circe.syntax.*
import io.circe.Json
import cats.effect.IO
import cats.implicits.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.*
import cats.*
import cats.data.*
import backend.database.{DbError, DbInterests}
import backend.domain.interests.InterestRequest

import java.util.UUID

class InterestRoutes private extends Http4sDsl[IO] {
  
  // TODO: really should start placing this in util
  given uuidQueryParamDecoder: QueryParamDecoder[UUID] =
    QueryParamDecoder[String].map(UUID.fromString)
  
  private val createInterestRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
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

  object PatientIdQP extends QueryParamDecoderMatcher[UUID]("patient_id")

  private val listForPatientRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root :? PatientIdQP(pid) =>
      DbInterests.getInterestsForPatient(pid).flatMap {
        case Right(records) =>
          // Return just the list of UUIDs
          Ok(records.map(_.clinic_id).asJson)

        case Left(DbError.NotFound(_, _)) =>
          // No rows = empty list
          Ok(Json.arr())

        case Left(DbError.DecodeError(msg)) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Decode error: $msg")))

        case Left(DbError.SqlError(code, body)) if code >= 400 && code < 500 =>
          BadRequest(Json.obj("error" -> Json.fromString("Invalid request"), "details" -> Json.fromString(body)))

        case Left(DbError.Unknown(msg)) =>
          InternalServerError(Json.obj("error" -> Json.fromString(msg)))

        case Left(DbError.SqlError(code, body)) =>
          InternalServerError(Json.obj("error" -> Json.fromString(s"Database error $code"), "details" -> Json.fromString(body)))
      }
  }

  val deleteInterestRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ DELETE -> Root =>
      req.as[InterestRequest].flatMap { ir =>
        DbInterests.removeInterest(ir).flatMap {
          case Right(_) =>
            NoContent()

          case Left(DbError.NotFound(_, _)) =>
            NotFound(Json.obj("error" -> Json.fromString("Interest not found")))

          case Left(DbError.DecodeError(msg)) =>
            BadRequest(Json.obj("error" -> Json.fromString(s"Decode error: $msg")))

          case Left(DbError.SqlError(code, body)) if code >= 400 && code < 500 =>
            BadRequest(Json.obj("error" -> Json.fromString("Invalid request"), "details" -> Json.fromString(body)))

          case Left(DbError.Unknown(msg)) =>
            InternalServerError(Json.obj("error" -> Json.fromString(msg)))

          case Left(DbError.SqlError(code, body)) =>
            InternalServerError(Json.obj("error" -> Json.fromString(s"Database error $code"), "details" -> Json.fromString(body)))
        }
      }
  }


  val routes: HttpRoutes[IO] = Router(
    "/interests" -> (
      createInterestRoute <+>
      listForPatientRoute <+>
      deleteInterestRoute
    )
  )
}


object InterestRoutes { def apply() = new InterestRoutes }
