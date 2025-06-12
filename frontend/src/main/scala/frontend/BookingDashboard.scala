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
  var lengthInput: Input = _
  var slotsContainer: Div = _
  var currentWeekStart: js.Date = getStartOfWeek(new js.Date())
  var dateSelect: Select = _
  var timeSelect: Select = _

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

        dateSelect = document.createElement("select").asInstanceOf[Select]
        def populateDateSelect(): Unit = {
          dateSelect.innerHTML = ""
          val weekStart = currentWeekStart
          for (i <- 0 to 4) {
            val d = addDays(weekStart, i)
            val yyyy = d.getUTCFullYear()
            val mm = d.getUTCMonth() + 1
            val dd = d.getUTCDate()
            val value = f"$yyyy-${mm.toInt}%02d-${dd.toInt}%02d"
            val opt = document.createElement("option").asInstanceOf[Option]
            opt.value = value
            opt.textContent = formatDay(d).replace("\n", " ")
            dateSelect.appendChild(opt)
          }
        }
        populateDateSelect()
        formContainer.appendChild(createRow("Select date:", dateSelect))

        timeSelect = document.createElement("select").asInstanceOf[Select]
        for (hour <- 9 to 16; minute <- List(0, 30)) {
          // Last slot should be 16:30
          if (hour < 16 || (hour == 16 && minute == 30)) {
            val value = f"$hour%02d:$minute%02d"
            val opt = document.createElement("option").asInstanceOf[Option]
            opt.value = value
            opt.textContent = value
            timeSelect.appendChild(opt)
          }
        }
        formContainer.appendChild(createRow("Select time:", timeSelect))


        val lengthSelect = document.createElement("select").asInstanceOf[Select]
        for (mins <- List(30, 60, 90, 120)) {
          val opt = document.createElement("option").asInstanceOf[Option]
          opt.value = mins.toString
          opt.textContent = s"$mins minutes"
          lengthSelect.appendChild(opt)
        }
        lengthSelect.value = "30"
        formContainer.appendChild(createRow("Length (minutes):", lengthSelect))
        lengthInput = lengthSelect.asInstanceOf[Input]

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
        bookButton.onclick = (_: dom.MouseEvent) => {
          createSlot(dateSelect.value, timeSelect.value, lengthSelect.value.toLong)
        }
        formContainer.appendChild(bookButton)

        val calendarContainer = document.createElement("div").asInstanceOf[Div]
        calendarContainer.setAttribute("style", "flex: 1; min-width: 500px;")

        val navDiv = document.createElement("div").asInstanceOf[Div]
        navDiv.setAttribute("style", "display: flex; gap: 20px; align-items: center; margin-bottom: 10px;")
        val prevBtn = document.createElement("button").asInstanceOf[Button]
        prevBtn.textContent = "< Previous Week"
        prevBtn.onclick = (_: dom.MouseEvent) => {
          currentWeekStart = addDays(currentWeekStart, -7)
          populateDateSelect()
          fetchAndDisplaySlots()
        }
        val nextBtn = document.createElement("button").asInstanceOf[Button]
        nextBtn.textContent = "Next Week >"
        nextBtn.onclick = (_: dom.MouseEvent) => {
          currentWeekStart = addDays(currentWeekStart, 7)
          populateDateSelect()
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

        mainContainer.appendChild(formContainer)
        mainContainer.appendChild(calendarContainer)

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

    val slotsApiUrl = s"$BACKEND_URL/slots/list?clinic_id=$clinicId"
    val hdrs = new dom.Headers()
    hdrs.append("Content-Type", "application/json")

    val req = new dom.RequestInit {
      method = dom.HttpMethod.GET
      headers = hdrs
    }

    dom.fetch(slotsApiUrl, req).toFuture.flatMap { res =>
      if (res.ok) res.json().toFuture
      else scala.concurrent.Future.failed(new Exception("Fetch slots failed"))
    }.foreach { json =>
      val slots = json.asInstanceOf[js.Array[js.Dynamic]]
      renderWeekCalendar(slots)
    }
  }

  def renderWeekCalendar(slots: js.Array[js.Dynamic]): Unit = {
    slotsContainer.innerHTML = ""
    val weekLabel = document.getElementById("weekLabel")
    val weekStart = new js.Date(currentWeekStart.getTime())
    val weekEnd = addDays(weekStart, 4)
    weekLabel.textContent = s"${formatDate(weekStart)} - ${formatDate(weekEnd)}"

    val today = new js.Date()
    today.setUTCHours(0, 0, 0, 0)
    val filteredSlots = slots.filter { slot =>
      val slotDate = new js.Date(slot.slotTime.asInstanceOf[String])
      slotDate.getTime() >= today.getTime()
    }

    val days = (0 to 4).map(i => addDays(weekStart, i))
    val times = (9 * 60 until 17 * 60 by 30).toArray
    val pxPerMinute = 1

    val table = document.createElement("table").asInstanceOf[Table]
    table.setAttribute("style", "border-collapse: collapse; width: 100%;")

    val header = document.createElement("tr")
    val timeHeader = document.createElement("th")
    header.appendChild(timeHeader)
    days.foreach { d =>
      val th = document.createElement("th")
      th.textContent = formatDay(d)
      th.setAttribute("style", "padding: 4px; background: #f0f0f0; border: 1px solid #ccc;")
      header.appendChild(th)
    }
    table.appendChild(header)

    val slotMap = scala.collection.mutable.Map[String, js.Dynamic]()
    val spanMap = scala.collection.mutable.Map[String, Int]()
    filteredSlots.foreach { slot =>
      val dt = new js.Date(slot.slotTime.asInstanceOf[String])
      val key = s"${dt.getUTCFullYear()}-${dt.getUTCMonth()}-${dt.getUTCDate()}-${dt.getUTCHours()}-${dt.getUTCMinutes()}"
      slotMap(key) = slot
      spanMap(key) = Math.ceil(slot.slotLength.asInstanceOf[Double] / 30.0).toInt
    }

    val skip = scala.collection.mutable.Set[String]()

    times.indices.foreach { tIdx =>
      val tr = document.createElement("tr")
      val min = times(tIdx)
      val timeTd = document.createElement("td")
      timeTd.textContent = f"${min / 60}%02d:${min % 60}%02d"
      timeTd.setAttribute("style", "width: 60px; padding: 4px; background: #f0f0f0; border: 1px solid #ccc;")
      tr.appendChild(timeTd)

      days.foreach { day =>
        val key = s"${day.getUTCFullYear()}-${day.getUTCMonth()}-${day.getUTCDate()}-${min / 60}-${min % 60}"
        if (skip.contains(key)) {
          // skip cell
        } else if (slotMap.contains(key)) {
          val slot = slotMap(key)
          val span = spanMap(key)
          val btn = document.createElement("button").asInstanceOf[Button]
          btn.textContent = if (slot.isTaken.asInstanceOf[Boolean]) "Booked" else "Available"
          btn.setAttribute("style",
            s"""
               |width: 100%;
               |height: ${span * 30 * pxPerMinute}px;
               |border: none;
               |border-radius: 4px;
               |cursor: pointer;
               |background: ${if (slot.isTaken.asInstanceOf[Boolean]) "#ffe066" else "#4caf50"};
               |color: ${if (slot.isTaken.asInstanceOf[Boolean]) "#333" else "#fff"};
               |display: flex;
               |align-items: center;
               |justify-content: center;
               """.stripMargin)
          btn.onclick = (_: dom.MouseEvent) => showSlotDetails(slot)

          val td = document.createElement("td")
          td.setAttribute("rowspan", span.toString)
          td.setAttribute("style", "padding: 2px; border: 1px solid #ccc;")
          td.appendChild(btn)
          tr.appendChild(td)

          // mark cells to be skipped
          for (i <- 1 until span if tIdx + i < times.length) {
            val nextMin = times(tIdx + i)
            val skipKey = s"${day.getUTCFullYear()}-${day.getUTCMonth()}-${day.getUTCDate()}-${nextMin / 60}-${nextMin % 60}"
            skip += skipKey
          }
        } else {
          val td = document.createElement("td")
          td.setAttribute("style", s"height: ${30 * pxPerMinute}px; border: 1px solid #ccc; padding: 1px;")
          tr.appendChild(td)
        }
      }
      table.appendChild(tr)
    }

    slotsContainer.appendChild(table)
  }

  def getStartOfWeek(date: js.Date): js.Date = {
    val d = new js.Date(date.getTime())
    val day = d.getUTCDay()
    val diff = if (day == 0) -6 else 1 - day
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
    f"${date.getUTCDate().toInt}%02d/${date.getUTCMonth().toInt + 1}%02d/${date.getUTCFullYear()}"

  def formatDay(date: js.Date): String = {
    val days = Array("Mon", "Tue", "Wed", "Thu", "Fri")
    val idx = date.getUTCDay() match {
      case 1 => 0; case 2 => 1; case 3 => 2; case 4 => 3; case 5 => 4; case _ => 0
    }
    s"${days(idx)}\n${formatDate(date)}"
  }

  def formatSlotTime(iso: String): String = {
    val d = new js.Date(iso)
    f"${d.getUTCHours().toInt}%02d:${d.getUTCMinutes().toInt}%02d"
  }

  def showSlotDetails(slot: js.Dynamic): Unit = {
    val time = formatSlotTime(slot.slotTime.asInstanceOf[String])
    val confirm = dom.window.confirm(
      s"""Time: $time
         |Length: ${slot.slotLength} minutes
         |Status: ${if (slot.isTaken.asInstanceOf[Boolean]) "Booked" else "Available"}
         |
         |Delete this slot?
         |""".stripMargin)
    if (confirm) {
      deleteSlot(slot.slotId.asInstanceOf[String])
    }
  }

  def deleteSlot(slotId: String): Unit = {
    val url = s"$BACKEND_URL/slots/delete/$slotId"
    val hdrs = new dom.Headers()
    hdrs.append("Content-Type", "application/json")
    val req = new dom.RequestInit {
      method = dom.HttpMethod.DELETE
      headers = hdrs
    }

    dom.fetch(url, req).toFuture.flatMap { res =>
      if (res.ok) {
        dom.window.alert("Slot deleted.")
        fetchAndDisplaySlots()
        scala.concurrent.Future.successful(())
      } else {
        dom.window.alert("Failed to delete slot.")
        scala.concurrent.Future.failed(new Exception("Delete slot failed"))
      }
    }
  }

  def createSlot(date: String, time: String, length: Long): Unit = {
    val clinicId = dom.window.localStorage.getItem("userId")
    if (clinicId == null || clinicId.isEmpty) {
      dom.window.alert("No clinic ID in local storage.")
      return
    }
    if (!isValidDateTime(date, time)) {
      dom.window.alert("Please select a weekday between 09:00 and 17:00.")
      return
    }

    // Prevent creating slots for today or in the past
    val now = new js.Date()
    now.setUTCHours(0, 0, 0, 0)
    val slotDate = new js.Date(s"${date}T$time:00Z")
    slotDate.setUTCHours(0, 0, 0, 0)
    if (slotDate.getTime() <= now.getTime()) {
      dom.window.alert("You cannot create slots for today or in the past.")
      return
    }

    val iso = s"${date}T${time}:00Z"
    val payload = literal("slot_time" -> iso, "clinic_id" -> clinicId, "slot_length" -> length)

    val hdrs = new dom.Headers()
    hdrs.append("Content-Type", "application/json")
    val req = literal(
      method = dom.HttpMethod.POST,
      headers = hdrs,
      body = JSON.stringify(payload)
    ).asInstanceOf[dom.RequestInit]

    dom.fetch(s"$BACKEND_URL/slots/create", req).toFuture.flatMap { res =>
      if (res.ok) {
        dom.window.alert("Slot created.")
        resetForm()
        fetchAndDisplaySlots()
        scala.concurrent.Future.successful(())
      } else {
        dom.window.alert("Failed to create slot.")
        scala.concurrent.Future.failed(new Exception("Create failed"))
      }
    }
  }

  def isValidDateTime(date: String, time: String): Boolean = {
    val dt = new js.Date(s"${date}T$time:00Z")
    val day = dt.getUTCDay()
    val hour = dt.getUTCHours()
    date.nonEmpty && time.nonEmpty && (day >= 1 && day <= 5) && (hour >= 9 && hour < 17)
  }

  def resetForm(): Unit = {
    dateSelect.selectedIndex = 0
    timeSelect.selectedIndex = 0
    lengthInput.value = "30"
  }

  def clearPage(): Unit = {
    while (document.body.firstChild != null) {
      document.body.removeChild(document.body.firstChild)
    }
  }

  def createHomeButton(): Button = {
    val btn = document.createElement("button").asInstanceOf[Button]
    btn.textContent = "← Home"
    btn.onclick = (_: dom.MouseEvent) => dom.window.location.href = "/"
    btn
  }
}
