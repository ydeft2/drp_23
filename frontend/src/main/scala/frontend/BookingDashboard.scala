package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.collection.immutable.Map

object BookingDashboard {

  val BACKEND_URL = "/api"
  var dateInput: Input = _
  var timeInput: Input = _
  var lengthInput: Input = _

  var slotsContainer: Div = _

  def createRow(labelText: String, inputElement: Element): Div = {
    val row = document.createElement("div").asInstanceOf[Div]
    row.setAttribute("style",
      """
        |display: flex;
        |align-items: center;
        |gap: 10px;
        """.stripMargin)
    val label = document.createElement("label")
    label.textContent = labelText
    row.appendChild(label)
    row.appendChild(inputElement)
    row
  }

  def render(): Unit = {
    Layout.renderPage(
      leftButton = Some(createHomeButton()),
      contentRender = () =>
        {
          val container = document.createElement("div").asInstanceOf[Div]
          container.setAttribute("style",
            """
            |display: flex;
            |flex-direction: column;
            |align-items: flex-start;
            |gap: 15px;
            |margin: 50px auto;
            |max-width: 500px;
            |padding: 20px;
            |border: 1px solid #ccc;
            |border-radius: 10px;
            |box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            """.stripMargin)

          dateInput = document.createElement("input").asInstanceOf[Input]
          dateInput.setAttribute("type", "date")
          dateInput.setAttribute("required", "true")
          container.appendChild(createRow("Select date:", dateInput))

          timeInput = document.createElement("input").asInstanceOf[Input]
          timeInput.setAttribute("type", "time")
          timeInput.setAttribute("required", "true")
          container.appendChild(createRow("Select time:", timeInput))

          lengthInput = document.createElement("input").asInstanceOf[Input]
          lengthInput.setAttribute("type", "number")
          lengthInput.setAttribute("min", "5")
          lengthInput.setAttribute("value", "30")
          container.appendChild(createRow("Length (minutes):", lengthInput))

          val bookButton = document.createElement("button").asInstanceOf[Button]
          bookButton.textContent = "Create Slot"
          bookButton.setAttribute("style",
            """
            |padding: 8px 16px;
            |border: none;
            |background-color: #800080;
            |color: white;
            |border-radius: 6px;
            |cursor: pointer;
            """.stripMargin)
          bookButton.addEventListener("click", { (_: dom.MouseEvent) =>
            createSlot(
              dateInput.value,
              timeInput.value,
              lengthInput.value.toLong
            )
          })
          container.appendChild(bookButton)

          document.body.appendChild(container)

          slotsContainer = document.createElement("div").asInstanceOf[Div]
          slotsContainer.setAttribute("style",
            """
              |margin-top: 30px;
              |max-width: 500px;
              |border-top: 1px solid #ccc;
              |padding-top: 20px;
              """.stripMargin)
          document.body.appendChild(slotsContainer)

          fetchAndDisplaySlots()
        }
    )
  }

  def fetchAndDisplaySlots(): Unit = {
    val clinicId = dom.window.localStorage.getItem("userId")
    if (clinicId == null || clinicId.isEmpty) {
      dom.window.alert("No clinic ID found in local storage.")
      return
    }

    val baseUrl = s"$BACKEND_URL/slots/list"
    val slotsApiUrl = s"$baseUrl?clinic_id=$clinicId"

    val requestHeaders = new dom.Headers()
    requestHeaders.append("Content-Type", "application/json")

    val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.GET
      headers = requestHeaders
    }

    dom.fetch(slotsApiUrl, requestInit).toFuture
      .flatMap { response =>
        if (response.ok) response.json().toFuture
        else {
          dom.window.alert(s"Failed to fetch slots: ${response.statusText}")
          scala.concurrent.Future.failed(new Exception("Fetch slots failed"))
        }
      }
      .foreach { json =>
        val slots = json.asInstanceOf[js.Array[js.Dynamic]]

        slotsContainer.innerHTML = ""

        if (slots.isEmpty) {
          val noSlotsMsg = document.createElement("p")
          noSlotsMsg.textContent = "No slots available."
          slotsContainer.appendChild(noSlotsMsg)
        } else {
          val sortedSlots = slots.sort((a, b) => {
            val timeA = js.Date.parse(a.slotTime.asInstanceOf[String])
            val timeB = js.Date.parse(b.slotTime.asInstanceOf[String])
            if (timeA < timeB) -1 else if (timeA > timeB) 1 else 0
          })
          sortedSlots.foreach { slot =>
            val slotDiv = document.createElement("div").asInstanceOf[Div]
            slotDiv.style.marginTop = "20px"
            slotDiv.style.marginLeft = "auto"
            slotDiv.style.marginRight = "auto"
            slotDiv.style.width = "80%"
            slotDiv.style.padding = "20px"
            slotDiv.style.border = "1px solid #ccc"
            slotDiv.style.borderRadius = "8px"
            slotDiv.style.backgroundColor = "#f9f9f9"
            slotDiv.style.boxShadow = "0 2px 8px rgba(0,0,0,0.1)"

            val formattedTime = formatSlotTime(slot.slotTime.asInstanceOf[String])

            val title = document.createElement("h3")
            title.textContent = "Your Clinic"
            slotDiv.appendChild(title)

            val timeP = document.createElement("p")
            timeP.textContent = formattedTime
            slotDiv.appendChild(timeP)

            val lengthP = document.createElement("p")
            lengthP.textContent = s"Length: ${slot.slotLength} minutes"
            slotDiv.appendChild(lengthP)

            val isTaken = slot.isTaken.asInstanceOf[Boolean]
            val statusP = document.createElement("p")
            statusP.textContent = s"Status: ${if (isTaken) "Booked" else "Available"}"
            slotDiv.appendChild(statusP)

            slotDiv.style.cursor = "pointer"
            slotDiv.addEventListener("click", { (_: dom.MouseEvent) =>
              showSlotDetails(slot)
            })

            slotsContainer.appendChild(slotDiv)
          }
        }
      }
  }

  def showSlotDetails(slot: js.Dynamic): Unit = {
    val formattedTime = formatSlotTime(slot.slotTime.asInstanceOf[String])
    val confirmDelete = dom.window.confirm(
      s"""Time: $formattedTime
         |Length: ${slot.slotLength} minutes
         |Status: ${if (slot.isTaken.asInstanceOf[Boolean]) "Booked" else "Available"}
         |Delete this slot?
         |""".stripMargin)
    if (confirmDelete) {
      deleteSlot(slot.slotId.asInstanceOf[String])
    }
  }

  def deleteSlot(slotId: String): Unit = {
    val deleteUrl = s"$BACKEND_URL/slots/delete/$slotId"
    val requestHeaders = new dom.Headers()
    requestHeaders.append("Content-Type", "application/json")

    val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.DELETE
      headers = requestHeaders
    }

    dom.fetch(deleteUrl, requestInit).toFuture
      .flatMap { response =>
        if (response.ok) {
          dom.window.alert("Slot deleted successfully.")
          fetchAndDisplaySlots()
          scala.concurrent.Future.successful(())
        } else {
          dom.window.alert(s"Failed to delete slot: ${response.statusText}")
          scala.concurrent.Future.failed(new Exception("Delete slot failed"))
        }
      }
  }

  def createSlot(date: String, time: String, length: Long): Unit = {
    val clinicId = dom.window.localStorage.getItem("userId")
    if (clinicId == null || clinicId.isEmpty) {
      dom.window.alert("No clinic ID found in local storage.")
      return
    }
    if (!isValidDateTime(date, time)) {
      dom.window.alert("Please select a weekday between 09:00 and 17:00.")
      return
    }
    if (length < 5) {
      dom.window.alert("Length must be at least 5 minutes.")
      return
    }

    val isoDatetime = s"${date}T${time}:00Z" // UTC time

    val slotReq = literal(
      "slot_time" -> isoDatetime,
      "clinic_id" -> clinicId,
      "slot_length" -> length
    )

    val createUrl = s"$BACKEND_URL/slots/create"
    val requestHeaders = new dom.Headers()
    requestHeaders.append("Content-Type", "application/json")

    val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.POST
      headers = requestHeaders
      body = JSON.stringify(slotReq)
    }

    dom.fetch(createUrl, requestInit).toFuture
      .flatMap { response =>
        if (response.ok) {
          dom.window.alert("Slot created successfully.")
          resetForm()
          fetchAndDisplaySlots()
          scala.concurrent.Future.successful(())
        } else {
          dom.window.alert(s"Failed to create slot: ${response.statusText}")
          scala.concurrent.Future.failed(new Exception("Create slot failed"))
        }
      }
  }

  def isValidDateTime(date: String, time: String): Boolean = {
    if (date.isEmpty || time.isEmpty) return false
    val dt = new js.Date(s"${date}T$time:00Z")
    val day = dt.getUTCDay()
    val hour = dt.getUTCHours()
    // Weekdays 1 to 5, 09:00 to 17:00 UTC
    (day >= 1 && day <= 5) && (hour >= 9 && hour < 17)
  }

  def resetForm(): Unit = {
    dateInput.value = ""
    timeInput.value = ""
    lengthInput.value = "30"
  }

  def clearPage(): Unit = {
    while (document.body.firstChild != null) {
      document.body.removeChild(document.body.firstChild)
    }
  }

  def createSubpageHeader(text: String): Div = {
    val header = document.createElement("div").asInstanceOf[Div]
    header.setAttribute("style",
      """
        |margin-top: 20px;
        |margin-bottom: 30px;
        |font-weight: bold;
        |font-size: 24px;
        |color: #800080;
        |text-align: center;
        |""".stripMargin)
    header.textContent = text
    header
  }

}
