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
    val clinicId = dom.window.localStorage.getItem("userId")
    fetchClinicDetails(clinicId).map { clinic =>
      Layout.renderPage(
        leftButton = Some(createHomeButton()),
        contentRender = () => {
          document.body.appendChild(buildClinicProfileCard(clinic))
          document.body.appendChild(buildDeleteAccountButton())
          Spinner.hide()
        }
      )
    }
  }
}
