package backend.domain

import io.circe.{Decoder, Encoder}
import io.circe.syntax.*
import java.util.UUID
import java.time.Instant

object bookings {

  final case class Booking(
      bookingId: UUID,
      slotId: UUID,
      appointmentType: AppointmentType,
      confirmed: Boolean,
      patientId: UUID,
      clinicId: UUID
  )

  final case class BookingResponse(
      patientId: UUID,
      clinicId: UUID,
      slotTime: Instant,
      slotLength: Long,
      clinicInfo: Option[String],
      isConfirmed: Boolean
  )

  final case class SlotInfo(
    slot_time: Instant,
    slot_length: Long,
    clinic_info: Option[String]
  )

  final case class BookingDto(
    patient_id: UUID,
    clinic_id: UUID,
    confirmed: Boolean,
    slot: SlotInfo
  )

  given Decoder[SlotInfo] = Decoder.forProduct3(
    "slot_time", "slot_length", "clinic_info"
  )(SlotInfo.apply)

  given Decoder[BookingDto] = Decoder.forProduct4(
    "patient_id", "clinic_id", "confirmed", "slot"
  )(BookingDto.apply)



  enum AppointmentType {
    case CHECKUP, EXTRACTION, FILLING, ROOT_CANAL, HYGIENE, OTHER, NOT_SET
  }

  val toBookingResponse: BookingDto => BookingResponse = dto =>
    BookingResponse(
      patientId = dto.patient_id,
      clinicId = dto.clinic_id,
      slotTime = dto.slot.slot_time,
      slotLength = dto.slot.slot_length,
      clinicInfo = dto.slot.clinic_info,
      isConfirmed = dto.confirmed
    )


  object AppointmentType {
    // encode as a JSON string of the enum’s name
    given Encoder[AppointmentType] =
    Encoder.encodeString.contramap(_.toString)

    given Decoder[AppointmentType] =
      Decoder.decodeString.emap { str =>
        try Right(AppointmentType.valueOf(str))
        catch {
          case _: IllegalArgumentException => Left(s"Unknown appointmentType: $str")
        }
      }
  }

  final case class BookingRequestPayload(
      slotId: UUID,
      // TODO: placeholder for an important message sent to the clinic, since the clinic should decide what appointment type it is
      patientId: UUID,
      clinicId: UUID
  )

  object BookingRequestPayload {

    given encoder: Encoder[BookingRequestPayload] = Encoder.instance { b =>
      io.circe.Json.obj(
        "slot_id" -> b.slotId.asJson,
        "patient_id" -> b.patientId.asJson,
        "clinic_id" -> b.clinicId.asJson
      )
    }
  }

  final case class ConfirmBookingPayload(appointmentType: AppointmentType)

  object ConfirmBookingPayload {
    given encoder: Encoder[ConfirmBookingPayload] = Encoder.instance { cb =>
      io.circe.Json.obj(
        "appointment_type" -> cb.appointmentType.asJson,
        "confirmed"        -> io.circe.Json.True
      )
    }
  }

  final case class BookingFilter(
      isConfirmed: Option[Boolean],
      clinicInfo: Option[String],
      slotTimeGte: Option[Instant],
      slotTimeLte: Option[Instant],
      patientId: Option[UUID],
      clinicId: Option[UUID]
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