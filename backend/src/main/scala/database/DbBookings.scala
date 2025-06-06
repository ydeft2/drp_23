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
  
  def deleteBooking(bookingId: UUID) = {
    val deleteUri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/bookings?booking_id=eq.${bookingId}")
    val deleteRequest = Request[IO](
      method = Method.DELETE,
      uri = deleteUri,
      headers = commonHeaders
    )
    executeNoContent(deleteRequest, "booking", bookingId.toString)
  }

  def getAllBookingsForPatients: IO[Either[DbError, List[BookingResponse]]] =
    getBookings(BookingFilter(None, None, None, None, None, None), Pagination(None, None))


  def getBookings(
      filter: BookingFilter,
      pagination: Pagination
  ): IO[Either[DbError, List[BookingResponse]]] = {

    val baseSelect =
      "patient_id,clinic_id,confirmed,slot:slot_id(slot_time,slot_length,clinic_info)"

    val filterClauses = List(
      filter.isConfirmed.map(c => s"confirmed=eq.$c"),
      filter.clinicInfo.map(ci => s"slot->>clinic_info=ilike.%25$ci%25"),
      filter.slotTimeGte.map(ts => s"slot->>slot_time=gte.$ts"),
      filter.slotTimeLte.map(ts => s"slot->>slot_time=lte.$ts"),
      filter.patientId.map(id => s"patient_id=eq.$id"),
      filter.clinicId.map(id => s"clinic_id=eq.$id")
    ).flatten.map("&" + _).mkString("")


    val paginationClauses = s"&limit=${pagination.limit}&offset=${pagination.offset}"


    val fullUri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/bookings?select=$baseSelect$filterClauses$paginationClauses"
    )

    val req = Request[IO](
      method = Method.GET,
      uri = fullUri,
      headers = commonHeaders
    )

    fetchAndDecode[List[BookingDto]](req, "bookings").map(_.map(_.map(toBookingResponse)))
  }

}
