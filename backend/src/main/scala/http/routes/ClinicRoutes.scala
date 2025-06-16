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
import backend.database.DbClinics


class ClinicRoutes private extends Http4sDsl[IO] {

  private val list: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root =>
      DbClinics.getAllClinics.flatMap {
        case Right(clinics) => Ok(clinics.asJson)
        case Left(err)      => InternalServerError(err.toString)
      }
  }

  private val getClinicByIdRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "api" / "clinics" / UUIDVar(clinicId) =>
      DbClinics.getClinicById(clinicId).flatMap {
        case Left(err) =>
          InternalServerError(Json.obj(
            "error" -> Json.fromString(err.toString)
          ))
        case Right(clinic) =>
          Ok(clinic.asJson)
      }
  }

  val routes: HttpRoutes[IO] = Router("/clinics" -> list)
}

object ClinicRoutes { def apply() = new ClinicRoutes }
