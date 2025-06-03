package backend.http.routes

import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.effect.*
import cats.implicits.*
import cats.data.*
import cats.syntax.*

// Every set of endpoints will have its own class specific to that set of responsibilities
class HealthRoutes private extends Http4sDsl[IO] {

  // dont want to expose endpoints individually
  private val healthRoute: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root =>
      Ok("All going great!")
  }

  // Path prefix for all endpoints this class is responsible for
  // We only want to expose this router to the outside world
  val routes = Router(
    "/health" -> healthRoute
  )

}

object HealthRoutes {
  def apply() = new HealthRoutes
  //def routes: HttpRoutes[IO] = apply.routes
}