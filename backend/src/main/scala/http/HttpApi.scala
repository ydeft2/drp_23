package backend.http

import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.effect.*
import cats.implicits.*
import backend.http.routes.*
import cats.Monad
import org.http4s.server.staticcontent._
import fs2.concurrent.Topic
import backend.domain.messages.Message


class HttpApi private (messageTopic: Topic[IO, Message]) {

  private val healthRoutes = HealthRoutes().routes
  private val authRoutes = AuthRoutes().routes
  private val notificationRoutes = NotificationRoutes().routes
  private val slotRoutes = SlotRoutes().routes
  private val bookingRoutes = BookingRoutes().routes
  private val messageRoutes = MessageRoutes(messageTopic).routes
  private val clinicRoutes = ClinicRoutes().routes
  private val interestRoutes = InterestRoutes().routes


  val endpoints = Router(
    "/api" -> (
      healthRoutes <+>
      slotRoutes <+>
      bookingRoutes <+>
      authRoutes <+>
      notificationRoutes <+>
      messageRoutes <+>
      clinicRoutes <+>
      interestRoutes
    ),
    "/" -> fileService[IO](FileService.Config("public", pathPrefix = ""))
  )
}

object HttpApi {
  def apply(messageTopic: Topic[IO, Message]): HttpApi = new HttpApi(messageTopic)
}
