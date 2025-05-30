package backend

import cats.effect.{IO, IOApp}
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.implicits._
import org.http4s.server.staticcontent._
import org.http4s.server.middleware.CORS
import org.mindrot.jbcrypt.BCrypt
import org.http4s.circe._              // added for CirceEntityDecoder
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder  // added for CirceEntityDecoder
import com.comcast.ip4s.{Host, Port}
import io.circe.generic.auto._
import doobie.implicits._
import doobie.util.transactor.Transactor

object Server extends IOApp.Simple {

  case class RegisterRequest(
    firstName: String,
    lastName: String,
    dob: String,
    email: String,
    password: String
  )

  
  def insertUser(regReq: RegisterRequest): IO[Int] = {
    // Todo: Implement user insertion logic
    val hashedPassword = BCrypt.hashpw(regReq.password, BCrypt.gensalt())
    // mocked return value - we should acc insert into the database
    IO.pure(1) 
  } 

  // Builds API routes using the provided transactor.
  def apiRoutes(): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "hello" =>
        Ok("Hello, World!")
      case req @ POST -> Root / "register" =>
        // TODO: Implement user registration logic
        Ok("User registered MOCK")
    }

  // Static file routes (serves files from backend/public)
  val staticRoutes = fileService[IO](FileService.Config("public", pathPrefix = ""))

  val run = {
    // Combine routes: API under /api, static files at root    
    val httpApp = Router[IO](
      "/api" -> apiRoutes(),
      "/"    -> staticRoutes
    ).orNotFound

    // Wrap the httpApp with CORS middleware
    val corsHttpApp = CORS(httpApp)

    val port = sys.env.get("PORT")
      .flatMap(p => scala.util.Try(p.toInt).toOption)
      .getOrElse(8080)

    EmberServerBuilder
      .default[IO]
      .withHost(Host.fromString("0.0.0.0").get)
      .withPort(Port.fromInt(port).get)
      .withHttpApp(corsHttpApp)
      .build
      .use(_ => IO.println(s"Server running at http://0.0.0.0:$port") >> IO.never)
  }
}
