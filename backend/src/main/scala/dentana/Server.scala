package dentana

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import scala.io.StdIn

object Server extends App {
  implicit val system = ActorSystem("dentana-server")
  implicit val ec     = system.dispatcher

  // serve everything under src/main/resources/web as static files
  val route =
    pathSingleSlash {
      getFromResource("web/index.html")
    } ~
    pathPrefix("web") {
      getFromResourceDirectory("web")
    }

  val binding = Http().newServerAt("0.0.0.0", 8080).bind(route)
  println(">>> Server online at http://localhost:8080/")
  StdIn.readLine(">>> press RETURN to stop")
  binding
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
