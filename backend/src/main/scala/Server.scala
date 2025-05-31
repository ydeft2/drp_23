package backend

import cats.effect._
import cats.implicits._

import org.http4s._
import org.http4s.dsl.io._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import io.circe.generic.auto._
import io.circe.Json
import io.circe.syntax._
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.server.Router
import org.http4s.server.staticcontent._
import org.http4s.server.middleware.CORS
import org.http4s.implicits._
import org.typelevel.ci.CIStringSyntax 
import com.comcast.ip4s._
import org.http4s.ember.server.EmberServerBuilder
import java.time.Instant


object Server extends IOApp.Simple {

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

  val supabaseUrl = sys.env("SUPABASE_URL")
  val supabaseKey = sys.env("SUPABASE_API_KEY")
  

  // Function to create an entry in patient table on Supabase
  def createPatientEntry(uid: String, reg: RegisterRequest): IO[Either[String, Unit]] = {
    val patient = PatientInsert(
      uid = uid,
      first_name = reg.firstName,
      last_name = reg.lastName,
      dob = reg.dob
    )

    val payload = patient.asJson

    val patientUri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/patients")

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
          case Status.Created =>
            IO.println("Patient entry created successfully") *> IO.pure(Right(()))
          case Status.Ok =>
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

   
  // Function to register an authorised user with Supabase
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
              // Extract the UID from the JSON (assumes field "id")
              json.hcursor.get[String]("id") match {
                case Left(err) =>
                  BadRequest(s"Error parsing user id: $err")
                case Right(uid) =>
                  // Create a patient entry
                  createPatientEntry(uid, reg).flatMap {
                    case Right(_) => Ok(s"User registered successfully with UID: $uid")
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


  def apiRoutes(): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "hello" =>
        Ok("Hello, World!")

      case req @ POST -> Root / "register" =>
        for {
          regReq <- req.as[RegisterRequest]
          response <- registerUserWithSupabase(regReq)
        } yield response
    }

  val staticRoutes = fileService[IO](FileService.Config("public", pathPrefix = ""))

  val run = {
    val httpApp = Router[IO](
      "/api" -> apiRoutes(),
      "/" -> staticRoutes
    ).orNotFound

    val corsHttpApp = CORS(httpApp)

    val port = sys.env.get("PORT").flatMap(p => scala.util.Try(p.toInt).toOption).getOrElse(8080)

    EmberServerBuilder
      .default[IO]
      .withHost(Host.fromString("0.0.0.0").get)
      .withPort(Port.fromInt(port).get)
      .withHttpApp(corsHttpApp)
      .build
      .use(_ => IO.println(s"Server running at http://0.0.0.0:$port") >> IO.never)
  }
}
