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


object Server extends IOApp.Simple {

  case class RegisterRequest(
    firstName: String,
    lastName: String,
    dob: String,
    email: String,
    password: String
  )

  val supabaseUrl = sys.env("SUPABASE_URL")
  val supabaseKey = sys.env("SUPABASE_API_KEY")
  
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

    val uri = Uri.unsafeFromString(s"$supabaseUrl/auth/v1/admin/users")

    val request = Request[IO](
      method = Method.POST,
      uri = uri,
      headers = Headers(
        Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
        Header.Raw(ci"apikey", s"$supabaseKey"),
        Header.Raw(ci"Content-Type", "application/json")
      )
    ).withEntity(payload)

    EmberClientBuilder.default[IO].build.use { httpClient =>
      httpClient.fetch(request) { response =>
        response.as[Json].flatMap { json =>
          if (response.status.isSuccess) {
            val userId = json.hcursor.downField("user").get[String]("id").toOption.get

            // Add patient record via Supabase REST API
            val insertPatientPayload = Json.obj(
              "id" := userId,
              "first_name" := reg.firstName,
              "last_name" := reg.lastName,
              "dob" := reg.dob,
              "email" := reg.email
            )

            val patientUri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/patients")
            val insertRequest = Request[IO](
              method = Method.POST,
              uri = patientUri,
              headers = Headers(
                Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
                Header.Raw(ci"apikey", s"$supabaseKey"),
                Header.Raw(ci"Content-Type", "application/json"),
                Header.Raw(ci"Prefer", "return=representation")
              )
            ).withEntity(insertPatientPayload.noSpaces)

            httpClient.fetch(insertRequest) { patientResponse =>
              if (patientResponse.status.isSuccess)
                Ok("User and patient registered.")
              else
                BadRequest("User created, but failed to insert patient data.")
            }
          } else {
            BadRequest(s"Failed to register user: ${response.status.code}")
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
