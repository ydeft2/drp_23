package backend.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.circe.CirceEntityEncoder.*
import io.circe.syntax.*
import io.circe.Json
import cats.effect.IO
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.*
import backend.database.DbInterests
import backend.domain.interests.InterestRequest

class InterestRoutes extends Http4sDsl[IO] {
  
  private val create: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root =>
      req.as[InterestRequest].flatMap { ir =>
        DbInterests.addInterest(ir).flatMap {
          case Right(_)  => Created(Json.obj("message" -> Json.fromString("Interest recorded")))
          case Left(err) => BadRequest(Json.obj("error" -> Json.fromString(err)))
        }
      }
  }

  val routes: HttpRoutes[IO] = Router("/interests" -> create)
}
object InterestRoutes { def apply() = new InterestRoutes }
