package backend.database


import cats.effect.IO
import org.http4s._
import org.http4s.circe._
import io.circe.syntax._
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.ci.CIStringSyntax
import io.circe.Json
import io.circe.parser._
import org.http4s.dsl.io._
import java.util.UUID
import backend.domain.notifications._
import backend.domain.notifications.*
import backend.domain.notifications.Notification.given
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
import io.circe.Decoder

given Decoder[Notification] = Notification.decoder
given EntityDecoder[IO, List[Notification]] = jsonOf[IO, List[Notification]]


// This is intended to be a function only used by the backend to notify users when necessary.
def notifyUser(userId: UUID, message: String): IO[Either[String, Unit]] = {
  
  val payload = NotificationRequest.create(
    userId = userId,
    message = message
  ).asJson

  val notifyUri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/notifications")

  val req = Request[IO](
    method = Method.POST,
    uri = notifyUri,
    headers = Headers(
      Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
      Header.Raw(ci"apikey", s"$supabaseKey"),
      Header.Raw(ci"Content-Type", "application/json"),
      Header.Raw(ci"Prefer", "return=minimal")
    )
  ).withEntity(payload)

  EmberClientBuilder.default[IO].build.use { httpClient =>
    httpClient.fetch(req) { response =>
      response.status match {
        case Status.Created | Status.Ok =>
          IO.println("Notification sent successfully") *> IO.pure(Right(()))
        case _ =>
          IO.pure(Left(s"Failed to send notification: ${response.status}"))
      }
    }
  }
}


def getNotifications(notificationsRequest: NotificationRequest): IO[Either[String, List[Notification]]] = {
  
  val getNotificationsUri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/notifications?user_id=eq.${notificationsRequest.userId}")

  val req = Request[IO](
    method = Method.GET,
    uri = getNotificationsUri,
    headers = Headers(
      Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
      Header.Raw(ci"apikey", s"$supabaseKey"),
      Header.Raw(ci"Content-Type", "application/json"),
      Header.Raw(ci"Accept", "application/json")
    )     
  )


  EmberClientBuilder.default[IO].build.use { httpClient =>
    httpClient.fetch(req) { response =>
      response.status match {
        case Status.Ok => 
          response.as[List[Notification]].attempt.map {
            case Right(notifications) => Right(notifications)
            case Left(err) => Left(s"Failed to decode notifications: ${err.getMessage}")
          }
        case _ =>
            IO.pure(Left(s"Error fetching notifications: ${response.status.code}"))
      }
    }
  }
}

def markNotificationRead(notificationId: UUID): IO[Either[String, Unit]] = {
  val updateUri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/notifications?notification_id=eq.$notificationId")
  val payload = io.circe.Json.obj("is_read" := true)
  val request = Request[IO](
    method = Method.PATCH,
    uri = updateUri,
    headers = Headers(
      Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
      Header.Raw(ci"apikey", s"$supabaseKey"),
      Header.Raw(ci"Content-Type", "application/json"),
      Header.Raw(ci"Prefer", "return=minimal")
    )
  ).withEntity(payload)
  EmberClientBuilder.default[IO].build.use { httpClient =>
    httpClient.fetch(request) { response =>
      response.status match {
        case Status.Ok | Status.NoContent => IO.pure(Right(()))
        case _ => response.as[String].flatMap(body => IO.pure(Left(s"Error: ${response.status.code} - $body")))
      }
    }
  }
}