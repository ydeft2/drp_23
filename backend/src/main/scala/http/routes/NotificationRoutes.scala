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
import java.util.UUID
import backend.database.*
import backend.domain.notifications.*
import backend.domain.notifications.given

class NotificationRoutes private extends Http4sDsl[IO] {

  // Expects JSON encoding of NotificationRequest in the request body
  private val getNotificationsRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "fetch" =>
      for {
        notificationReq <- req.as[NotificationRequest]
        notificationsRes <- getNotifications(notificationReq)
        response <- notificationsRes match {
                      case Right(notifications) => 
                        IO.println(s"Fetched notifications: ${notifications.length}") *>
                        Ok(notifications.asJson)
                      case Left(error) => 
                        IO.println(s"Error fetching notifications: $error") *>
                        BadRequest(Json.obj("error" -> Json.fromString(error)))
                    }
      } yield response
  }
  // Expects JSON string of UUID in the request body
  private val markNotificationReadRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "markNotificationRead" =>
      for {
        notificationId <- req.as[UUID]
        res <- markNotificationRead(notificationId)
        response <- res match {
                      case Right(_) => 
                        IO.println(s"Notification $notificationId marked as read") *>
                        Ok()
                      case Left(error) => 
                        IO.println(s"Error marking notification as read: $error") *>
                        BadRequest(Json.obj("error" -> Json.fromString(error)))
                    }
      } yield response
  }
  
  val routes = Router(
    "/notifications" -> (getNotificationsRoute <+> markNotificationReadRoute)
    )

}

object NotificationRoutes {
  def apply(): NotificationRoutes = new NotificationRoutes
  //def routes: HttpRoutes[IO] = apply.routes
}