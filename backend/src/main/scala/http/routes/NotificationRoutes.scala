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

class NotificationRoutes private extends Http4sDsl[IO] {

  private val getNotificationsRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "notifications" =>
      for {
        authReq <- req.as[AuthRequest]
        notificationsRes <- getNotifications(authReq)
        response <- notificationsRes match {
                      case Right(notifications) => Ok(notifications.asJson)
                      case Left(error) => BadRequest(Json.obj("error" -> Json.fromString(error)))
                    }
      } yield response
  }
  private val markNotificationReadRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "markNotificationRead" =>
      for {
        json <- req.as[Json]
        uid = json.hcursor.get[String]("uid").getOrElse("")
        id = json.hcursor.get[Int]("id").getOrElse(-1)
        res <- if (uid.nonEmpty && id != -1)
                markNotificationRead(uid, id).flatMap {
                  case Right(_) => Ok(Json.obj("success" := true))
                  case Left(err) => BadRequest(Json.obj("error" := err))
                }
              else BadRequest(Json.obj("error" := "Missing uid or id"))
      } yield res
  }
  
  val routes = Router(
    "/notifications" -> (getNotificationsRoute <+> markNotificationReadRoute)
    )

}

object NotificationRoutes {
  def apply(): NotificationRoutes = new NotificationRoutes
  //def routes: HttpRoutes[IO] = apply.routes
}