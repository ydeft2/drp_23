package backend.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
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
import backend.database.*
import backend.domain.slots._


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

  private val getSlotsRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "fetch" =>
      // Logic to fetch slots
      Ok("Fetching slots...")
  }

  private val createSlotRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "create" =>
      for {
        slotReq <- req.as[SlotRequest]
        newSlot = SlotRequest.create(slotReq.clinicId, slotReq.slotTime, slotReq.slotLength)
        ret <- addSlot(newSlot)
        resp <- ret match {
          case Right(_) => Created()
          case Left(error) => BadRequest(Json.obj("Error creating slot: " -> Json.fromString(error.toString)))
        }
      } yield resp
  }

  private val deleteSlotRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ DELETE -> Root / "delete" =>
      // Logic to delete a slot
      Ok("Deleting a slot...")
  }

  val routes = Router(
    "/slots" -> (getSlotsRoute <+> createSlotRoute <+> deleteSlotRoute)
  )
}

object SlotRoutes {
  def apply(): SlotRoutes = new SlotRoutes

}