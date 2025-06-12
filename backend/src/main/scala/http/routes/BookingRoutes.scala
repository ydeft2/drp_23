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
import org.http4s.*
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.*
import backend.database.{DbBookings, DbError, DbInterests, notifyUser}
import backend.domain.bookings.*
import backend.domain.interests.PatientInterestRecord
import cats.implicits.*

import java.time.Instant
import java.util.UUID

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

  // TODO: We can move these elsewhere
  given instantQueryParmDecoder: QueryParamDecoder[Instant] =
    QueryParamDecoder[String].map(Instant.parse)

  given uuidQueryParamDecoder: QueryParamDecoder[UUID] =
    QueryParamDecoder[String].map(UUID.fromString)

  object ConfirmedQP    extends OptionalQueryParamDecoderMatcher[Boolean]("is_confirmed")
  object ClinicInfoQP   extends OptionalQueryParamDecoderMatcher[String] ("clinic_info")
  object SlotTimeGteQP  extends OptionalQueryParamDecoderMatcher[Instant]("slot_time_gte")
  object SlotTimeLteQP  extends OptionalQueryParamDecoderMatcher[Instant]("slot_time_lte")
  object PatientIdQP    extends OptionalQueryParamDecoderMatcher[UUID]   ("patient_id")
  object ClinicIdQP     extends OptionalQueryParamDecoderMatcher[UUID]   ("clinic_id")
  object LimitQP        extends OptionalQueryParamDecoderMatcher[Int]    ("limit")
  object OffsetQP       extends OptionalQueryParamDecoderMatcher[Int]    ("offset")

  /** 1) GET /bookings/list?...filters...&limit=&offset= */
  private val listFilteredBookingsRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "list" :?
      ConfirmedQP(mConfirmed)   +&
      ClinicInfoQP(mClinicInfo) +&
      SlotTimeGteQP(mGte)       +&
      SlotTimeLteQP(mLte)       +&
      PatientIdQP(mPatient)     +&
      ClinicIdQP(mClinic)       +&
      LimitQP(mLimit)           +&
      OffsetQP(mOffset) =>

      val filter = BookingFilter(
        isConfirmed = mConfirmed,
        clinicInfo  = mClinicInfo,
        slotTimeGte = mGte,
        slotTimeLte = mLte,
        patientId   = mPatient,
        clinicId    = mClinic
      )
      val pagination = Pagination(mLimit, mOffset)

      DbBookings.getBookings(filter, pagination).flatMap {
        case Right(bookings) =>
          // directly a List[BookingResponse]
          Ok(bookings.asJson)

        case Left(DbError.NotFound(_, _)) =>
          Ok(Json.arr())

        case Left(DbError.DecodeError(msg)) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Decode error: $msg")))

        case Left(DbError.SqlError(code, body)) =>
          // for 4xx from Supabase (e.g. foreign-key violation), return 400
          if (400 until 500 contains code)
            BadRequest(Json.obj(
              "error"   -> Json.fromString("Booking request rejected by DB"),
              "details" -> Json.fromString(body)
            ))
          else
            InternalServerError(Json.obj(
              "error"   -> Json.fromString(s"Database error $code"),
              "details" -> Json.fromString(body)
            ))

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

  // TODO: can refactor to use a for comp i believe
  private val cancelBookingRoute_v2: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case DELETE -> Root / "cancel" / UUIDVar(bookingId) =>
      // 1) Look up the booking so we know the clinicId
      DbBookings.getSlotAndClinicForBooking(bookingId).flatMap {
        case Left(_) =>
          NotFound(Json.obj("error" -> Json.fromString("Booking not found")))
        case Right((slotId, clinicId)) =>
          // 2) Unlink and delete
          DbBookings.unlinkSlotWithBooking(bookingId) *>
            DbBookings.deleteBooking(bookingId).flatMap {
              case Left(DbError.NotFound(_, _)) =>
                NotFound(Json.obj("error" -> Json.fromString("Booking not found")))
              case Left(DbError.SqlError(code, body)) =>
                InternalServerError(Json.obj("error" -> Json.fromString(s"DB error $code: $body")))
              case Left(DbError.DecodeError(msg)) =>
                BadRequest(Json.obj("error" -> Json.fromString(s"Decode error: $msg")))
              case Left(DbError.Unknown(msg)) =>
                InternalServerError(Json.obj("error" -> Json.fromString(msg)))
              case Right(_) =>
                // 3) Fetch all watchers of that clinic
                DbInterests.getInterestsForClinic(clinicId).flatMap {
                  case Left(_) =>
                    // Even if we can’t load watchers, the cancel succeeded
                    NoContent()
                  case Right(watchers: List[PatientInterestRecord]) =>
                    // 4) Notify each watcher
                    watchers.traverse_ { pr =>
                      notifyUser(
                        pr.patient_id,
                        s"A slot has just opened at clinic $clinicId!"
                      )
                    } *> NoContent()
                }
            }
      }
  }

  // I think this is better for data consistency... but worse for user ux
  // TODO: can refactor to use a for comp i believe
  private val cancelBookingRoute_v3: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case DELETE -> Root / "cancel" / UUIDVar(bookingId) =>
      // 1) Lookup booking to get (slotId, clinicId)
      DbBookings.getSlotAndClinicForBooking(bookingId).flatMap {
        case Left(_) =>
          // Booking doesn't exist
          NotFound(Json.obj("error" -> Json.fromString("Booking not found")))

        case Right((slotId, clinicId)) =>
          // 2) First unlink the slot; if that fails, abort
          DbBookings.unlinkSlotWithBooking(bookingId).flatMap {
            case Left(err) =>
              // Could not free the slot → rollback
              InternalServerError(Json.obj("error" -> Json.fromString(
                s"Failed to free slot: $err"
              )))
            case Right(_) =>
              // 3) Now delete the booking row
              DbBookings.deleteBooking(bookingId).flatMap {
                case Left(DbError.NotFound(_, _)) =>
                  NotFound(Json.obj("error" -> Json.fromString("Booking not found")))

                case Left(DbError.SqlError(code, body)) =>
                  InternalServerError(Json.obj(
                    "error" -> Json.fromString(s"DB error $code"),
                    "details" -> Json.fromString(body)
                  ))

                case Left(DbError.DecodeError(msg)) =>
                  BadRequest(Json.obj("error" -> Json.fromString(s"Decode error: $msg")))

                case Left(DbError.Unknown(msg)) =>
                  InternalServerError(Json.obj("error" -> Json.fromString(msg)))

                case Right(_) =>
                  // 4) Fetch all watchers of that clinic and notify them
                  DbInterests.getInterestsForClinic(clinicId).flatMap {
                    case Left(_) =>
                      // If we can't load watchers, still return success
                      NoContent()

                    case Right(watchers: List[PatientInterestRecord]) =>
                      watchers.traverse_ { pr =>
                        notifyUser(
                          pr.patient_id,
                          s"A slot has just opened at clinic $clinicId!"
                        )
                      } *> NoContent()
                  }
              }
          }
      }
  }

  
  val routes = Router(
    "/bookings" -> (
      listFilteredBookingsRoute <+>
      listAllBookingsRoute <+>
      getBookingByIdRoute <+>
      requestBookingRoute <+>
      confirmBookingRoute <+>
      updateBookingsRoute <+>
      cancelBookingRoute_v3
      )
  )

}

object BookingRoutes {

  def apply(): BookingRoutes = new BookingRoutes

}
