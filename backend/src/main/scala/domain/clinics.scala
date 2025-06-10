package backend.domain

import io.circe.Decoder
import java.util.UUID

object clinics {


  final case class Clinic(
   clinicId: UUID,
   name: String,
   address: String,
   latitude: Double,
   longitude: Double
  )

  object Clinic {
    given Decoder[Clinic] = Decoder.forProduct5(
      "clinic_id", "name", "address", "latitude", "longitude"
    )(Clinic.apply)
  }
  
}
