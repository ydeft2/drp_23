package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import frontend.Inbox

object Main {
  def main(args: Array[String]): Unit = render()

  def render(): Unit = {
    // Clear existing page content before rendering
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


    val accountBtn = document.createElement("button").asInstanceOf[Button]
    accountBtn.textContent = "Account"
    accountBtn.style.background = "transparent"
    accountBtn.style.color = "white"
    accountBtn.style.border = "none"
    accountBtn.style.cursor = "pointer"
    accountBtn.style.fontSize = "16px"

    val inboxBtn = document.createElement("button").asInstanceOf[Button]
    inboxBtn.textContent = "Inbox"
    inboxBtn.style.background = "transparent"
    inboxBtn.style.color = "white"
    inboxBtn.style.border = "none"
    inboxBtn.style.cursor = "pointer"
    inboxBtn.style.fontSize = "16px"
    inboxBtn.addEventListener("click", (_: dom.MouseEvent) => Inbox.render())

    val title = document.createElement("div")
    title.textContent = "Dentana"
    title.asInstanceOf[Div].style.fontSize = "20px"
    title.asInstanceOf[Div].style.fontWeight = "bold"
    title.asInstanceOf[Div].style.margin = "0 auto"
    title.asInstanceOf[Div].style.position = "absolute"
    title.asInstanceOf[Div].style.left = "50%"
    title.asInstanceOf[Div].style.transform = "translateX(-50%)"

    header.appendChild(accountBtn)
    header.appendChild(title)
    header.appendChild(inboxBtn)
    document.body.appendChild(header)

    // Create scroll box for bookings
    val bookingsBox = document.createElement("div").asInstanceOf[Div]
    bookingsBox.style.marginTop = "70px"
    bookingsBox.style.marginLeft = "auto"
    bookingsBox.style.marginRight = "auto"
    bookingsBox.style.width = "80%"
    bookingsBox.style.maxHeight = "400px"
    bookingsBox.style.overflowY = "scroll"
    bookingsBox.style.border = "1px solid #ccc"
    bookingsBox.style.borderRadius = "8px"
    bookingsBox.style.padding = "20px"
    bookingsBox.style.boxShadow = "0 2px 8px rgba(0, 0, 0, 0.1)"
    bookingsBox.style.backgroundColor = "#f9f9f9"

    val bookingsTitle = document.createElement("h2")
    bookingsTitle.textContent = "Your bookings"
    bookingsBox.appendChild(bookingsTitle)

    val mockBookings = List(
      ("Dental Cleaning", "10:00 AM", "Downtown Clinic"),
      ("Root Canal", "2:30 PM", "East Side Dental"),
      ("Checkup", "9:15 AM", "Northview Dental Office")
    )

    mockBookings.foreach { case (name, time, location) =>
      val bookingEntry = document.createElement("div")
      bookingEntry.innerHTML =
        s"""
           |<strong>$name</strong><br>
           |Time: $time<br>
           |Location: $location
           |<hr>
           |""".stripMargin
      bookingsBox.appendChild(bookingEntry)
    }

    document.body.appendChild(bookingsBox)
  }
}
