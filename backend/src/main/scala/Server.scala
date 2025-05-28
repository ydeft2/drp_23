import cats.effect.{IO, IOApp}
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.implicits._
import com.comcast.ip4s.{Host, Port}
import org.http4s.server.staticcontent._

object Server extends IOApp.Simple {

  // API routes
  val apiRoutes = HttpRoutes.of[IO] {
    case GET -> Root / "hello" =>
      Ok("Hello, World!")
  }

  // Static file routes (serves files from backend/public)
  val staticRoutes = fileService[IO](FileService.Config("/home/guy/Scala/jstest/backend/public"))

  // Combine routes: API under /api, static files at root
  val httpApp = Router[IO](
    "/api" -> apiRoutes,
    "/"    -> staticRoutes
  ).orNotFound

  val run = EmberServerBuilder
    .default[IO]
    .withHost(Host.fromString("localhost").get)
    .withPort(Port.fromInt(8080).get)
    .withHttpApp(httpApp)
    .build
    .use(_ => IO.println("Server running at http://localhost:8080") >> IO.never)
}