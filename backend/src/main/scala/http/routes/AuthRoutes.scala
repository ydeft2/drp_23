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
import backend.domain.auth.*
import backend.domain.auth.given

class AuthRoutes private extends Http4sDsl[IO] {
  
  private val registerRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
      case req @ POST -> Root / "register" =>
        for {
          regReq <- req.as[RegisterRequest]
          response <- registerUserWithSupabase(regReq)
        } yield response
  }

  private val accountDetailsRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
      case req @ POST -> Root / "accountDetails" =>
        for {
          userId   <- req.as[UUID]
          patientRes <- getAccountDetails(userId)
          response   <- patientRes match {
                          case Right(details) => Ok(details.asJson)
                          case Left(error)    => BadRequest(Json.obj("error" -> Json.fromString(error)))
                        }
        } yield response
  }
  // Expects a JSON string of User ID in the request body
  private val rolesRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
      case req @ POST -> Root / "roles" =>
        for {
          userId <- req.as[UUID]
          rolesRes <- getUserRoles(userId)
          response <- rolesRes match {
                        case Right(roles) => Ok(roles.asJson)
                        case Left(error)  => BadRequest(Json.obj("error" -> Json.fromString(error)))
                      }
        } yield response
  }
  // Expects a JSON string of User ID in the request body
  private val deleteAccountRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
      case req @ POST -> Root / "deleteAccount" =>
        for {
          userId <- req.as[UUID]
          deleteRes <- deleteAccount(userId)
          response <- deleteRes match {
                        case Right(_) => Ok(Json.obj("message" -> Json.fromString("Account deleted successfully")))
                        case Left(error) => BadRequest(Json.obj("error" -> Json.fromString(error)))
                      }
        } yield response
  }

  val routes = Router(
    "/auth" -> (registerRoute <+> accountDetailsRoute <+> rolesRoute <+> deleteAccountRoute)
  )

}

object AuthRoutes {
  def apply() = new AuthRoutes
  //def routes: HttpRoutes[IO] = apply.routes
}
