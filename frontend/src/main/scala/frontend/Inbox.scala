package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._

object Inbox {
  def render(): Unit = {
    // Clear the body to remove existing content
    document.body.innerHTML = ""

    // Create header
    val header = document.createElement("div").asInstanceOf[Div]
    header.style.backgroundColor = "purple"
    header.style.color = "white"
    header.style.padding = "10px"
    header.style.display = "flex"
    header.style.setProperty("justify-content", "space-between")
    header.style.setProperty("align-items", "center")
    header.style.position = "fixed"
    header.style.top = "0"
    header.style.left = "0"
    header.style.right = "0"
    header.style.height = "50px"
    header.style.zIndex = "1"

    // Home button
    val homeBtn = document.createElement("button").asInstanceOf[Button]
    homeBtn.textContent = "Home"
    homeBtn.style.background = "transparent"
    homeBtn.style.color = "white"
    homeBtn.style.border = "none"
    homeBtn.style.cursor = "pointer"
    homeBtn.style.fontSize = "16px"
    homeBtn.onclick = (_: dom.MouseEvent) => Main.render()

    val title = document.createElement("div")
    title.textContent = "Dentana Notifications"
    title.asInstanceOf[Div].style.fontSize = "20px"
    title.asInstanceOf[Div].style.fontWeight = "bold"
    title.asInstanceOf[Div].style.margin = "0 auto"
    title.asInstanceOf[Div].style.position = "absolute"
    title.asInstanceOf[Div].style.left = "50%"
    title.asInstanceOf[Div].style.transform = "translateX(-50%)"

    header.appendChild(homeBtn)
    header.appendChild(title)
    document.body.appendChild(header)

    // Notifications box
    val notificationsBox = document.createElement("div").asInstanceOf[Div]
    notificationsBox.style.marginTop = "70px"
    notificationsBox.style.marginLeft = "auto"
    notificationsBox.style.marginRight = "auto"
    notificationsBox.style.width = "80%"
    notificationsBox.style.maxHeight = "400px"
    notificationsBox.style.overflowY = "scroll"
    notificationsBox.style.border = "1px solid #ccc"
    notificationsBox.style.borderRadius = "8px"
    notificationsBox.style.padding = "20px"
    notificationsBox.style.boxShadow = "0 2px 8px rgba(0, 0, 0, 0.1)"
    notificationsBox.style.backgroundColor = "#f9f9f9"

    val notificationsTitle = document.createElement("h2")
    notificationsTitle.textContent = "Notifications"
    notificationsBox.appendChild(notificationsTitle)

    val dummyNotifications = List(
      "Your appointment for Dental Cleaning is confirmed at 10:00 AM.",
      "Reminder: Your Checkup is scheduled for 9:15 AM tomorrow.",
      "Alert: Appointment for Root Canal has been rescheduled to 3:00 PM."
    )

    dummyNotifications.foreach { note =>
      val noteEntry = document.createElement("div")
      noteEntry.innerHTML = s"""
         |<p>$note</p>
         |<hr>
         |""".stripMargin
      notificationsBox.appendChild(noteEntry)
    }

    document.body.appendChild(notificationsBox)
  }
}
