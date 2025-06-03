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
import backend.database._
import backend.http.HttpApi

object Server extends IOApp.Simple {

  override def run = {

    val corsHttpApp = CORS(HttpApi().endpoints.orNotFound)

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
