package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue



object AdminAccount {

  val logOutButton = document.createElement("button").asInstanceOf[Button]
    logOutButton.textContent = "Log Out"
    styleButton(logOutButton, background = "red", color = "white", border = "none")
    logOutButton.onclick = (_: dom.MouseEvent) => {
      dom.window.localStorage.removeItem("accessToken")
      dom.window.localStorage.removeItem("userId")
      dom.window.location.href = "/"
    }


  def render(): Unit = {
    Spinner.show()
    fetchClinicDetails()
      .map { currentUser =>
        Layout.renderPage(
          leftButton = Some(createHomeButton()),
          contentRender = () => {
            val container = document.createElement("div")
            val textNode = document.createTextNode(currentUser)
            container.appendChild(textNode)
            container.appendChild(logOutButton)
            container.appendChild(buildDeleteAccountButton())
            document.body.appendChild(container)
          }
        )
        Spinner.hide()
      }
  }
}
