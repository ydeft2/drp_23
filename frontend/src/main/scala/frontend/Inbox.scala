package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._

object Inbox {
  def render(): Unit = {
    // Clear the body to remove existing content
    document.body.innerHTML = ""

    // Create header
    val header = createSubpageHeader("Dentana Notifications")
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
