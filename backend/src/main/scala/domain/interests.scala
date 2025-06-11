package backend.domain

import io.circe.{Decoder, Encoder}
import java.util.UUID

object interests {
  
  final case class InterestRequest(
    patient_id: UUID,
    clinic_id: UUID
  )

  object InterestRequest {
    given Encoder[InterestRequest] = Encoder.forProduct2(
      "patient_id", "clinic_id"
    )(ir => (ir.patient_id, ir.clinic_id))

    given Decoder[InterestRequest] = Decoder.forProduct2(
      "patient_id", "clinic_id"
    )(InterestRequest.apply)
  }
  
  final case class InterestRecord(clinic_id: UUID)
  
  object InterestRecord {
    given Decoder[InterestRecord] =
      Decoder.forProduct1("clinic_id")(InterestRecord.apply)
  }
  
}
