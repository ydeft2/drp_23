package frontend

import org.scalajs.dom.{document, html, Element}

object Layout {

  def renderHeader(
      leftButton: Option[Element] = None,
      rightButton: Option[Element] = None
  ): Unit = {
    val existingHeader = document.getElementById("header")
    if (existingHeader == null) {
      val header = document.createElement("div").asInstanceOf[html.Div]
      header.id = "header"
      header.setAttribute("style", "display: flex; justify-content: space-between; align-items: center; padding: 10px;")

      val leftContainer = document.createElement("div").asInstanceOf[html.Div]
      leftButton.foreach(leftContainer.appendChild)

      val logoContainer = document.createElement("div").asInstanceOf[html.Div]
      logoContainer.setAttribute("style", "display: flex; align-items: center; flex: 1;")

      val logo = document.createElement("img").asInstanceOf[html.Image]
      logo.src = "images/DentanaTitle.png"
      logo.alt = "Dentana Logo"
      logo.setAttribute("style", "height: 40px;")
      logo.id = "header-logo"

      logoContainer.appendChild(logo)

      val rightContainer = document.createElement("div").asInstanceOf[html.Div]
      rightButton.foreach(rightContainer.appendChild)

      header.appendChild(leftContainer)
      header.appendChild(logoContainer)
      header.appendChild(rightContainer)

      document.body.insertBefore(header, document.body.firstChild)
    }
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
