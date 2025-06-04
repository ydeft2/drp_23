package backend.http.routes


import backend.database.DbSlots
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

//  private val getSlotsRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
//    case GET -> Root / "fetch" =>
//      for {
//        result <- getAllSlots
//        resp   <- result match {
//          case Right(slotsList) => Ok(slotsList.asJson)
//          case Left(errorMsg) =>
//            BadRequest(Json.obj("error" -> Json.fromString(s"Could not fetch slots: $errorMsg")))
//        }
//      } yield resp
//  }

  /** 1) PATIENT VIEW: GET /slots/list */
  private val listSlotsRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "list" =>
      DbSlots.getAllSlotsForPatients.flatMap {
        case Right(slotList: List[PatientSlotResponse]) =>
          Ok(slotList.asJson)
        case Left(err) =>
          InternalServerError(Json.obj("error" -> Json.fromString(err)))
      }
  }
  
  /** 2) CLINIC VIEW: GET /slots/{slotId} */
  private val getSlotByIdRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / UUIDVar(slotId) =>
      DbSlots.getSlotById(slotId).flatMap {
        case Right(slot: Slot) =>
          Ok(slot.asJson)
        case Left(err) if err.contains("not found") =>
          NotFound(Json.obj("error" -> Json.fromString("Slot not found")))
        case Left(err) =>
          InternalServerError(Json.obj("error" -> Json.fromString(err)))
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
          case Right(_) => Created()
          case Left(error) => BadRequest(Json.obj("Error creating slot: " -> Json.fromString(error.toString)))
        }
      } yield resp
  }

  private val deleteSlotRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ DELETE -> Root / "delete" / UUIDVar(slotId) =>
      DbSlots.deleteSlot(slotId).flatMap {
        case Right(_) =>
          NoContent()  // 204
        case Left(err) if err.contains("not found") =>
          NotFound(Json.obj("error" -> Json.fromString("Slot not found")))
        case Left(err) =>
          InternalServerError(Json.obj("error" -> Json.fromString(err)))        
      }
  }

  val routes = Router(
    "/slots" -> (
      listSlotsRoute <+> 
        getSlotByIdRoute <+>
        createSlotRoute <+> 
        deleteSlotRoute
    )
  )
  
}

object SlotRoutes {
  def apply(): SlotRoutes = new SlotRoutes

}