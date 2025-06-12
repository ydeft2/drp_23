package backend.domain

import io.circe.{Decoder, Encoder}

import java.util.UUID

object clinics {


  final case class Clinic(
   clinicId: UUID,
      // TODO: name is nullable on supabase currently.. not sure about this
      // TODO: similarly for address.
   name: String,
   address: Option[String],
   latitude: Option[Double],
   longitude: Option[Double]
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
