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
          response.as[List[RoleResponse]].flatMap { roles =>
            IO.pure(Right(roles(0)))
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

def getAccountDetails(userId: UUID): IO[Either[String, AccountDetailsResponse]] = {
  val userUri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/patients?uid=eq.${userId}")

  val userRequest = Request[IO](
    method = Method.GET,
    uri = userUri,
    headers = Headers(
      Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
      Header.Raw(ci"apikey", s"${supabaseKey}"),
      Header.Raw(ci"Content-Type", "application/json")
    )
  )

  EmberClientBuilder.default[IO].build.use { httpClient =>
    httpClient.fetch(userRequest) { response =>
      response.status match {
        case Status.Ok =>
          response.as[List[AccountDetailsResponse]].flatMap { accounts =>
            accounts.headOption match {
              case Some(accountDetails) => IO.pure(Right(accountDetails))
              case None => IO.pure(Left("No account details found"))
            }
          }
        case _ =>
          response.as[String].flatMap { body =>
            IO.pure(Left(s"Error fetching account details: ${response.status.code} - $body"))
          }
      }
    }
  }
}

def insertUserRole(
    userId: UUID,
    isPatient: Boolean
): IO[Either[String, Unit]] = {

  val payload = Json.obj(
    "uid" := userId.toString,
    "is_patient" := isPatient
  )

  val roleUri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/roles")

  val roleRequest = Request[IO](
    method = Method.POST,
    uri = roleUri,
    headers = Headers(
      Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
      Header.Raw(ci"apikey", s"$supabaseKey"),
      Header.Raw(ci"Content-Type", "application/json"),
      Header.Raw(ci"Prefer", "return=minimal")
    )
  ).withEntity(payload)

  EmberClientBuilder.default[IO].build.use { httpClient =>
    httpClient.fetch(roleRequest) { response =>
      response.status match {
        case Status.Created | Status.Ok =>
          IO.println("User role inserted successfully") *> IO.pure(Right(()))
        case _ =>
          response.as[String].flatMap { body =>
            IO.println(s"Error response: Status ${response.status.code}, Body: $body") *>
            IO.pure(Left(s"Error inserting user role: ${response.status.code} - $body"))
          }
      }
    }
  }
}

def createPatientEntry(userId: UUID, reg: RegisterRequest): IO[Either[String, Unit]] = {
  val patient = PatientInsert(
    uid = userId.toString,
    first_name = reg.firstName,
    last_name = reg.lastName,
    dob = reg.dob,
    address = reg.address,
    latitude = reg.latitude,
    longitude = reg.longitude
  )

  val payload = patient.asJson

  val patientUri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/patients?select=*")

  val patientRequest = Request[IO](
    method = Method.POST,
    uri = patientUri,
    headers = Headers(
      Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
      Header.Raw(ci"apikey", supabaseKey),
      Header.Raw(ci"Content-Type", "application/json"),
      Header.Raw(ci"Prefer", "return=minimal")
    )
  ).withEntity(payload)

  EmberClientBuilder.default[IO].build.use { httpClient =>
    httpClient.fetch(patientRequest) { response =>
      response.status match {
        case Status.Created | Status.Ok =>
          IO.println("Patient entry created successfully") *> IO.pure(Right(()))
        case _ =>
          response.as[String].flatMap { body =>
            IO.println(s"Error response: Status ${response.status.code}, Body: $body") *>
            IO.pure(Left(s"Error creating patient entry: ${response.status.code} - $body"))
          }
      }
    }
  }
}

def registerUserWithSupabase(reg: RegisterRequest): IO[Response[IO]] = {
  val payload = Json.obj(
    "email" := reg.email,
    "password" := reg.password,
    "data" := Json.obj(
      "first_name" := reg.firstName,
      "last_name" := reg.lastName,
      "dob" := reg.dob,
      "address" := reg.address,
      "latitude" := reg.latitude,
      "longitude" := reg.longitude
    )
  )

  val userUri = Uri.unsafeFromString(s"$supabaseUrl/auth/v1/admin/users")

  val userRequest = Request[IO](
    method = Method.POST,
    uri = userUri,
    headers = Headers(
      Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
      Header.Raw(ci"apikey", s"$supabaseKey"),
      Header.Raw(ci"Content-Type", "application/json")
    )
  ).withEntity(payload)

  EmberClientBuilder.default[IO].build.use { httpClient =>
    httpClient.fetch(userRequest) { userResponse =>
      userResponse.status match {
        case Status.Ok | Status.Created =>
          userResponse.as[Json].flatMap { json =>
            json.hcursor.get[UUID]("id") match {
              case Left(err) =>
                BadRequest(s"Error parsing user id: $err")
              case Right(uid) =>
                createPatientEntry(uid, reg).flatMap {
                  case Right(_) =>
                    insertUserRole(uid, true).flatMap {
                      case Right(_) =>
                        notifyUser(uid, s"Welcome ${reg.firstName}, your account has been created successfully.").flatMap {
                          case Right(_) => Ok(s"User registered successfully with UID: $uid")
                          case Left(err) => BadRequest(s"Error sending notification: $err")
                        }
                      case Left(err) => BadRequest(s"Error inserting user role: $err")
                    }
                  case Left(err) => BadRequest(s"Error creating patient entry: $err")
                }
            }
          }
        case _ =>
          userResponse.as[String].flatMap { body =>
            BadRequest(s"Error registering user: ${userResponse.status.code} - $body")
          }
      }
    }
  }
}