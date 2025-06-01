package frontend

import org.scalajs.dom
import scalajs.concurrent.JSExecutionContext.Implicits.queue


object Main {
  def main(args: Array[String]): Unit = {
    val tokenOpt = Option(dom.window.localStorage.getItem("accessToken"))
    tokenOpt match {
      case Some(token) =>
        println("Token found: " + token)
        verifyToken(token).foreach { valid =>
          println(s"Token valid? $valid")
          if (valid) HomePage.render()
          else {
            println("Invalid token, removing and rendering login")
            dom.window.localStorage.removeItem("accessToken")
            dom.window.localStorage.removeItem("userId")
            LoginPage.render()
          }
        }
      case None =>
        println("No token found, rendering login")
        LoginPage.render()
    }
  }
}

