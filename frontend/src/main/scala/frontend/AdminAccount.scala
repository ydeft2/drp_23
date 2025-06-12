package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object AdminAccount {
  def render(): Unit = {
    Spinner.show()

    fetchClinicDetails().map { clinicName =>
      Layout.renderPage(
        leftButton = Some(createHomeButton()),
        rightButton = None,
        contentRender = () => {
          // Mount into our single #app
          val app = document.getElementById("app")

          // Build a simple container showing the clinic name
          val container = document.createElement("div")
          container.textContent = clinicName

          // Add the delete‐account button underneath
          val deleteBtn = buildDeleteAccountButton()

          app.appendChild(container)
          app.appendChild(deleteBtn)

          Spinner.hide()
        }
      )
    }
  }
}
