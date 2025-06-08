package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
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
    fetchUnreadCount { () =>
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
    entry.addEventListener("click", (_: dom.MouseEvent) => {
        showModal(renderBookingDetails(booking))
        addCancelBookingButton(booking)
      }
    )
    val hr = document.createElement("hr")
    entry.appendChild(hr)
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

  private def fetchUnreadCount(onDone: () => Unit): Unit = {
    val accessToken = dom.window.localStorage.getItem("accessToken")
    val uid = dom.window.localStorage.getItem("userId")

    if (accessToken == null || uid == null) {
      dom.window.alert("You are not logged in.")
      onDone() // still call it to allow partial rendering if needed
      return
    }

    val requestHeaders = new dom.Headers()
    requestHeaders.append("Content-Type", "application/json")
    requestHeaders.append("Authorization", s"Bearer $accessToken")

    val requestBody = js.Dynamic.literal(
      "user_id" -> uid,
      "message" -> ""
    )

    val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.POST
      headers = requestHeaders
      body = JSON.stringify(requestBody)
    }

    dom.fetch("/api/notifications/fetch", requestInit)
      .toFuture
      .flatMap(_.json().toFuture)
      .map { jsValue =>
        if (js.Array.isArray(jsValue)) {
          val arr = jsValue.asInstanceOf[js.Array[js.Dynamic]]
          val parsed = Inbox.parseNotifications(arr)
          unreadNotifications = parsed.count(!_.isRead)
        } else {
          println("Unexpected response format")
        }
      }
      .recover {
        case e => println(s"Failed to fetch unread notifications: ${e.getMessage}")
      }
      .foreach(_ => onDone())
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

