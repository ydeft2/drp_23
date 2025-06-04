package backend.database

import cats.effect.IO
import org.http4s._
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.ci.CIStringSyntax
import io.circe.Json
import io.circe.parser._
import org.http4s.dsl.io._
import java.util.UUID
import backend.domain.auth._
import backend.domain.auth.given
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder

def getUserRoles(userId: UUID): IO[Either[String, RoleResponse]] = {
  val rolesUri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/roles?uid=eq.${userId}")
  val rolesRequest = Request[IO](
    method = Method.GET,
    uri = rolesUri,
    headers = Headers(
      Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
      Header.Raw(ci"apikey", s"$supabaseKey"),
      Header.Raw(ci"Content-Type", "application/json")
    )
  )
  EmberClientBuilder.default[IO].build.use { httpClient =>
    httpClient.fetch(rolesRequest) { response =>
      response.status match {
        case Status.Ok =>
          response.as[RoleResponse].flatMap { roles =>
            IO.pure(Right(roles))
          }
        case _ =>
          response.as[String].flatMap { body =>
            IO.pure(Left(s"Error fetching user roles: ${response.status.code} - $body"))
          }
      }
    }
  }
}

def deleteAccount(userId: UUID): IO[Either[String, Unit]] = {
  val deleteUri = Uri.unsafeFromString(s"$supabaseUrl/auth/v1/admin/users/${userId}")

  val deleteRequest = Request[IO](
    method = Method.DELETE,
    uri = deleteUri,
    headers = Headers(
      Header.Raw(ci"Authorization", s"Bearer ${supabaseKey}"),
      Header.Raw(ci"apikey", s"${supabaseKey}"),
      Header.Raw(ci"Content-Type", "application/json")
    )
  )
  EmberClientBuilder.default[IO].build.use { httpClient =>
    httpClient.fetch(deleteRequest) { response =>
      response.status match {
        case Status.NoContent =>
          IO.println(s"Account $userId deleted successfully") *> IO.pure(Right(()))
        case _ =>
          response.as[String].flatMap { body =>
            IO.pure(Left(s"Error deleting account: ${response.status.code} - $body"))
          }
      }
    }
  }
}
