package backend.http.routes


import backend.database.{DbError, DbSlots, notifyUser, newSlotCreatedMessage}
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceEntityEncoder.*
import io.circe.syntax.*
import io.circe.Json
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.Logger
import backend.http.routes.*
import cats.*
import cats.data.*
import cats.syntax.*
import backend.domain.slots.*

import java.time.Instant
import java.util.UUID


/*
case class Slot(
  slotId: UUID,
  bookingId: UUID,
  clinicID: UUID,
  isTaken: Boolean,
  slotTime: Instant,
  slotLength: Long
  clinicInfo: String,
  createdAt: Instant
)

*/

class SlotRoutes private extends Http4sDsl[IO] {

  // TODO: depending on supabase the whole java time Instant might have to change, genuinely
  given instantQueryParmDecoder: QueryParamDecoder[Instant] =
    QueryParamDecoder[String].map(Instant.parse)

  given uuidQueryParamDecoder: QueryParamDecoder[UUID] =
    QueryParamDecoder[String].map(UUID.fromString)

  object ClinicIdQP extends OptionalQueryParamDecoderMatcher[UUID]("clinic_id")
  object IsTakenQP     extends OptionalQueryParamDecoderMatcher[Boolean]("is_taken")
  object ClinicInfoQP  extends OptionalQueryParamDecoderMatcher[String]("clinic_info")
  object SlotTimeGteQP extends OptionalQueryParamDecoderMatcher[Instant]("slot_time_gte")
  object SlotTimeLteQP extends OptionalQueryParamDecoderMatcher[Instant]("slot_time_lte")
  object LimitQP       extends OptionalQueryParamDecoderMatcher[Int]("limit")
  object OffsetQP      extends OptionalQueryParamDecoderMatcher[Int]("offset")

  private val listAllSlotsRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root =>
      DbSlots.getAllSlotsForPatients.flatMap {
        case Right(slotList) =>
          Ok(slotList.asJson)
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

  /** 1) PATIENT VIEW: GET /slots/list?is_taken=...&clinic_info=...&slot_time_gte=...&limit=...&offset=... */ 
  private val listFilteredSlotsRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "list" :?
      ClinicIdQP(mClinicId) +&
      IsTakenQP(mIsTaken) +&
      ClinicInfoQP(mClinicInfo) +& 
      SlotTimeGteQP(mGte) +&
      SlotTimeLteQP(mLte) +&
      LimitQP(mLimit) +&
      OffsetQP(mOffset) =>
      
        val filter = SlotFilter(
          clinicId = mClinicId,
          isTaken = mIsTaken,
          clinicInfo = mClinicInfo,
          slotTimeGte = mGte,
          slotTimeLte = mLte
        )
        
        val pagination = Pagination(mLimit, mOffset)
        
        DbSlots.getSlots(filter, pagination).flatMap {
          case Right(slotList) =>
            // Return 200 + JSON array even if list is empty
            Ok(slotList.asJson)

          case Left(DbError.NotFound(_, _)) =>
            // Treat “no rows” as an empty array
            Ok(Json.arr())

          case Left(DbError.DecodeError(msg)) =>
            // JSON‐decoding issue
            BadRequest(Json.obj("error" -> Json.fromString(msg)))

          case Left(DbError.SqlError(code, body)) =>
            InternalServerError(Json.obj("error" -> Json.fromString(s"DB error $code: $body")))

          case Left(DbError.Unknown(msg)) =>
            InternalServerError(Json.obj("error" -> Json.fromString(msg)))       
        }
  }
  
  /** 2) CLINIC VIEW: GET /slots/{slotId} */
  private val getSlotByIdRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / UUIDVar(slotId) =>
      DbSlots.getSlotById(slotId).flatMap {
        case Right(slot: Slot) =>
          Ok(slot.asJson)
        case Left(DbError.NotFound(_, _)) =>
          NotFound(Json.obj("error" -> Json.fromString("Slot not found")))
        case Left(DbError.DecodeError(msg)) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Decode error: $msg")))
        case Left(DbError.SqlError(code, body)) =>
          InternalServerError(Json.obj("error" -> Json.fromString(s"DB error $code: $body")))
        case Left(DbError.Unknown(msg)) =>
          InternalServerError(Json.obj("error" -> Json.fromString(msg)))
      }
  }
  
  // Expects a JSON encoded SlotRequest in the body
  private val createSlotRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "create" =>
      for {
        slotReq <- req.as[SlotRequest]
        newSlot = SlotRequest.create(slotReq.clinicId, slotReq.slotTime, slotReq.slotLength)
        ret <- DbSlots.addSlot(newSlot)
        resp <- ret match {
          case Right(_) =>
            notifyUser(slotReq.clinicId, newSlotCreatedMessage(newSlot)) *> Created()

          case Left(DbError.DecodeError(msg)) =>
            BadRequest(Json.obj("error" -> Json.fromString(s"Invalid JSON: $msg")))

          case Left(DbError.SqlError(code, body)) =>
            InternalServerError(Json.obj("error" -> Json.fromString(s"DB error $code: $body")))

          case Left(DbError.Unknown(msg)) =>
            InternalServerError(Json.obj("error" -> Json.fromString(msg)))

          case Left(DbError.NotFound(_, _)) =>
            // Shouldn’t happen on insert, but just in case:
            NotFound(Json.obj("error" -> Json.fromString("Related resource not found")))
        }
      } yield resp
  }

  private val deleteSlotRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ DELETE -> Root / "delete" / UUIDVar(slotId) =>
      DbSlots.deleteSlot(slotId).flatMap {
        case Right(_) =>
          NoContent()
        case Left(DbError.NotFound(_, _)) =>
          NotFound(Json.obj("error" -> Json.fromString("Slot not found")))

        case Left(DbError.SqlError(code, body)) =>
          InternalServerError(Json.obj("error" -> Json.fromString(s"DB error $code: $body")))

        case Left(DbError.Unknown(msg)) =>
          InternalServerError(Json.obj("error" -> Json.fromString(msg)))

        case Left(DbError.DecodeError(msg)) =>
          BadRequest(Json.obj("error" -> Json.fromString(s"Decode error: $msg")))
      }
  }

  val routes = Router(
    "/slots" -> (
      listAllSlotsRoute <+>
      listFilteredSlotsRoute <+>
      getSlotByIdRoute <+>
      createSlotRoute <+>
      deleteSlotRoute
    )
  )
  
}

object SlotRoutes {
  def apply(): SlotRoutes = new SlotRoutes
}