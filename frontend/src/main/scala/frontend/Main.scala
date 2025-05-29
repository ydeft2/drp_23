package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._

object Main {
  case class Booking(name: String, time: String, location: String)

  private var bookings: List[Booking] = List(
    Booking("Dental Cleaning", "10:00 AM", "Downtown Clinic"),
    Booking("Root Canal", "2:30 PM", "East Side Dental"),
    Booking("Checkup", "9:15 AM", "Northview Dental Office")
  )

  def main(args: Array[String]): Unit = render()

  def render(): Unit = {
    document.body.innerHTML = ""
    document.body.appendChild(buildHeader())
    document.body.appendChild(buildBookingsBox())
    document.body.appendChild(createBookingButton())
  }

  private def buildHeader(): Div = {
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

    val accountBtn = createHeaderButton("Account")
    accountBtn.addEventListener("click", (_: dom.MouseEvent) => Account.render())

    val inboxBtn = createHeaderButton("Inbox")
    inboxBtn.addEventListener("click", (_: dom.MouseEvent) => Inbox.render())

    val title = document.createElement("div").asInstanceOf[Div]
    title.textContent = "Dentana"
    title.style.fontSize = "20px"
    title.style.fontWeight = "bold"
    title.style.margin = "0 auto"
    title.style.position = "absolute"
    title.style.left = "50%"
    title.style.transform = "translateX(-50%)"

    header.appendChild(accountBtn)
    header.appendChild(title)
    header.appendChild(inboxBtn)
    header
  }

  private def buildBookingsBox(): Div = {
    val box = document.createElement("div").asInstanceOf[Div]
    box.style.marginTop = "70px"
    box.style.marginLeft = "auto"
    box.style.marginRight = "auto"
    box.style.width = "80%"
    box.style.maxHeight = "400px"
    box.style.overflowY = "scroll"
    box.style.border = "1px solid #ccc"
    box.style.borderRadius = "8px"
    box.style.padding = "20px"
    box.style.boxShadow = "0 2px 8px rgba(0, 0, 0, 0.1)"
    box.style.backgroundColor = "#f9f9f9"

    val title = document.createElement("h2")
    title.textContent = "Your bookings"
    box.appendChild(title)

    bookings.foreach(b => box.appendChild(buildBookingEntry(b)))
    box
  }

  private def buildBookingEntry(booking: Booking): Div = {
    val entry = document.createElement("div").asInstanceOf[Div]
    entry.innerHTML = s"<strong>${booking.name}</strong><br>Time: ${booking.time}<br>Location: ${booking.location}"
    entry.style.cursor = "pointer"
    entry.addEventListener("click", (_: dom.MouseEvent) => renderBookingDetails(booking))
    val hr = document.createElement("hr")
    entry.appendChild(hr)
    entry
  }

  private def renderBookingDetails(booking: Booking): Unit = {
    document.body.innerHTML = ""
    document.body.appendChild(buildHeader())

    val container = document.createElement("div").asInstanceOf[Div]
    container.style.marginTop = "70px"
    container.style.marginLeft = "auto"
    container.style.marginRight = "auto"
    container.style.width = "80%"
    container.style.padding = "20px"
    container.style.border = "1px solid #ccc"
    container.style.borderRadius = "8px"
    container.style.backgroundColor = "#f9f9f9"
    container.style.boxShadow = "0 2px 8px rgba(0,0,0,0.1)"

    val title = document.createElement("h2")
    title.textContent = booking.name
    container.appendChild(title)

    val timeP = document.createElement("p")
    timeP.textContent = s"Time: ${booking.time}"
    container.appendChild(timeP)

    val locP = document.createElement("p")
    locP.textContent = s"Location: ${booking.location}"
    container.appendChild(locP)

    val cancelBtn = document.createElement("button").asInstanceOf[Button]
    cancelBtn.textContent = "Cancel Booking"
    cancelBtn.style.backgroundColor = "red"
    cancelBtn.style.color = "white"
    cancelBtn.style.border = "none"
    cancelBtn.style.padding = "10px 20px"
    cancelBtn.style.cursor = "pointer"
    cancelBtn.style.borderRadius = "4px"
    cancelBtn.addEventListener("click", (_: dom.MouseEvent) => {
      bookings = bookings.filterNot(_ == booking)
      render()
    })
    container.appendChild(cancelBtn)

    val backBtn = document.createElement("button").asInstanceOf[Button]
    backBtn.textContent = "Back"
    backBtn.style.marginLeft = "10px"
    backBtn.style.padding = "10px 20px"
    backBtn.style.cursor = "pointer"
    backBtn.style.border = "1px solid #333"
    backBtn.style.background = "transparent"
    backBtn.addEventListener("click", (_: dom.MouseEvent) => render())
    container.appendChild(backBtn)

    document.body.appendChild(container)
  }

  private def createBookingButton(): Div = {
    val button = document.createElement("div").asInstanceOf[Div]
    button.textContent = "Create Booking"
    button.style.position = "fixed"
    button.style.left = "50%"
    button.style.bottom = "80px"
    button.style.transform = "translateX(-50%)"
    button.style.backgroundColor = "purple"
    button.style.color = "white"
    button.style.padding = "20px 40px"
    button.style.borderRadius = "50px"
    button.style.cursor = "pointer"
    button.style.boxShadow = "0 2px 8px rgba(0, 0, 0, 0.2)"
    button.addEventListener("click", (_: dom.MouseEvent) => 
      System.out.println("Create Booking button clicked")
      BookingPage.render())

    button
  }
}

