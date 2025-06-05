package http.routes

import cats.*
import cats.data.*
import cats.implicits.*
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.*


class BookingRoutes private extends Http4sDsl[IO] {

  // TODO: i guess theres both a patient and clinic view of this
  // maybe they will have different filters
  private val listAllBookingsRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root =>
      Ok("getting ma bookings")
  }

  private val getBookingByIdRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / UUIDVar(bookingId) =>
      Ok("getting a specific booking")
  }

  private val cancelBookingsRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "cancel" / UUIDVar(bookingId) =>
      Ok("cancelling mai bookings")
  }

  // TODO: so hopefully a clinic can update, say, with an important message
  // ideally this could be in the form of a sort of chat
  // so could be the case that maybe a patient could potentially update a booking, but for the chat feature only
  private val updateBookingsRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "update" / UUIDVar(bookingId) =>
      Ok("updating my bookings")
  }

  val routes = Router(
    "/bookings" -> (
      listAllBookingsRoute <+>
      getBookingByIdRoute <+>
      cancelBookingsRoute <+>
      updateBookingsRoute
      )
  )

}

object BookingRoutes {

  def apply(): BookingRoutes = new BookingRoutes

}
