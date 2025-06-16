package backend

import cats.effect._
import cats.implicits.*

import org.http4s.dsl.io._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.server.Router
import org.http4s.server.staticcontent._
import org.http4s.server.middleware.CORS
import com.comcast.ip4s.{Host, Port}
import org.http4s.ember.server.EmberServerBuilder

import backend.database._
import backend.http.HttpApi
import fs2.concurrent.Topic
import backend.domain.messages.Message

object Server extends IOApp.Simple {

override def run: IO[Unit] =
  // Create a broadcast topic for chat messages
  Topic[IO, Message].flatMap { messageTopic =>

    // Build all HTTP routes, including chat
    val apiRoutes = HttpApi(messageTopic).endpoints
    val msgRoutes = backend.http.routes.MessageRoutes(messageTopic).routes

    // Combine and wrap in CORS
    val combinedApp = Router[IO](
      "/"         -> apiRoutes,
      "/messages" -> msgRoutes
    ).orNotFound

    val corsHttpApp = CORS(combinedApp)

    // Determine port and start server
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
