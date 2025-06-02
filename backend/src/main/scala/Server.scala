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

  def apiRoutes(): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "hello" =>
        Ok("Hello, World!")

      case req @ POST -> Root / "register" =>
        for {
          regReq <- req.as[RegisterRequest]
          response <- registerUserWithSupabase(regReq)
        } yield response

      case req @ POST -> Root / "accountDetails" =>
        for {
          authReq    <- req.as[AccountDetailsRequest]
          patientRes <- getAccountDetails(authReq)
          response   <- patientRes match {
                          case Right(details) => Ok(details.asJson)
                          case Left(error)    => BadRequest(Json.obj("error" -> Json.fromString(error)))
                        }
        } yield response
      case req @ POST -> Root / "roles" =>
        //debug printing
        println(s"Received request for role check")
        for {
          authReq <- req.as[AuthRequest]
          rolesRes <- getUserRoles(authReq)
          response <- rolesRes match {
                        case Right(roles) => Ok(roles.asJson)
                        case Left(error)  => BadRequest(Json.obj("error" -> Json.fromString(error)))
                      }
        } yield response
      case req @ POST -> Root / "deleteAccount" =>
        for {
          authReq <- req.as[AuthRequest]
          deleteRes <- deleteAccount(authReq)
          response <- deleteRes match {
                        case Right(_) => Ok(Json.obj("message" -> Json.fromString("Account deleted successfully")))
                        case Left(error) => BadRequest(Json.obj("error" -> Json.fromString(error)))
                      }
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
