package backend.domain

import io.circe.{Decoder, Encoder}
import io.circe.syntax.*
import java.util.UUID
import java.time.{Instant, OffsetDateTime}

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
      bookingId: UUID,
      patientId: UUID,
      clinicId: UUID,
      slotTime: Instant,
      slotLength: Long,
      clinicInfo: Option[String],
      isConfirmed: Boolean,
      appointmentType: AppointmentType
  )
  
  object BookingResponse {

    given Decoder[Instant] = Decoder.decodeString.emap { str =>
      try {
        val odt = OffsetDateTime.parse(str)
        Right(odt.toInstant)
      } catch {
        case e: Exception => Left(s"Could not parse Instant: $str - ${e.getMessage}")
      }
    }

    given Decoder[BookingResponse] = Decoder.forProduct8(
      "booking_id",
      "patient_id",
      "clinic_id",
      "slot_time",
      "slot_length",
      "clinic_info",
      "confirmed",
      "appointment_type"
    )(BookingResponse.apply)

    given Encoder[BookingResponse] = Encoder.instance { br =>
      io.circe.Json.obj(
        "booking_id"   -> br.bookingId.asJson,
        "patient_id"   -> br.patientId.asJson,
        "clinic_id"    -> br.clinicId.asJson,
        "slot_time"    -> br.slotTime.asJson,
        "slot_length"  -> br.slotLength.asJson,
        "clinic_info"  -> br.clinicInfo.asJson,
        "confirmed"    -> br.isConfirmed.asJson,
        "appointment_type" -> br.appointmentType.asJson
      )
    }

  }

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

    given decoder: Decoder[BookingRequestPayload] = Decoder.forProduct3(
      "slot_id", "patient_id", "clinic_id"
    )(BookingRequestPayload.apply)
  }

  final case class ConfirmBookingPayload(appointmentType: AppointmentType, clinicInfo: Option[String] = None)

  object ConfirmBookingPayload {
    given encoder: Encoder[ConfirmBookingPayload] = Encoder.instance { cb =>
      io.circe.Json.obj(
        "appointment_type" -> cb.appointmentType.asJson,
        "clinic_info"      -> cb.clinicInfo.asJson,
        "confirmed"        -> io.circe.Json.True
      )
    }

    given decoder: Decoder[ConfirmBookingPayload] = Decoder.instance { c =>
      for {
        appointmentType <- c.downField("appointment_type").as[AppointmentType]
        clinicInfo      <- c.downField("clinic_info").as[Option[String]]
      } yield ConfirmBookingPayload(appointmentType, clinicInfo)
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