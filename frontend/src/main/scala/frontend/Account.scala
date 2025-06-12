package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue



object Account {
  def render(): Unit = {
    Spinner.show()

    // fetchUserDetails returns a Future[User]
    fetchUserDetails(dom.window.localStorage.getItem("userId")).map { currentUser =>
      Layout.renderPage(
        leftButton    = Some(createHomeButton()),
        rightButton   = None,
        contentRender = () => {
          // grab our single mount point
          val app = document.getElementById("app")

          // build the profile card and delete button
          val card   = buildProfileCard(currentUser, isPatient = true)
          val delete = buildDeleteAccountButton()

          // append into #app
          app.appendChild(card)
          app.appendChild(delete)

          Spinner.hide()
        }
      )
    }
  }
}
