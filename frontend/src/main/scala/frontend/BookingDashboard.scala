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
  var currentWeekStart: js.Date = getStartOfWeek(new js.Date())

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
      contentRender = () => {
        // Main flex container
        val mainContainer = document.createElement("div").asInstanceOf[Div]
        mainContainer.setAttribute("style",
          """
          |display: flex;
          |flex-direction: row;
          |align-items: flex-start;
          |gap: 40px;
          |margin: 50px auto;
          |max-width: 1200px;
          |padding: 20px;
          |border: 1px solid #ccc;
          |border-radius: 10px;
          |box-shadow: 0 2px 8px rgba(0,0,0,0.1);
          """.stripMargin)

        // Booking form (left)
        val formContainer = document.createElement("div").asInstanceOf[Div]
        formContainer.setAttribute("style",
          """
          |display: flex;
          |flex-direction: column;
          |align-items: flex-start;
          |gap: 15px;
          |min-width: 280px;
          |max-width: 350px;
          """.stripMargin)

        dateInput = document.createElement("input").asInstanceOf[Input]
        dateInput.setAttribute("type", "date")
        dateInput.setAttribute("required", "true")
        formContainer.appendChild(createRow("Select date:", dateInput))

        timeInput = document.createElement("input").asInstanceOf[Input]
        timeInput.setAttribute("type", "time")
        timeInput.setAttribute("required", "true")
        formContainer.appendChild(createRow("Select time:", timeInput))

        lengthInput = document.createElement("input").asInstanceOf[Input]
        lengthInput.setAttribute("type", "number")
        lengthInput.setAttribute("min", "5")
        lengthInput.setAttribute("value", "30")
        formContainer.appendChild(createRow("Length (minutes):", lengthInput))

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
        formContainer.appendChild(bookButton)

        // Calendar and navigation (right)
        val calendarContainer = document.createElement("div").asInstanceOf[Div]
        calendarContainer.setAttribute("style", "flex: 1; min-width: 500px;")

        val navDiv = document.createElement("div").asInstanceOf[Div]
        navDiv.setAttribute("style", "display: flex; gap: 20px; align-items: center; margin-bottom: 10px;")
        val prevBtn = document.createElement("button").asInstanceOf[Button]
        prevBtn.textContent = "< Previous Week"
        prevBtn.onclick = (_: dom.MouseEvent) => {
          currentWeekStart = addDays(currentWeekStart, -7)
          fetchAndDisplaySlots()
        }
        val nextBtn = document.createElement("button").asInstanceOf[Button]
        nextBtn.textContent = "Next Week >"
        nextBtn.onclick = (_: dom.MouseEvent) => {
          currentWeekStart = addDays(currentWeekStart, 7)
          fetchAndDisplaySlots()
        }
        val weekLabel = document.createElement("span")
        weekLabel.setAttribute("id", "weekLabel")
        navDiv.appendChild(prevBtn)
        navDiv.appendChild(weekLabel)
        navDiv.appendChild(nextBtn)
        calendarContainer.appendChild(navDiv)

        slotsContainer = document.createElement("div").asInstanceOf[Div]
        slotsContainer.setAttribute("style", "margin-top: 10px; max-width: 900px; overflow-x: auto;")
        calendarContainer.appendChild(slotsContainer)

        // Add both to main container
        mainContainer.appendChild(formContainer)
        mainContainer.appendChild(calendarContainer)

        // Clear and append to body
        clearPage()
        document.body.appendChild(mainContainer)

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
        renderWeekCalendar(slots)
      }
  }

  def renderWeekCalendar(slots: js.Array[js.Dynamic]): Unit = {
    slotsContainer.innerHTML = ""

    // Set week label
    val weekLabel = document.getElementById("weekLabel")
    val weekStart = new js.Date(currentWeekStart.getTime())
    val weekEnd = addDays(weekStart, 4)
    weekLabel.textContent = s"${formatDate(weekStart)} - ${formatDate(weekEnd)}"

    // Prepare calendar grid
    val days = (0 to 4).map(i => addDays(weekStart, i))
    val times = (9 * 60 until 17 * 60 by 30).toArray

    val table = document.createElement("table").asInstanceOf[Table]
    table.setAttribute("style",
      """
      |border-collapse: collapse;
      |width: 100%;
      |background: #fff;
      """.stripMargin)

    // Header row
    val headerRow = document.createElement("tr")
    val emptyTh = document.createElement("th")
    emptyTh.textContent = ""
    headerRow.appendChild(emptyTh)
    days.foreach { d =>
      val th = document.createElement("th")
      th.textContent = formatDay(d)
      th.setAttribute("style", "padding: 4px; border: 1px solid #ccc; background: #f0f0f0;")
      headerRow.appendChild(th)
    }
    table.appendChild(headerRow)

    // Build a map for quick slot lookup and track multi-slot spans
    val slotMap = scala.collection.mutable.Map[String, js.Dynamic]()
    val slotSpanMap = scala.collection.mutable.Map[String, Int]()
    slots.foreach { slot =>
      val dt = new js.Date(slot.slotTime.asInstanceOf[String])
      val key = s"${dt.getUTCFullYear()}-${dt.getUTCMonth()}-${dt.getUTCDate()}-${dt.getUTCHours()}-${dt.getUTCMinutes()}"
      slotMap(key) = slot
      // Calculate how many 30-min intervals this slot covers
      val length = slot.slotLength.asInstanceOf[Double].toInt
      val span = Math.ceil(length / 30.0).toInt
      slotSpanMap(key) = span
    }

    // Track cells that should be skipped due to rowspan
    val skipCells = scala.collection.mutable.Set[String]()

    val baseHeight = 28 // px, height for a 30-min slot

    times.indices.foreach { tIdx =>
      val min = times(tIdx)
      val tr = document.createElement("tr")
      val timeTd = document.createElement("td")
      timeTd.textContent = f"${min / 60}%02d:${min % 60}%02d"
      timeTd.setAttribute("style", "padding: 4px; border: 1px solid #ccc; background: #f0f0f0; width: 60px;")
      tr.appendChild(timeTd)

      days.foreach { day =>
        val cellKey = s"${day.getUTCFullYear()}-${day.getUTCMonth()}-${day.getUTCDate()}-${min / 60}-${min % 60}"
        if (skipCells.contains(cellKey)) {
          // This cell is covered by a previous slot's rowspan, skip it
        } else if (slotMap.contains(cellKey)) {
          val slot = slotMap(cellKey)
          val span = slotSpanMap(cellKey) // <-- Move this line up!
          val btn = document.createElement("button").asInstanceOf[Button]
          btn.textContent = if (slot.isTaken.asInstanceOf[Boolean]) "Booked" else "Available"
          btn.setAttribute(
            "style",
            (if (slot.isTaken.asInstanceOf[Boolean])
              "width: 100%; background: #ffe066; color: #333; border-radius: 4px; border: none; cursor: pointer;"
            else
              "width: 100%; background: #4caf50; color: #fff; border-radius: 4px; border: none; cursor: pointer;")
              + s"height: ${baseHeight * span}px;"
          )
          btn.onclick = (_: dom.MouseEvent) => showSlotDetails(slot)

          val td = document.createElement("td")
          td.appendChild(btn)
          td.setAttribute("style", s"padding: 2px; border: 1px solid #ccc; position: relative;")

          // Set rowspan if slot is longer than 30min
          if (span > 1) {
            td.setAttribute("rowspan", span.toString)
            // Mark the next (span-1) cells in this column to be skipped
            for (i <- 1 until span if tIdx + i < times.length) {
              val nextMin = times(tIdx + i)
              val skipKey = s"${day.getUTCFullYear()}-${day.getUTCMonth()}-${day.getUTCDate()}-${nextMin / 60}-${nextMin % 60}"
              skipCells += skipKey
            }
          }
          tr.appendChild(td)
        } else {
          val cell = document.createElement("td")
          cell.setAttribute("style", s"padding: 2px; border: 1px solid #ccc; height: ${baseHeight}px; position: relative;")
          tr.appendChild(cell)
        }
      }
      table.appendChild(tr)
    }

    slotsContainer.appendChild(table)
  }

  // --- Utility functions for week navigation and formatting ---

  def getStartOfWeek(date: js.Date): js.Date = {
    val d = new js.Date(date.getTime())
    val day = d.getUTCDay()
    val diff = if (day == 0) -6 else 1 - day // Monday as start
    d.setUTCDate(d.getUTCDate() + diff)
    d.setUTCHours(0, 0, 0, 0)
    d
  }

  def addDays(date: js.Date, days: Int): js.Date = {
    val d = new js.Date(date.getTime())
    d.setUTCDate(d.getUTCDate() + days)
    d
  }

  def formatDate(date: js.Date): String =
    f"${date.getUTCDate().toInt}%02d/${(date.getUTCMonth().toInt + 1)}%02d/${date.getUTCFullYear().toInt}"

  def formatDay(date: js.Date): String = {
    val days = Array("Mon", "Tue", "Wed", "Thu", "Fri")
    val idx = date.getUTCDay() match {
      case 1 => 0; case 2 => 1; case 3 => 2; case 4 => 3; case 5 => 4; case _ => 0
    }
    s"${days(idx)}\n${formatDate(date)}"
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
