package backend.database

import backend.domain.clinics.*
import cats.effect.IO
import org.http4s.*

import java.util.UUID

object DbClinics {


  /** GET all clinics with their lat/lng */
  def getAllClinics: IO[Either[DbError, List[Clinic]]] = {
    val uri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/clinics?select=clinic_id,name,address,latitude,longitude"
    )
    val req = Request[IO](Method.GET, uri, headers = commonHeaders)
    fetchAndDecode[List[Clinic]](req, "clinics")
  }

  /** Fetch a single clinic by its UUID primary key */
  def getClinicById(clinicId: UUID): IO[Either[DbError, Clinic]] = {
    val uri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/clinics?select=*&clinic_id=eq.$clinicId"
    )
    val req = Request[IO](Method.GET, uri, headers = commonHeaders)

    // Decided to keep same style as slot version
    fetchAndDecode[List[Clinic]](req, "clinics").flatMap {
      case Right(clinic :: Nil) => IO.pure(Right(clinic))
      case Right(Nil) => IO.pure(Left(DbError.NotFound("clinic", clinicId.toString)))
      case Right(_ :: _ :: _) => IO.pure(Left(DbError.Unknown(s"Multiple clinics with id $clinicId")))
      case Left(err) => IO.pure(Left(err))
    }
  }


}
