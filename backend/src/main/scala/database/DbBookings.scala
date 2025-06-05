package backend.database

import cats.effect.IO
import io.circe.Json
import io.circe.syntax._
import org.http4s._
import org.http4s.Method._
import org.http4s.circe._

import java.util.UUID
import backend.domain.bookings._



object DbBookings {


  /**
   * 1) Insert a new booking request:
   * POST /rest/v1/bookings  with JSON = BookingRequestPayload
   * On success: Supabase normally returns 201 Created.
   * We return Right(()) if 201 or 200, otherwise Left(DbError.SqlError).
   */
  def requestBooking(payload: BookingRequestPayload): IO[Either[DbError, Unit]] = {
    val insertJson = io.circe.Json.obj(
      "slot_id" -> payload.slotId.asJson,
      "appointment_type" -> AppointmentType.NOT_SET.asJson,
      "confirmed" -> io.circe.Json.False,
      "patient_id" -> payload.patientId.asJson,
      "clinic_id" -> payload.clinicId.asJson
    )
    val uri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/bookings")
    val req = Request[IO](
      method = POST,
      uri = uri,
      headers = commonHeaders
    ).withEntity(payload.asJson)

    executeNoContent(req, "booking", "") // no known ID yet for noew booking
  }

  /**
   *  2) Confirm an existing booking (set confirmed = true):
   *     PATCH /rest/v1/bookings?booking_id=eq.<UUID>
   *     with JSON { "confirmed": true }
   *     On success: 200 or 204
   *     If no row matched -> Supabase returns 404
   */
  def confirmBooking(
                      bookingId: UUID,
                      payload: ConfirmBookingPayload
  ): IO[Either[DbError, Unit]] = {
    // Supabase uses PATCH to update
    val patchJson = payload.asJson
    
    val uri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/bookings?booking_id=eq.$bookingId"
    )

    val req = Request[IO](
      method = Method.PATCH,
      uri = uri,
      headers = commonHeaders
    ).withEntity(patchJson)

    executeNoContent(req, "booking", bookingId.toString)
  }  
  

}
