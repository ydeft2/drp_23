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
      header.setAttribute(
        "style",
        "background-color: purple; color: white; padding: 10px; position: relative; font-size: 24px; font-weight: bold; text-align: center;"
      )

      // Left container
      val leftContainer = document.createElement("div").asInstanceOf[html.Div]
      leftContainer.setAttribute(
        "style",
        "position: absolute; left: 10px; top: 10px;"
      )
      leftButton.foreach(leftContainer.appendChild)

      // Right container
      val rightContainer = document.createElement("div").asInstanceOf[html.Div]
      rightContainer.setAttribute(
        "style",
        "position: absolute; right: 10px; top: 10px;"
      )
      rightButton.foreach(rightContainer.appendChild)

      leftContainer.className = "header-left"
      rightContainer.className = "header-right"

      val logo = document.createElement("img").asInstanceOf[html.Image]
      logo.src = "images/DentanaTitle.png"
      logo.alt = "Dentana Logo"
      logo.setAttribute("style", "height: 40px;")

      header.appendChild(leftContainer)
      header.appendChild(logo)  
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
      footer.textContent = "© 2025 Dentana. All rights reserved."
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
