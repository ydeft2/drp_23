package frontend

import org.scalajs.dom.{Element, document, html}
import org.scalajs.dom

import scala.scalajs.js

object Layout {
  private val AppId    = "app"
  private val HeaderId = "header"
  private val FooterId = "footer"

  /** The one‐and‐only entrypoint: clears #app, renders header, page content, then footer. */
  def renderPage(
                  leftButton: Option[html.Element] = None,
                  rightButton: Option[html.Element] = None,
                  contentRender: () => Unit
                ): Unit = {
    // 1) Ensure <div id="app"> exists
    val app: html.Div = document.getElementById(AppId) match {
      case null =>
        val d = document.createElement("div").asInstanceOf[html.Div]
        d.id = AppId
        document.body.appendChild(d)
        d
      case el =>
        el.asInstanceOf[html.Div]
    }

    // 2) Wipe out whatever was inside #app
    app.innerHTML = ""

    // 3) Build header/footer elements (but do NOT append them to body)
    val header = buildHeader(leftButton, rightButton)
    val footer = buildFooter()

    // 4) Stick the header into #app
    app.appendChild(header)

    // 5) Let the page fill the remainder of #app
    contentRender()

    // 6) Finally append the footer
    app.appendChild(footer)
  }

  /** Creates (but does not insert) the header bar. */
  private def buildHeader(
                           leftButton: Option[html.Element],
                           rightButton: Option[html.Element]
                         ): html.Div = {
    val header = document.createElement("div").asInstanceOf[html.Div]
    header.id = HeaderId
    header.style.cssText =
      "display:flex;justify-content:space-between;align-items:center;padding:10px;background:#fff;box-shadow:0 2px 4px rgba(0,0,0,0.1);"

    // left slot
    val left = document.createElement("div").asInstanceOf[html.Div]
    leftButton.foreach { btn =>
      btn.classList.add("sheen-button")
      left.appendChild(btn)
    }

    // center logo
    val center = document.createElement("div").asInstanceOf[html.Div]
    center.style.cssText = "flex:1;display:flex;justify-content:center;align-items:center;"
    val logo = document.createElement("img").asInstanceOf[html.Image]
    logo.src = "images/DentanaTitle.png"
    logo.alt = "Dentana"
    logo.style.height = "40px"
    logo.style.cursor = "pointer"
    logo.onclick = (_: dom.MouseEvent) => dom.window.location.reload()
    center.appendChild(logo)

    // right slot
    val right = document.createElement("div").asInstanceOf[html.Div]
    rightButton.foreach { btn =>
      btn.classList.add("sheen-button")
      right.appendChild(btn)
    }

    // assemble
    header.appendChild(left)
    header.appendChild(center)
    header.appendChild(right)
    header
  }

  /** Creates (but does not insert) the footer bar. */
  private def buildFooter(): html.Element = {
    val footer = document.createElement("footer").asInstanceOf[html.Element]
    footer.id = FooterId
    footer.style.cssText =
      "position:fixed;bottom:0;left:0;width:100%;background:#333;color:#fff;text-align:center;padding:10px 0;font-size:14px;"
    footer.textContent = "© 2025 DRP Group 23. All rights reserved."
    footer
  }
}
