package backend.http.routes

import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceEntityEncoder.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.Json
import cats.*
import cats.data.*
import cats.implicits.*
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.*
import backend.database.{DbBookings, DbError}
import backend.domain.bookings.*
import cats.implicits._

class BookingRoutes private extends Http4sDsl[IO] {

  // TODO: i guess theres both a patient and clinic view of this
  // maybe they will have different filters
  // todo: filters + pagination
  private val listAllBookingsRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root =>
      DbBookings.getAllBookingsForPatients.flatMap {
        case Right(bookingList) =>
          Ok(bookingList.asJson)
        case Left(DbError.NotFound(_, _)) =>
          // “no rows” .. empty array
          Ok(Json.arr())
        case Left(DbError.DecodeError(msg)) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Decode error: $msg")))
        case Left(DbError.SqlError(code, body)) =>
          InternalServerError(Json.obj("error" -> Json.fromString(s"DB error $code: $body")))
        case Left(DbError.Unknown(msg)) =>
          InternalServerError(Json.obj("error" -> Json.fromString(msg)))
      }
  }

  private val getBookingByIdRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / UUIDVar(bookingId) =>
      Ok("getting a specific booking")
  }

  private val requestBookingRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "request" =>
      req.as[BookingRequestPayload].attempt.flatMap {
        case Left(err) =>
          // Malformed JSON
          BadRequest(Json.obj("error" -> Json.fromString(s"Invalid JSON: ${err.getMessage}")))

        case Right(br) =>
          DbBookings.requestBooking(br).flatMap {
            case Right(bookingId) =>
              DbBookings.linkSlotWithBooking(br.slotId, bookingId).flatMap {
                case Right(_) =>
                  Created(Json.obj(
                    "message" -> Json.fromString("Booking requested"),
                    "booking_id" -> bookingId.asJson
                  ))
                case Left(err) =>
                  InternalServerError(Json.obj("error" -> Json.fromString("Failed to link slot")))
              }

            case Left(DbError.DecodeError(msg)) =>
              BadRequest(Json.obj("error" -> Json.fromString(s"Decode error: $msg")))

            case Left(DbError.SqlError(code, body)) if code >= 400 && code < 500 =>
              BadRequest(Json.obj(
                "error" -> Json.fromString("Booking request rejected by DB"),
                "details" -> Json.fromString(body)
              ))

            case Left(DbError.SqlError(code, body)) =>
              InternalServerError(Json.obj(
                "error" -> Json.fromString(s"Database error $code"),
                "info" -> Json.fromString(body)
              ))

            case Left(DbError.NotFound(_, _)) =>
              // foreign key (slot_id, patient_id, or clinic_id) didn’t exist
              NotFound(Json.obj("error" -> Json.fromString("Referenced resource not found")))

            case Left(DbError.Unknown(msg)) =>
              InternalServerError(Json.obj("error" -> Json.fromString(msg)))
          }
      }
  }

  private val confirmBookingRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ PUT -> Root / "confirm" / UUIDVar(bookingId) =>
      req.as[ConfirmBookingPayload].attempt.flatMap {
        case Left(err) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Invalid JSON: ${err.getMessage}")))

        case Right(cb) =>
          DbBookings.confirmBooking(bookingId, cb).flatMap {
            case Right(_) =>
              Ok(Json.obj("message" -> Json.fromString("Booking confirmed")))

            case Left(DbError.NotFound(_, _)) =>
              NotFound(Json.obj("error" -> Json.fromString("Booking not found")))

            case Left(DbError.SqlError(code, body)) if code >= 400 && code < 500 =>
              BadRequest(Json.obj(
                "error"   -> Json.fromString("Cannot confirm booking"),
                "details" -> Json.fromString(body)
              ))

            case Left(DbError.SqlError(code, body)) =>
              InternalServerError(Json.obj(
                "error" -> Json.fromString(s"Database error $code"),
                "info"  -> Json.fromString(body)
              ))

            case Left(DbError.DecodeError(msg)) =>
              BadRequest(Json.obj("error" -> Json.fromString(s"Decode error: $msg")))

            case Left(DbError.Unknown(msg)) =>
              InternalServerError(Json.obj("error" -> Json.fromString(msg)))
          }
      }
  }

  // TODO: so hopefully a clinic can update, say, with an important message
  // ideally this could be in the form of a sort of chat
  // so could be the case that maybe a patient could potentially update a booking, but for the chat feature only
  private val updateBookingsRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "update" / UUIDVar(bookingId) =>
      Ok("updating my bookings")
  }
  
  private val cancelBookingRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case DELETE -> Root / "cancel" / UUIDVar(bookingId) =>
      // First unlink the slot with the booking
      DbBookings.unlinkSlotWithBooking(bookingId).flatMap {
        case Right(_) =>
          // Then delete the booking
          DbBookings.deleteBooking(bookingId).flatMap {
            case Right(_) =>
              NoContent()
            case Left(DbError.NotFound(_, _)) =>
              NotFound(Json.obj("error" -> Json.fromString("Booking not found")))
            case Left(DbError.SqlError(code, body)) =>
              InternalServerError(Json.obj("error" -> Json.fromString(s"DB error $code: $body")))
            case Left(DbError.Unknown(msg)) =>
              InternalServerError(Json.obj("error" -> Json.fromString(msg)))
            case Left(DbError.DecodeError(msg)) =>
              BadRequest(Json.obj("error" -> Json.fromString(s"Decode error: $msg")))
          }
        case Left(err) =>
          // If unlinking fails, log or handle, but still try to delete
          DbBookings.deleteBooking(bookingId).flatMap {
            case Right(_) =>
              // Even if unlink failed, deletion succeeded, so return success
              NoContent()
            case Left(DbError.NotFound(_, _)) =>
              NotFound(Json.obj("error" -> Json.fromString("Booking not found")))
            case Left(DbError.SqlError(code, body)) =>
              InternalServerError(Json.obj("error" -> Json.fromString(s"DB error $code: $body")))
            case Left(DbError.Unknown(msg)) =>
              InternalServerError(Json.obj("error" -> Json.fromString(msg)))
            case Left(DbError.DecodeError(msg)) =>
              BadRequest(Json.obj("error" -> Json.fromString(s"Decode error: $msg")))
          }
      }
  }

  
  val routes = Router(
    "/bookings" -> (
      listAllBookingsRoute <+>
      getBookingByIdRoute <+>
      requestBookingRoute <+>
      confirmBookingRoute <+>
      updateBookingsRoute <+>
      cancelBookingRoute
      )
  )

}

object BookingRoutes {

  def apply(): BookingRoutes = new BookingRoutes

}
