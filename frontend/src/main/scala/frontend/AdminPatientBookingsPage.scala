package frontend

import org.scalajs.dom
import org.scalajs.dom.Element
import org.scalajs.dom.document
import scala.concurrent.Future
import scala.scalajs.js
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.Thenable.Implicits._


object AdminPatientBookingsPage {

  def render(): Unit = {
    Spinner.show()
    fetchBookingsForAdmin()
      .map { bookings =>
        Layout.renderPage(
          leftButton = Some(createHomeButton()),
          contentRender = () => {
            val container = document.createElement("div")
            container.appendChild(renderBookings(bookings))
            document.body.appendChild(container)
          }
        )
        Spinner.hide()
      }
  }

  def fetchBookingsForAdmin(): Future[js.Array[js.Dynamic]] = {
    val accessToken = dom.window.localStorage.getItem("accessToken")
    val uid = dom.window.localStorage.getItem("userId")

    if (accessToken == null || uid == null) {
      dom.window.alert("You are not logged in.")
      return Future.successful(js.Array[js.Dynamic]())
    }

    val requestHeaders = new dom.Headers()
    requestHeaders.append("Content-Type", "application/json")
    requestHeaders.append("Authorization", s"Bearer $accessToken")
    requestHeaders.append("apikey", SUPABASE_ANON_KEY)

    val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.GET
      headers = requestHeaders
    }

    dom.fetch(s"/api/bookings/list?clinic_id=$uid", requestInit)
      .flatMap { response =>
        if (response.ok) {
          response.json().toFuture.map { data =>
            data.asInstanceOf[js.Array[js.Dynamic]]
          }
        } else {
          dom.window.alert("Failed to fetch bookings.")
          Future.failed(new Exception("Failed to fetch bookings"))
        }
      }
  }

  def renderBookings(bookings: js.Array[js.Dynamic]): Element = {
    val container = document.createElement("div")

    // Sort bookings by slot_time ascending
    val sorted = bookings.sortBy(b => new js.Date(b.slot_time.asInstanceOf[String]).getTime())

    sorted.foreach { booking =>
      val patientId = booking.patient_id.asInstanceOf[String]
      val slotTime = booking.slot_time.asInstanceOf[String]
      val slotLength = booking.slot_length.asInstanceOf[Int]

      // Create a div for this booking
      val bookingDiv = document.createElement("div")
      bookingDiv.setAttribute("style", "margin-bottom: 1em; padding: 0.5em; border: 1px solid #ccc;")

      // Placeholder text while loading user info
      bookingDiv.textContent =
        s"Patient: Loading... | Time: ${formatSlotTime(slotTime)} | Length: $slotLength min"

      // Fetch and update with real patient info
      fetchUserDetails(patientId).foreach { user =>
        bookingDiv.textContent =
          s"Patient: ${user.name} (DOB: ${user.dob}) | Time: ${formatSlotTime(slotTime)} | Length: $slotLength min"
      }

      container.appendChild(bookingDiv)
    }

    container
  }
}