package backend.database

import cats.effect.IO
import org.http4s._
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.ci.CIStringSyntax
import io.circe.Json
import org.http4s.dsl.io._

case class AuthRequest(
    uid: String,
    accessToken: String
)

case class RegisterRequest(
    firstName: String,
    lastName: String,
    dob: String,
    email: String,
    password: String
)

case class PatientInsert(
    uid: String,
    first_name: String,
    last_name: String,
    dob: String
)

case class NotificationResponse(
    id: Int,
    message: String,
    created_at: String,
    is_read: Boolean
)

val supabaseUrl: String = sys.env("SUPABASE_URL")
val supabaseKey: String = sys.env("SUPABASE_API_KEY")

def createPatientEntry(uid: String, reg: RegisterRequest): IO[Either[String, Unit]] = {
  val patient = PatientInsert(
    uid = uid,
    first_name = reg.firstName,
    last_name = reg.lastName,
    dob = reg.dob
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
      "dob" := reg.dob
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
            json.hcursor.get[String]("id") match {
              case Left(err) =>
                BadRequest(s"Error parsing user id: $err")
              case Right(uid) =>
                createPatientEntry(uid, reg).flatMap {
                  case Right(_) =>
                    // insertUserRole(uid, is_patient = "true").flatMap {
                    //   case Right(_) =>
                    //     notifyUser(uid, s"Welcome ${reg.firstName}, your account has been created successfully.").flatMap {
                    //       case Right(_) => Ok(s"User registered successfully with UID: $uid")
                    //       case Left(err) => BadRequest(s"Error sending notification: $err")
                    //     }
                    //   case Left(err) => BadRequest(s"Error inserting user role: $err")
                    // }
                    Ok() // TEMP
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

def insertUserRole(
    uid: String,
    is_patient: String
): IO[Either[String, Unit]] = {
  val payload = Json.obj(
    "uid" := uid,
    "is_patient" := is_patient
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