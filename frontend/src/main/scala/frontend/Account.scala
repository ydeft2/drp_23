package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue



object Account {
  def render(): Unit = {

  Spinner.show()


  fetchUserDetails(dom.window.localStorage.getItem("userId"))
    .map { currentUser =>
      Layout.renderPage(
        leftButton = Some(createHomeButton()), 
        contentRender = () => 
          {
            val card = buildProfileCard(currentUser)
            document.body.appendChild(card)
            document.body.appendChild(buildDeleteAccountButton())
            Spinner.hide()
          }
      )
    }
  }
}
