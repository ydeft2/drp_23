package backend.database

import cats.effect.IO
import io.circe.Json
import io.circe.syntax._
import org.http4s._
import org.http4s.Method._
import org.http4s.circe._
import org.typelevel.ci.CIString
import org.http4s.Header.Raw
import io.circe.Decoder

import java.util.UUID
import backend.domain.bookings._



object DbBookings {


  /**
   * 1) Insert a new booking request:
   * POST /rest/v1/bookings  with JSON = BookingRequestPayload
   * On success: Supabase normally returns 201 Created.
   * We return Right(()) if 201 or 200, otherwise Left(DbError.SqlError).
   */
  def requestBooking(payload: BookingRequestPayload): IO[Either[DbError, UUID]] = {
    val uri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/bookings?select=booking_id")

    val preferHeader = Header.Raw(CIString("Prefer"), "return=representation")

    val req = Request[IO](
      method = Method.POST,
      uri = uri,
      headers = commonHeaders.put(preferHeader)
    ).withEntity(payload.asJson)

    case class BookingIdResponse(booking_id: UUID)
    given Decoder[BookingIdResponse] = Decoder.forProduct1("booking_id")(BookingIdResponse.apply)

    for {
      result <- fetchAndDecode[List[BookingIdResponse]](req, "booking")
      response <- result match {
        case Right(bookings) if bookings.nonEmpty =>
          val id = bookings.head.booking_id
          IO.pure(Right(id))

        case Right(_) =>
          IO.pure(Left(DbError.Unknown("No booking returned")))

        case Left(err) =>
          IO.pure(Left(err))
      }
    } yield response
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
      "booking_id,patient_id,clinic_id,confirmed,slot_time,slot_length,clinic_info,appointment_type"
      
    val selectQuery = s"?select=$baseSelect"

    val filterClauses = List(
      filter.isConfirmed.map(c => s"confirmed=eq.$c"),
      filter.clinicInfo.map(ci => s"slot->>clinic_info=ilike.%25$ci%25"),
      filter.slotTimeGte.map(gt => s"slot->>slot_time=gte.$gt"),
      filter.slotTimeLte.map(lt => s"slot->>slot_time=lte.$lt"),
      filter.patientId.map(id => s"patient_id=eq.$id"),
      filter.clinicId.map(id => s"clinic_id=eq.$id")
    ).flatten
      .map("&" + _)
      .mkString("")
    
    val paginationClauses = s"&limit=${pagination.limit}&offset=${pagination.offset}"

    val fullUri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/bookings_with_slots$selectQuery$filterClauses$paginationClauses"
    )
    
    val req = Request[IO](
      method = Method.GET,
      uri = fullUri,
      headers = commonHeaders
    )

    fetchAndDecode[List[BookingResponse]](req, "bookings")
  }

  def linkSlotWithBooking(slotId: UUID, bookingId: UUID): IO[Either[DbError, Unit]] = {

    IO.println(s"Linking slot $slotId with booking $bookingId")

    val patchJson = Json.obj(
      "booking_id" -> bookingId.asJson,
      "is_taken" -> Json.True
    )

    val uri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/slots?slot_id=eq.$slotId"
    )

    val req = Request[IO](
      method = Method.PATCH,
      uri = uri,
      headers = commonHeaders
    ).withEntity(patchJson)

    executeNoContent(req, "slot", slotId.toString)
  }


  def unlinkSlotWithBooking(bookingId: UUID): IO[Either[DbError, Unit]] = {
    val patchJson = Json.obj(
      "booking_id" -> Json.Null,
      "is_taken" -> Json.False
    )

    val uri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/slots?booking_id=eq.$bookingId"
    )

    val req = Request[IO](
      method = Method.PATCH,
      uri = uri,
      headers = commonHeaders
    ).withEntity(patchJson)

    executeNoContent(req, "slot", s"booking_id=$bookingId")
  }

  def getSlotAndClinicForBooking(bookingId: UUID): IO[Either[DbError, (UUID, UUID)]] = {
    val uri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/bookings?select=slot_id,clinic_id&booking_id=eq.$bookingId"
    )
    val req = Request[IO](Method.GET, uri, headers = commonHeaders)

    fetchAndDecode[List[BookingSlotClinic]](req, "booking").map {
      case Right(List(BookingSlotClinic(slot, clinic))) =>
        Right((slot, clinic))
      case Right(Nil) =>
        Left(DbError.NotFound("booking", bookingId.toString))
      case Right(_ :: _ :: _) =>
        Left(DbError.Unknown("Multiple matching bookings"))
      case Left(err) =>
        Left(err)
    }
  }

}
