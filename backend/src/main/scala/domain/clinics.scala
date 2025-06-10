package backend.domain

import io.circe.{Decoder, Encoder}

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

    given Encoder[Clinic] = Encoder.forProduct5(
      "clinic_id", "name", "address", "latitude", "longitude"
    )(c => (c.clinicId, c.name, c.address, c.latitude, c.longitude))

    given Decoder[Clinic] = Decoder.forProduct5(
      "clinic_id", "name", "address", "latitude", "longitude"
    )(Clinic.apply)
  }

}
