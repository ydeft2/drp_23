package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import scala.scalajs.js

object AdminPage {

  def render(): Unit = {
    document.body.innerHTML = ""

    document.body.appendChild(createBlankHeaderWithTitle())

    // Create a grey box container for the admin page
    val container = document.createElement("div")
    container.setAttribute("style", "margin: 50px auto; width: 600px; padding: 20px; background-color: lightgrey; border-radius: 5px;")
    document.body.appendChild(container)

    // Add admin content
    val adminContent = document.createElement("h2")
    adminContent.textContent = "Admin Dashboard"
    container.appendChild(adminContent)

    // Add more admin functionalities here
  }
}
