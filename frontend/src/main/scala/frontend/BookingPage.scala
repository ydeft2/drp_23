package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.{Button, Div, Input, Table, TableCell, TableRow}
import scala.scalajs.js
import scala.scalajs.js.JSON
import java.time._
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BookingPage {
  // the hours we display
  private val timeStrings = Seq("09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00")
  private val zoneId      = ZoneOffset.UTC

  // filter state
  private var onlyFree     = true
  private var clinicFilter = ""
  private var fromFilter   = Option.empty[String] // ISO datetime string
  private var toFilter     = Option.empty[String]

  // page‐top elements
  private var filterOnlyFreeCheckbox: Input = _
  private var filterClinicInput:        Input = _
  private var filterFromInput:          Input = _
  private var filterToInput:            Input = _

  def render(): Unit = {
    Layout.renderPage(
      leftButton    = Some(createHomeButton()),
      contentRender = () => {
        // compute week bounds
        val today        = LocalDate.now(Clock.systemUTC())
        val daysFromSun  = today.getDayOfWeek.getValue % 7
        val weekStart0   = today.minusDays(daysFromSun.toLong)
        var currentStart = weekStart0
        val lastStart    = weekStart0.plusWeeks(52)

        val tableHolder = document.createElement("div").asInstanceOf[Div]

        val prevBtn = navButton("< Prev Week")
        val nextBtn = navButton("Next Week >")
        val weekLabel = document.createElement("span").asInstanceOf[Div]
        weekLabel.style.cssText = "font-weight: bold;"
        
        // update nav label & buttons
        def updateNav(): Unit = {
          val end = currentStart.plusDays(6)
          weekLabel.textContent = s"${currentStart} → ${end}"
          prevBtn.disabled = currentStart == weekStart0
          nextBtn.disabled = currentStart == lastStart
        }
        
        // --- fetch & draw ---
        def drawWeek(): Unit = {
          tableHolder.innerHTML = "<em>Loading…</em>"
          updateNav()

          fetchSlots(currentStart).foreach { slots =>
            tableHolder.innerHTML = ""
            tableHolder.appendChild(buildTable(currentStart, slots))
          }
        }
        
        // container
        val container = document.createElement("div").asInstanceOf[Div]
        container.style.cssText =
          """
            margin-top: 80px;
            width: 90%; max-width: 1200px;
            margin-left: auto; margin-right: auto;
          """

        // --- 1) Filter bar ---
        val fbar = document.createElement("div").asInstanceOf[Div]
        fbar.style.cssText =
          """
            display: flex;
            gap: 12px;
            margin-bottom: 16px;
          """

        // only‐free checkbox
        filterOnlyFreeCheckbox = document.createElement("input").asInstanceOf[Input]
        filterOnlyFreeCheckbox.`type`    = "checkbox"
        filterOnlyFreeCheckbox.checked   = onlyFree
        filterOnlyFreeCheckbox.onchange  = (_: dom.Event) => {
          onlyFree = filterOnlyFreeCheckbox.checked
          drawWeek()
        }
        val freeLabel = document.createElement("label")
        freeLabel.textContent = "Only available "
        freeLabel.appendChild(filterOnlyFreeCheckbox)
        fbar.appendChild(freeLabel)

        // clinic‐info text
        filterClinicInput = document.createElement("input").asInstanceOf[Input]
        filterClinicInput.placeholder    = "Clinic info contains…"
        filterClinicInput.oninput       = (_: dom.Event) => {
          clinicFilter = filterClinicInput.value.trim
          drawWeek()
        }
        fbar.appendChild(filterClinicInput)

        // from / to datetime
        filterFromInput = document.createElement("input").asInstanceOf[Input]
        filterFromInput.`type`          = "datetime-local"
        filterFromInput.onchange        = (_: dom.Event) => {
          fromFilter = Option(filterFromInput.value).filter(_.nonEmpty)
          drawWeek()
        }
        fbar.appendChild(filterFromInput)

        filterToInput = document.createElement("input").asInstanceOf[Input]
        filterToInput.`type`            = "datetime-local"
        filterToInput.onchange          = (_: dom.Event) => {
          toFilter = Option(filterToInput.value).filter(_.nonEmpty)
          drawWeek()
        }
        fbar.appendChild(filterToInput)

        container.appendChild(fbar)

        // --- 2) Week nav ---
        val navBar = document.createElement("div").asInstanceOf[Div]
        navBar.style.cssText =
          """
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 12px;
            margin-bottom: 15px;
            padding: 10px;
            background-color: #e0e0e0;
            border-radius: 5px;
            box-shadow: 0 1px 3px rgba(23,21,21,0.1);
          """

        navBar.appendChild(prevBtn)
        navBar.appendChild(weekLabel)
        navBar.appendChild(nextBtn)
        container.appendChild(navBar)

        // --- 3) Table placeholder ---
        container.appendChild(tableHolder)



        // wire up nav
        prevBtn.onclick = (_: dom.MouseEvent) => {
          if (currentStart.isAfter(weekStart0)) {
            currentStart = currentStart.minusWeeks(1)
            drawWeek()
          }
        }
        nextBtn.onclick = (_: dom.MouseEvent) => {
          if (currentStart.isBefore(lastStart)) {
            currentStart = currentStart.plusWeeks(1)
            drawWeek()
          }
        }

        // initial draw
        document.body.appendChild(container)
        drawWeek()


      }
    )
  }

  // make the HTTP request to our filterable endpoint
  private def fetchSlots(weekStart: LocalDate): Future[Seq[js.Dynamic]] = {
    // build query params
    val base   = "/api/slots/list"
    val parts  = scala.collection.mutable.ArrayBuffer.empty[String]
    if (onlyFree)   parts += "is_taken=false"
    if (clinicFilter.nonEmpty)
      parts += s"clinic_info=${encode(clinicFilter)}"
    fromFilter.foreach(f => parts += s"slot_time_gte=${encode(f)}")
    toFilter.foreach  (t => parts += s"slot_time_lte=${encode(t)}")
    // always bring back entire week: limit=100 offset=0
    parts += "limit=100"; parts += "offset=0"

    val url = if (parts.isEmpty) base else base + "?" + parts.mkString("&")

    dom.fetch(url).toFuture
      .flatMap(_.json().toFuture)
      .map(_.asInstanceOf[js.Array[js.Dynamic]].toSeq)
  }

  // build the 7×rows grid
  private def buildTable(weekStart: LocalDate, slots: Seq[js.Dynamic]): Table = {
    // group by date → time
    val byDay = slots.groupBy { s =>
      Instant.parse(s.slotTime.asInstanceOf[String])
        .atZone(zoneId).toLocalDate
    }.view.mapValues(_.toList).toMap

    val tbl = document.createElement("table").asInstanceOf[Table]
    tbl.style.cssText =
      """
        border-collapse: collapse;
        width: 100%;
        background: white;
      """

    // header row
    val hdr = document.createElement("tr").asInstanceOf[TableRow]
    hdr.appendChild(th(""))
    (0 to 6).foreach { d =>
      hdr.appendChild(th(weekStart.plusDays(d).getDayOfWeek.toString.take(3)))
    }
    tbl.appendChild(hdr)

    // each time row
    timeStrings.foreach { t =>
      val row = document.createElement("tr").asInstanceOf[TableRow]
      row.appendChild(th(t))

      (0 to 6).foreach { d =>
        val date = weekStart.plusDays(d)
        val slotAtTime = byDay.get(date).flatMap(_.find { s =>
          Instant.parse(s.slotTime.asInstanceOf[String])
            .atZone(zoneId).toLocalTime.format(DateTimeFormatter.ofPattern("HH:mm")) == t
        })

        slotAtTime match {
          case Some(s) =>
            val btn = document.createElement("button").asInstanceOf[Button]
            btn.textContent = "Book"
            btn.style.cssText =
              """
                width: 100%; height: 100%;
                background: #4caf50; color: white;
                border: none; cursor: pointer;
              """
            btn.onclick = (_: dom.MouseEvent) => requestBooking(s)
            val cell = document.createElement("td").asInstanceOf[TableCell]
            cell.style.cssText = "border:1px solid #ccc; padding:4px; background:#e0ffe0;"
            cell.appendChild(btn)
            row.appendChild(cell)

          case None =>
            val cell = document.createElement("td").asInstanceOf[TableCell]
            cell.textContent = "-"
            cell.style.cssText = "border:1px solid #ccc; padding:4px; color:#999;"
            row.appendChild(cell)
        }
      }

      tbl.appendChild(row)
    }

    tbl
  }

  // send POST /api/bookings/request
  private def requestBooking(slot: js.Dynamic): Unit = {
    val payload = js.Dynamic.literal(
      slot_id    = slot.slotId.asInstanceOf[String],
      patient_id = dom.window.localStorage.getItem("userId"),
      clinic_id  = slot.clinicId.asInstanceOf[String]
    )

    val reqInit = new dom.RequestInit {
      method = dom.HttpMethod.POST
      headers = new dom.Headers {
        append("Content-Type", "application/json")
        Option(dom.window.localStorage.getItem("accessToken"))
          .foreach(t => append("Authorization", s"Bearer $t"))
      }
      body = JSON.stringify(payload)
    }

    dom.fetch("/api/bookings/request", reqInit).toFuture
      .flatMap { r =>
        if (r.ok) r.json().toFuture.map(_ => dom.window.alert("Requested!"))
        else      r.text().toFuture.map(err => dom.window.alert(s"Failed: $err"))
      }
  }

  private def th(txt: String): TableCell = {
    val c = document.createElement("th").asInstanceOf[TableCell]
    c.textContent = txt
    c.style.cssText = "border:1px solid #999; padding:6px; background:#f0f0f0;"
    c
  }

  private def navButton(text: String): Button = {
    val b = document.createElement("button").asInstanceOf[Button]
    b.textContent = text
    b.style.cssText = "padding:6px 12px;"
    b
  }

  private def encode(s: String): String =
    js.URIUtils.encodeURIComponent(s)
}
