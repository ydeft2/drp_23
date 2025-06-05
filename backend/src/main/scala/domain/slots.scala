package backend.domain

import java.util.UUID
import java.time.Instant
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax.EncoderOps

object slots {

  final case class Slot(
    slotId: UUID,
    bookingId: UUID,
    clinicId: UUID,
    isTaken: Boolean,
    slotTime: Instant,
    slotLength: Long,
    clinicInfo: String,
    createdAt: Instant
  )

  object Slot {

    given decoder: Decoder[Slot] = Decoder.forProduct8(
      "slot_id", "booking_id", "clinic_id", "is_taken",
      "slot_time", "slot_length", "clinic_info", "created_at"
    )(Slot.apply)

    // Encoder for Slot -> JSON (in case we need it)
    // todo: can refactor to use .instance instead
    given encoder: Encoder[Slot] = Encoder.forProduct8(
      "slot_id", "booking_id", "clinic_id", "is_taken",
      "slot_time", "slot_length", "clinic_info", "created_at"
    )(s => (
      s.slotId, s.bookingId, s.clinicId, s.isTaken,
      s.slotTime, s.slotLength, s.clinicInfo, s.createdAt
    ))

  }

  final case class SlotRequest(
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

    given decoder: Decoder[SlotRequest] = Decoder.forProduct3(
      "clinic_id", "slot_time", "slot_length"
    )(SlotRequest.apply)


    def create(clinicId: UUID, slotTime: Instant, slotLength: Long) = 
      SlotRequest(
        clinicId = clinicId,
        slotTime = slotTime,
        slotLength = slotLength
      )
  }

  final case class PatientSlotResponse(
      slotTime: Instant,
      slotLength: Long,
      clinicInfo: Option[String],
      isTaken: Boolean
  )

  object PatientSlotResponse {

    given decoder: Decoder[PatientSlotResponse] = Decoder.forProduct4(
      "slot_time", "slot_length", "clinic_info", "is_taken"
    )(PatientSlotResponse.apply)

  }

  /** Which fields can a user filter on? */
  final case class SlotFilter(
      isTaken: Option[Boolean],
      clinicInfo: Option[String],
      slotTimeGte: Option[Instant],
      slotTimeLte: Option[Instant]
  )

  final case class Pagination(limit: Int, offset: Int)

  object Pagination {
    val defaultPageSize = 10

    def apply(maybeLimit: Option[Int], maybeOffset: Option[Int]): Pagination =
      Pagination(
        limit = maybeLimit.getOrElse(defaultPageSize),
        offset = maybeOffset.getOrElse(0)
      )

  }

}