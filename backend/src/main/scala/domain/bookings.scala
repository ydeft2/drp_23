package backend.domain

import io.circe.{Decoder, Encoder}
import io.circe.syntax.*
import java.util.UUID

object bookings {

  final case class Booking(
      bookingId: UUID,
      slotId: UUID,
      appointmentType: AppointmentType,
      confirmed: Boolean,
      patientId: UUID,
      clinicId: UUID
  )

  enum AppointmentType {
    case CHECKUP, EXTRACTION, FILLING, ROOT_CANAL, HYGIENE, OTHER, NOT_SET
  }

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

}