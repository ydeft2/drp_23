package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSON



object HomePage {
  case class Booking(name: String, time: String, location: String)

  private var bookings: List[Booking] = List(
    Booking("Dental Test", "10:00 AM", "Downtown Clinic"),
    Booking("Root Canal", "2:30 PM", "East Side Dental"),
    Booking("Checkup", "9:15 AM", "Northview Dental Office")
  )

  private var unreadNotifications: Int = 0

  // def main(args: Array[String]): Unit = render()

  def render(): Unit = {
    Spinner.show()
    fetchUnreadCount().foreach { unreadCount =>
      unreadNotifications = unreadCount
      val accountBtn = createHeaderButton("Account")
      accountBtn.addEventListener("click", (_: dom.MouseEvent) => Account.render())

      val inboxLabel = if (unreadNotifications > 0) s"Inbox ($unreadNotifications)" else "Inbox"
      val inboxBtn = createHeaderButton(inboxLabel)
      inboxBtn.addEventListener("click", (_: dom.MouseEvent) => Inbox.render())

      Layout.renderPage(
        leftButton = Some(accountBtn),
        rightButton = Some(inboxBtn),
        contentRender = () =>
        {
          document.body.appendChild(buildBookingsBox())
          document.body.appendChild(createBookingButton())
          Spinner.hide()
        }
      )
    }
  }

  private def buildBookingsBox(): Div = {
    val outerBox = document.createElement("div").asInstanceOf[Div]
    outerBox.style.marginTop = "20px"
    outerBox.style.marginLeft = "auto"
    outerBox.style.marginRight = "auto"
    outerBox.style.width = "80%"
    outerBox.style.border = "1px solid #ccc"
    outerBox.style.borderRadius = "8px"
    outerBox.style.boxShadow = "0 2px 8px rgba(0, 0, 0, 0.1)"
    outerBox.style.backgroundColor = "#f9f9f9"
    outerBox.style.overflow = "hidden"

    val title = document.createElement("div").asInstanceOf[Div]
    title.textContent = "Your bookings"
    title.style.fontSize = "1.5em"
    title.style.fontWeight = "bold"
    title.style.padding = "20px"
    title.style.borderBottom = "1px solid #ddd"
    title.style.backgroundColor = "#f9f9f9"
    title.style.position = "sticky"
    title.style.top = "0"
    title.style.zIndex = "1"

    val scrollArea = document.createElement("div").asInstanceOf[Div]
    scrollArea.style.maxHeight = "400px"
    scrollArea.style.overflowY = "auto"
    scrollArea.style.padding = "20px"

    bookings.foreach(b => scrollArea.appendChild(buildBookingEntry(b)))

    outerBox.appendChild(title)
    outerBox.appendChild(scrollArea)

    outerBox
  }


  private def buildBookingEntry(booking: Booking): Div = {
    val entry = document.createElement("div").asInstanceOf[Div]

    entry.style.backgroundColor = "#ffffff"
    entry.style.borderRadius = "12px"
    entry.style.boxShadow = "0 4px 12px rgba(0, 0, 0, 0.1)"
    entry.style.marginBottom = "16px"
    entry.style.padding = "16px"
    entry.style.transition = "transform 0.2s ease, box-shadow 0.2s ease"
    entry.style.cursor = "pointer"
    entry.style.backgroundImage = "linear-gradient(135deg, #ffffff 0%, #f7f7f7 100%)"

    entry.addEventListener("mouseover", (_: dom.MouseEvent) => {
      entry.style.transform = "translateY(-2px)"
      entry.style.boxShadow = "0 6px 16px rgba(0, 0, 0, 0.15)"
    })
    entry.addEventListener("mouseout", (_: dom.MouseEvent) => {
      entry.style.transform = "translateY(0)"
      entry.style.boxShadow = "0 4px 12px rgba(0, 0, 0, 0.1)"
    })

    entry.innerHTML =
      s"""
        <strong style="font-size: 1.2em;">${booking.name}</strong><br>
        <span><strong>Time:</strong> ${booking.time}</span><br>
        <span><strong>Location:</strong> ${booking.location}</span>
      """

    entry.addEventListener("click", (_: dom.MouseEvent) => {
      showModal(renderBookingDetails(booking))
      addCancelBookingButton(booking)
    })

    entry
  }

  def renderBookingDetails(booking: Booking): String = {
    s"""
      <h3>${booking.name}</h3>
      <p><strong>Time:</strong> ${booking.time}</p>
      <p><strong>Location:</strong> ${booking.location}</p>
      <p><strong>Details:</strong> This is a detailed description of the booking.</p>
      <div id="cancel-booking-btn-container"></div>
    """
  }

  private def createBookingButton(): Div = {
    val button = document.createElement("div").asInstanceOf[Div]
    button.textContent = "Create Booking"

    // Base styles
    button.style.position = "fixed"
    button.style.left = "50%"
    button.style.bottom = "80px"
    button.style.transform = "translate(-50%, 0)" // Ensure horizontal center remains fixed
    button.style.backgroundImage = "linear-gradient(135deg, #7b2ff7, #f107a3)"
    button.style.color = "white"
    button.style.padding = "24px 48px" // slightly larger
    button.style.fontSize = "1.2em"
    button.style.fontWeight = "bold"
    button.style.borderRadius = "60px"
    button.style.cursor = "pointer"
    button.style.boxShadow = "0 6px 14px rgba(0, 0, 0, 0.2)"
    button.style.transition = "transform 0.2s ease, box-shadow 0.2s ease, background-position 0.5s ease"
    button.style.backgroundSize = "200% 200%"
    button.style.backgroundRepeat = "no-repeat"
    button.style.setProperty("will-change", "transform") // Hint browser for performance

    // Prevent "hopping" by preserving horizontal transform
    button.addEventListener("mouseover", (_: dom.MouseEvent) => {
      button.style.transform = "translate(-50%, -4px)" // move up, stay centered
      button.style.boxShadow = "0 8px 18px rgba(0, 0, 0, 0.25)"
    })

    button.addEventListener("mouseout", (_: dom.MouseEvent) => {
      button.style.transform = "translate(-50%, 0)" // reset to original position
      button.style.boxShadow = "0 6px 14px rgba(0, 0, 0, 0.2)"
    })

    // Click handler
    button.addEventListener("click", (_: dom.MouseEvent) => {
      BookingPage.render()
    })

    button
  }

  // Add this after showing the modal, to inject the button and logic
  def addCancelBookingButton(booking: Booking): Unit = {
    val container = dom.document.getElementById("cancel-booking-btn-container")
    if (container != null) {
      val button = dom.document.createElement("button").asInstanceOf[dom.html.Button]
      button.textContent = "Cancel Booking"
      button.style.backgroundColor = "#e74c3c"
      button.style.color = "white"
      button.style.padding = "10px 20px"
      button.style.border = "none"
      button.style.borderRadius = "5px"
      button.style.cursor = "pointer"

      var confirmState = false

      button.onclick = (_: dom.MouseEvent) => {
        if (!confirmState) {
          button.textContent = "Confirm Cancellation"
          button.style.backgroundColor = "#c0392b"
          confirmState = true
        } else {
          cancelBooking(booking)
        }
      }

      container.appendChild(button)
    }
  }

  def cancelBooking(booking: Booking): Unit = {
    
    replaceModalContent(
      """
      <h3>Cancellation Successful</h3>
      <p>Your booking has been successfully cancelled.</p>
      <img src="images/Sad.png" alt="Sad Icon" style="width: 50px; height: 50px;">
      """
    )

    // val bookingId = "12345678-1234-1234-1234-123456789012"

    // val req = new dom.Request(
    //   s"/api/bookings/cancel/$bookingId",
    //   new dom.RequestInit {
    //     method = "DELETE"
    //     headers = new dom.Headers {
    //       append("Content-Type", "application/json")
    //       val accessToken = dom.window.localStorage.getItem("accessToken")
    //       if (accessToken != null) {
    //         append("Authorization", s"Bearer $accessToken")
    //       }
    //     }
    //   }
    // )
  }
}

