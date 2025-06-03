package backend.domain

import java.util.UUID
import java.time.Instant
import io.circe.{Encoder, Json}
import io.circe.syntax.EncoderOps

object slots {

  case class Slot(
    slotId: UUID,
    bookingId: UUID,
    clinicId: UUID,
    isTaken: Boolean,
    slotTime: Instant,
    slotLength: Long,
    clinicInfo: String,
    createdAt: Instant
  )

  case class SlotRequest(
    clinicId: UUID,
    slotTime: Instant,
    slotLength: Long
  )

  object SlotRequest {

    given encoder: Encoder[SlotRequest] = Encoder.instance { s =>
      Json.obj(
        "clinic_id"   -> s.clinicId.asJson,
        "slot_time"   -> s.slotTime.asJson,
        "slot_length" -> s.slotLength.asJson
      )
    }

    def create(clinicId: UUID, slotTime: Instant, slotLength: Long) = 
      SlotRequest(
        clinicId = clinicId,
        slotTime = slotTime,
        slotLength = slotLength
      )
  }
}