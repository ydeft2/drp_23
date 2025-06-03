package frontend

import org.scalajs.dom
import scalajs.concurrent.JSExecutionContext.Implicits.queue


object Main {
  def main(args: Array[String]): Unit = {
    BookingDashboard.render()
    // val tokenOpt = Option(dom.window.localStorage.getItem("accessToken"))
    // tokenOpt match {
    //   case Some(token) =>
    //     println("Token found: " + token)
    //     verifyToken(token).foreach { valid =>
    //       println(s"Token valid? $valid")
    //       if (valid) {
    //         isPatient().map { isPatient =>
    //           if (isPatient) {
    //             HomePage.render()
                
    //           } else {
    //             AdminPage.render()
    //           }
    //         }.recover {
    //           case e: Throwable => dom.window.alert(s"An error occurred: ${e.getMessage}")
    //         }
    //       } else {
    //         println("Invalid token, removing and rendering login")
    //         dom.window.localStorage.removeItem("accessToken")
    //         dom.window.localStorage.removeItem("userId")
    //         LoginPage.render()
            
    //       }
    //     }
    //   case None =>
    //     println("No token found, rendering login")
    //     LoginPage.render()
    // }
  }
}

