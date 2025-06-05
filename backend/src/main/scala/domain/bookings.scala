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



}