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
  clearPage()

  document.body.appendChild(createSubpageHeader("Dentana Account"))

  Spinner.show()

  fetchUserDetails()
    .map { currentUser =>
      Spinner.hide()
      val card = buildProfileCard(currentUser, true)
      document.body.appendChild(card)
      document.body.appendChild(buildDeleteAccountButton())
    }
  }
}
