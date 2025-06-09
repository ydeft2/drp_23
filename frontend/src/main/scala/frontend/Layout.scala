package frontend

import org.scalajs.dom.{document, html, Element}
import org.scalajs.dom

object Layout {

  def renderHeader(
      leftButton: Option[Element] = None,
      rightButton: Option[Element] = None
  ): Unit = {
    val existingHeader = document.getElementById("header")

    val header = if (existingHeader == null) {
      val newHeader = document.createElement("div").asInstanceOf[html.Div]
      newHeader.id = "header"
      newHeader.setAttribute(
        "style",
        "display: flex; justify-content: space-between; align-items: center; padding: 10px;"
      )
      document.body.insertBefore(newHeader, document.body.firstChild)
      newHeader
    } else {
      existingHeader.asInstanceOf[html.Div]
    }

    header.innerHTML = ""

    val leftContainer = document.createElement("div").asInstanceOf[html.Div]
    leftButton.foreach(leftContainer.appendChild)

    val logoContainer = document.createElement("div").asInstanceOf[html.Div]
    logoContainer.setAttribute("style", "display: flex; align-items: center; flex: 1; justify-content: center;")

    val logo = document.createElement("img").asInstanceOf[html.Image]
    logo.src = "images/DentanaTitle.png"
    logo.alt = "Dentana Logo"
    logo.setAttribute("style", "height: 40px;")
    logo.style.cursor = "pointer"
    logo.id = "header-logo"
    logo.addEventListener("click", (_: dom.MouseEvent) => {
      dom.window.location.reload()
    })
    logoContainer.appendChild(logo)

    val rightContainer = document.createElement("div").asInstanceOf[html.Div]
    rightButton.foreach(rightContainer.appendChild)

    header.appendChild(leftContainer)
    header.appendChild(logoContainer)
    header.appendChild(rightContainer)
  }



  def renderFooter(): Unit = {
    val existingFooter = document.getElementById("footer")
    if (existingFooter == null) {
      val footer = document.createElement("footer")
      footer.id = "footer"
      footer.setAttribute(
        "style",
        "position: fixed; bottom: 0; left: 0; width: 100%; background-color: #333; color: white; " +
        "text-align: center; padding: 10px 0; font-size: 14px; z-index: 1000;"
      )
      footer.textContent = "© 2025 DRP Group 23. All rights reserved."
      document.body.appendChild(footer)
    }
  }

  def clearMainContent(): Unit = {
    val children = document.body.children
    val toRemove = (0 until children.length).map(children.apply).filter { el =>
      val id = el.id
      id != "header" && id != "footer"
    }
    toRemove.foreach(document.body.removeChild)
  }

  def renderPage(
      leftButton: Option[Element] = None,
      rightButton: Option[Element] = None,
      contentRender: () => Unit,
  ): Unit = {
    renderHeader(leftButton, rightButton)
    renderFooter()
    clearMainContent()
    contentRender()
  }
}
