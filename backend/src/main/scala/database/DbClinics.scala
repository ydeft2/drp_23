package backend.database

import backend.domain.clinics.*
import cats.effect.IO
import org.http4s.*

object DbClinics {


  /** GET all clinics with their lat/lng */
  def getAllClinics: IO[Either[DbError, List[Clinic]]] = {
    val uri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/clinics?select=clinic_id,name,address,latitude,longitude"
    )
    val req = Request[IO](Method.GET, uri, headers = commonHeaders)
    fetchAndDecode[List[Clinic]](req, "clinics")
  }


}
