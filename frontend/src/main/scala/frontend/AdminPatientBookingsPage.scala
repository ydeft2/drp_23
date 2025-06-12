package frontend

import org.scalajs.dom
import org.scalajs.dom.Element
import org.scalajs.dom.document
import org.scalajs.dom.html.Div

import scala.concurrent.Future
import scala.scalajs.js
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.Thenable.Implicits.*
import scala.scalajs.js.JSON // <-- Add this import at the top


object AdminPatientBookingsPage {

  def render(): Unit = {
    Spinner.show()

    // Fetch all bookings for this clinic/admin
    fetchBookingsForAdmin().foreach { bookings =>
      // Prepare header buttons
      val homeBtn  = createHomeButton()
      val inboxBtn = createHeaderButton("Inbox")
      inboxBtn.onclick = (_: dom.MouseEvent) => Inbox.render()

      Layout.renderPage(
        leftButton    = Some(homeBtn),
        rightButton   = Some(inboxBtn),
        contentRender = () => {
          // Container for the booking list
          val container = document.createElement("div").asInstanceOf[Div]
          container.appendChild(renderBookings(bookings))

          // Append into the <div id="app">
          document.getElementById("app").appendChild(container)

          Spinner.hide()
        }
      )
    }
  }

  private def fetchBookingsForAdmin(): scala.concurrent.Future[js.Array[js.Dynamic]] = {
    val accessToken = dom.window.localStorage.getItem("accessToken")
    val uid         = dom.window.localStorage.getItem("userId")

    if (accessToken == null || uid == null) {
      dom.window.alert("You are not logged in.")
      return scala.concurrent.Future.successful(js.Array[js.Dynamic]())
    }

    val reqheaders = new dom.Headers()
    reqheaders.append("Content-Type", "application/json")
    reqheaders.append("Authorization", s"Bearer $accessToken")
    reqheaders.append("apikey", SUPABASE_ANON_KEY)

    val init = new dom.RequestInit {
      method  = dom.HttpMethod.GET
      headers = reqheaders
    }

    dom.fetch(s"/api/bookings/list?clinic_id=$uid", init)
      .toFuture
      .flatMap { resp =>
        if (resp.ok) resp.json().toFuture.map(_.asInstanceOf[js.Array[js.Dynamic]])
        else {
          dom.window.alert("Failed to fetch bookings.")
          scala.concurrent.Future.failed(new Exception("Fetch error"))
        }
      }
  }

  private def infoRow(label: String, value: String): Div = {
    val row = document.createElement("div").asInstanceOf[Div]
    row.style.marginBottom = "8px"
    val strong = document.createElement("strong")
    strong.textContent = label + " "
    row.appendChild(strong)
    row.appendChild(document.createTextNode(value))
    row
  }

  private def renderBookings(bookings: js.Array[js.Dynamic]) = {
    val container = document.createElement("div")

    // Sort by slot_time ascending
    val sorted = bookings.sortBy(b => new js.Date(b.slot_time.asInstanceOf[String]).getTime())

    sorted.foreach { b =>
      val bookingId       = b.booking_id.asInstanceOf[String]
      val patientId       = b.patient_id.asInstanceOf[String]
      val slotTime        = b.slot_time.asInstanceOf[String]
      val slotLength      = b.slot_length.asInstanceOf[Int]
      val appointmentType = b.appointment_type.asInstanceOf[String]
      val clinicInfo      = Option(b.clinic_info).map(_.toString).getOrElse("")
      val confirmed       = b.confirmed.asInstanceOf[Boolean]

      val box = document.createElement("div").asInstanceOf[Div]
      box.className = "booking-item"
      // initial placeholder
      box.textContent = s"Loading…"

      // fetch patient details to replace placeholder
      fetchUserDetails(patientId).foreach { user =>
        box.innerHTML =
          s"""
             |<div>
             |  <strong>${user.name}</strong> (DOB: ${user.dob})<br/>
             |  ${formatSlotTime(slotTime)} • $slotLength min<br/>
             |  Status: ${if (confirmed) "✓ Confirmed" else "⏳ Pending"}
             |</div>
           """.stripMargin

        box.onclick = (_: dom.MouseEvent) => showModal(createBookingModal(
          bookingId,
          user.name,
          user.dob,
          slotTime,
          slotLength,
          clinicInfo,
          appointmentType,
          confirmed
        ))
      }

      container.appendChild(box)
    }

    container
  }

  // Helper to create the modal for a booking
  private def createBookingModal(
    bookingId: String,
    patientName: String,
    patientDob: String,
    slotTime: String,
    slotLength: Int,
    clinicInfo: String,
    appointmentType: String,
    confirmed: Boolean
  ): dom.html.Div = {
    val modalDiv = document.createElement("div").asInstanceOf[dom.html.Div]
    modalDiv.style.display = "flex"
    modalDiv.style.setProperty("flex-direction", "column")
    modalDiv.style.height = "100%"

    // Title
    val title = document.createElement("h2")
    title.textContent = "Booking Details"
    modalDiv.appendChild(title)

    // Status indicator
    val statusDiv = document.createElement("div").asInstanceOf[dom.html.Div]
    statusDiv.style.marginBottom = "16px"
    statusDiv.style.padding = "8px 12px"
    statusDiv.style.borderRadius = "4px"
    statusDiv.style.fontWeight = "bold"
    if (confirmed) {
      statusDiv.textContent = "✓ CONFIRMED"
      statusDiv.style.backgroundColor = "#d4edda"
      statusDiv.style.color = "#155724"
      statusDiv.style.border = "1px solid #c3e6cb"
    } else {
      statusDiv.textContent = "⏳ PENDING CONFIRMATION"
      statusDiv.style.backgroundColor = "#fff3cd"
      statusDiv.style.color = "#856404"
      statusDiv.style.border = "1px solid #ffeaa7"
    }
    modalDiv.appendChild(statusDiv)

    // Info rows
    modalDiv.appendChild(infoRow("Booking ID:", bookingId))
    modalDiv.appendChild(infoRow("Patient Name:", patientName))
    modalDiv.appendChild(infoRow("DOB:", patientDob))
    modalDiv.appendChild(infoRow("Time:", formatSlotTime(slotTime)))
    modalDiv.appendChild(infoRow("Length:", s"$slotLength min"))

    // Only show editable fields if booking is not confirmed
    // Declare these outside the if so they're visible in the handler
    var clinicBox: dom.html.TextArea = null
    var typeSelect: dom.html.Select = null

    if (!confirmed) {
      // Clinic info textarea
      val clinicLabel = document.createElement("label")
      clinicLabel.textContent = "Clinic Info:"
      clinicLabel.setAttribute("for", "clinic-info-box")
      modalDiv.appendChild(clinicLabel)
      modalDiv.appendChild(document.createElement("br"))
      clinicBox = document.createElement("textarea").asInstanceOf[dom.html.TextArea]
      clinicBox.id = "clinic-info-box"
      clinicBox.value = clinicInfo
      clinicBox.style.width = "100%"
      clinicBox.style.minHeight = "80px"
      clinicBox.style.marginBottom = "12px"
      modalDiv.appendChild(clinicBox)

      // Appointment type dropdown
      val typeLabel = document.createElement("label")
      typeLabel.textContent = "Appointment Type:"
      typeLabel.setAttribute("for", "appointment-type-select")
      modalDiv.appendChild(typeLabel)
      modalDiv.appendChild(document.createElement("br"))
      typeSelect = document.createElement("select").asInstanceOf[dom.html.Select]
      typeSelect.id = "appointment-type-select"
      typeSelect.style.width = "100%"
      typeSelect.style.padding = "8px"
      typeSelect.style.fontSize = "16px"
      typeSelect.style.marginBottom = "16px"
      val types = Seq("CHECKUP", "EXTRACTION", "FILLING", "ROOT_CANAL", "HYGIENE", "OTHER", "NOT_SET")
      types.foreach { t =>
        val opt = document.createElement("option").asInstanceOf[dom.html.Option]
        opt.value = t
        opt.textContent = t.capitalize
        if (t == appointmentType) opt.selected = true
        typeSelect.appendChild(opt)
      }
      modalDiv.appendChild(typeSelect)
    } else {
      // For confirmed bookings, show clinic info and appointment type as read-only
      modalDiv.appendChild(infoRow("Clinic Info:", if (clinicInfo.nonEmpty) clinicInfo else "Not specified"))
      modalDiv.appendChild(infoRow("Appointment Type:", appointmentType.capitalize))
    }

    // Spacer to push the buttons to the bottom
    val spacer = document.createElement("div").asInstanceOf[dom.html.Div]
    spacer.style.setProperty("flex", "1")
    modalDiv.appendChild(spacer)

    // Show confirm button only for unconfirmed bookings
    if (!confirmed) {
      val confirmBtn = document.createElement("button").asInstanceOf[dom.html.Button]
      confirmBtn.textContent = "Confirm Booking"
      confirmBtn.style.background = "green"
      confirmBtn.style.color = "white"
      confirmBtn.style.padding = "10px 20px"
      confirmBtn.style.border = "none"
      confirmBtn.style.borderRadius = "4px"
      confirmBtn.style.fontSize = "16px"
      confirmBtn.style.marginBottom = "12px"
      confirmBtn.style.cursor = "pointer"
      
      // TODO: Add click handler for confirming booking
      confirmBtn.onclick = (_: dom.MouseEvent) => {
        val updatedClinicInfo = clinicBox.value
        val updatedAppointmentType = typeSelect.value

        // Call backend API to confirm booking
        confirmBooking(bookingId, updatedClinicInfo, updatedAppointmentType).foreach { success =>
          if (success) {
            replaceModalContent(
              s"""
              <h3>Booking Confirmed</h3>
              <p>Booking has been successfully confirmed.</p>
              <img src="images/Confirmation.png" alt="Confirmation Icon" style="width: 80px;;">
              """
            )
          } else {
            dom.window.alert("Failed to confirm booking.")
          }
        }
      }
      
      modalDiv.appendChild(confirmBtn)
    }

    // Cancel button (always present)
    val cancelBtn = document.createElement("button").asInstanceOf[dom.html.Button]
    cancelBtn.textContent = "Cancel Booking"
    cancelBtn.style.background = "red"
    cancelBtn.style.color = "white"
    cancelBtn.style.padding = "10px 20px"
    cancelBtn.style.border = "none"
    cancelBtn.style.borderRadius = "4px"
    cancelBtn.style.fontSize = "16px"
    cancelBtn.style.marginBottom = "16px"
    cancelBtn.style.cursor = "pointer"
    
    // TODO: Add click handler for canceling booking
    cancelBtn.onclick = (_: dom.MouseEvent) => {
      cancelBooking(bookingId).foreach { success =>
        if (success) {
          replaceModalContent(
            s"""
            <h3>Booking Cancelled</h3>
            <p>Booking has been successfully cancelled.</p>
            <img src="images/Cancelled.png" alt="Cancelled Icon" style="width: 80px;">
            """
          )
        } else {
          dom.window.alert("Failed to cancel booking.")
        }
      }
    }
    
    modalDiv.appendChild(cancelBtn)

    modalDiv
  }

  def cancelBooking(bookingId: String): Future[Boolean] = {
    val accessToken = dom.window.localStorage.getItem("accessToken")
    if (accessToken == null) {
      dom.window.alert("You are not logged in.")
      return Future.successful(false)
    }

    val requestHeaders = new dom.Headers()
    requestHeaders.append("Content-Type", "application/json")
    requestHeaders.append("Authorization", s"Bearer $accessToken")
    requestHeaders.append("apikey", SUPABASE_ANON_KEY)

    val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.DELETE
      headers = requestHeaders
    }

    dom.fetch(s"/api/bookings/cancel/$bookingId", requestInit)
      .flatMap { response =>
        if (response.ok) {
          response.json().toFuture.map { _ =>
            true // Successfully parsed response
          }.recover {
            case _ => true // Even if JSON parsing fails, cancellation was successful
          }
        } else {
          Future.successful(false)
        }
      }
      .recover {
        case e =>
          dom.window.alert(s"Failed to cancel booking: ${e.getMessage}")
          false
      }
  }

  def confirmBooking(
    bookingId: String,
    clinicInfo: String,
    appointmentType: String
  ): Future[Boolean] = {
    val accessToken = dom.window.localStorage.getItem("accessToken")
    if (accessToken == null) {
      dom.window.alert("You are not logged in.")
      return Future.successful(false)
    }

    val requestHeaders = new dom.Headers()
    requestHeaders.append("Content-Type", "application/json")
    requestHeaders.append("Authorization", s"Bearer $accessToken")
    requestHeaders.append("apikey", SUPABASE_ANON_KEY)

    val payload = js.Dynamic.literal(
      clinic_info = clinicInfo,
      appointment_type = appointmentType
    )

    val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.PUT
      headers = requestHeaders
      body = JSON.stringify(payload)
    }

    dom.fetch(s"/api/bookings/confirm/$bookingId", requestInit)
      .flatMap { response =>
        if (response.ok) {
          response.json().toFuture.map(_ => true)
        } else {
          dom.window.alert("Failed to confirm booking.")
          Future.successful(false)
        }
      }
  }
}