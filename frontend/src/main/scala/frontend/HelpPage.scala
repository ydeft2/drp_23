package frontend

import org.scalajs.dom
import org.scalajs.dom.document

object HelpPage {

  def render(): Unit = {
    Layout.renderPage(
      leftButton = Some(createHomeButton()),
      contentRender = () => {
        val contentDiv = document.createElement("div").asInstanceOf[dom.html.Div]
        contentDiv.className = "help-content"
        contentDiv.style.maxWidth = "600px"
        contentDiv.style.margin = "60px auto"
        contentDiv.style.background = "#fff"
        contentDiv.style.borderRadius = "16px"
        contentDiv.style.boxShadow = "0 4px 24px rgba(0,0,0,0.10)"
        contentDiv.style.padding = "40px 32px"
        contentDiv.style.textAlign = "left"

        contentDiv.innerHTML =
          """
            |<div style="text-align:center;">
            |  <img src="/images/icons/Help.png" alt="Help" style="width:64px;height:64px;margin-bottom:16px;opacity:0.85;">
            |</div>
            |<h1 style="text-align:center; margin-bottom: 18px; color: #4a4a8a;">Help &amp; Support</h1>
            |<p style="font-size:1.15rem; color:#444; text-align:center;">
            |  Welcome! Here you can find answers to common questions and get assistance with using our platform.
            |</p>
            |<hr style="margin: 28px 0;">
            |<h2 style="color:#4a4a8a; font-size:1.2rem;">What are each of the pages for?</h2>
            |<ul style="list-style:none; padding:0; font-size:1.07rem;">
            | <li style="margin-bottom:10px;"><span style="font-weight:bold; color:#7f53ac;">My Account</span> – Manage your clinic's important information.</li>
            | <li style="margin-bottom:10px;"><span style="font-weight:bold; color:#7f53ac;">Inbox</span> – View and manage your system notifications.</li>
            | <li style="margin-bottom:10px;"><span style="font-weight:bold; color:#7f53ac;">Patient Bookings</span> – View and manage bookings your patients have created.</li>
            | <li style="margin-bottom:10px;"><span style="font-weight:bold; color:#7f53ac;">Set Availability</span> – Set when you are able to see patients.</li>
            | <li style="margin-bottom:10px;"><span style="font-weight:bold; color:#7f53ac;">Messages</span> – Send and receive messages with patients.</li>
            |</ul>
            |<hr style="margin: 28px 0;">
            |<h2 style="color:#4a4a8a; font-size:1.2rem;">Need more help?</h2>
            |<p style="font-size:1.07rem; color:#444;">
            |  If you need further assistancee please email <a href="mailto:support@dentana.com" style="color:#4a4a8a;">support@dentana.com</a>.
            |</p>
          """.stripMargin

        document.body.appendChild(contentDiv)
      }
    )
  }
}