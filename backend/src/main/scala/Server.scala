import cats.effect.{IO, IOApp}
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.implicits._
import com.comcast.ip4s.{Host, Port}
import org.http4s.server.staticcontent._
import org.http4s.server.middleware.CORS

object Server extends IOApp.Simple {

  // API routes
  val apiRoutes = HttpRoutes.of[IO] {
    case GET -> Root / "hello" =>
      Ok("Hello, World!")
  }

  // Static file routes (serves files from backend/public)
  val staticRoutes = fileService[IO](FileService.Config("public", pathPrefix = ""))

  // Combine routes: API under /api, static files at root
  val httpApp = Router[IO](
    "/api" -> apiRoutes,
    "/"    -> staticRoutes
  ).orNotFound

  // Wrap the httpApp with CORS middleware
  val corsHttpApp = CORS(httpApp)

  val port = sys.env.get("PORT").flatMap(p => scala.util.Try(p.toInt).toOption).getOrElse(8080)

  val run = EmberServerBuilder
    .default[IO]
    .withHost(Host.fromString("0.0.0.0").get)
    .withPort(Port.fromInt(port).get)
    .withHttpApp(corsHttpApp)
    .build
    .use(_ => IO.println(s"Server running at http://0.0.0.0:$port") >> IO.never)
}
