package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.{Button, Div, Input, Table, TableCell, TableRow, Span}
import scala.scalajs.js
import java.time._
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BookingPage {
  // --- Common state for both views ---
  private val timeStrings = Seq("09:00","10:00","11:00","13:00","14:00","15:00","16:00")
  private val zoneId      = ZoneOffset.UTC
  private val displayFmt  = DateTimeFormatter.ofPattern("dd MMM yyyy")

  // Filters used only by list view:
  private var clinicFilter = ""
  private var fromFilter   = Option.empty[String]
  private var toFilter     = Option.empty[String]

  // The one and only `render()`
  def render(): Unit = {
    Layout.renderPage(
      leftButton    = Some(createHomeButton()),
      contentRender = () => {
        // Toggle UI
        val toggleWrapper = document.createElement("div").asInstanceOf[Div]
        toggleWrapper.style.cssText =
          """
            position: fixed; top: 80px; right: 40px; z-index:1000;
          """

        val toggleContainer = document.createElement("div").asInstanceOf[Div]
        toggleContainer.style.cssText =
          """
            display:flex; align-items:center; justify-content:center;
            background:#eee; border-radius:20px; overflow:hidden;
            box-shadow:0 2px 8px rgba(0,0,0,0.07);
          """

        val listLabel = document.createElement("span").asInstanceOf[Span]
        val mapLabel  = document.createElement("span").asInstanceOf[Span]
        Seq(listLabel -> "List View", mapLabel -> "Map View").foreach { case (el, txt) =>
          el.textContent = txt
          el.style.cssText = """
            cursor:pointer; padding:8px 24px; user-select:none;
            font-weight:bold; transition:background 0.2s,color 0.2s;
          """
        }

        toggleContainer.appendChild(listLabel)
        toggleContainer.appendChild(mapLabel)
        toggleWrapper.appendChild(toggleContainer)
        document.body.appendChild(toggleWrapper)

        // Content area under toggle
        val contentArea = document.createElement("div").asInstanceOf[Div]
        contentArea.style.marginTop = "120px"  // leave room for toggle
        /// flika added
        contentArea.style.width = "90%"
        contentArea.style.maxWidth = "1200px"
        contentArea.style.marginLeft = "auto"
        contentArea.style.marginRight = "auto"
        /// end flika
        document.body.appendChild(contentArea)

        // view switch logic
        def setView(isMap: Boolean): Unit = {
          // style labels
          if (isMap) {
            mapLabel.style.background = "#1976d2"; mapLabel.style.color = "white"
            listLabel.style.background = "transparent"; listLabel.style.color = "#333"
          } else {
            listLabel.style.background = "#1976d2"; listLabel.style.color = "white"
            mapLabel.style.background = "transparent"; mapLabel.style.color = "#333"
          }
          contentArea.innerHTML = ""
          contentArea.appendChild(
            if (isMap) buildMapViewContent()
            else        buildListViewContent()
          )
        }

        listLabel.onclick = (_: dom.MouseEvent) => setView(false)
        mapLabel .onclick = (_: dom.MouseEvent) => setView(true)

        // initial
        setView(isMap = false)
      }
    )
  }

  // ------------------------
  // LIST VIEW IMPLEMENTATION
  // ------------------------
  private def buildListViewContent(): Div = {
    // compute week bounds
    val today        = LocalDate.now(Clock.systemUTC())
    val daysFromSun  = today.getDayOfWeek.getValue % 7
    val weekStart0   = today.minusDays(daysFromSun.toLong)
    var currentStart = weekStart0
    val lastStart    = weekStart0.plusWeeks(52)

    // page elements
    val container    = document.createElement("div").asInstanceOf[Div]
    container.style.cssText =
      """
        width=100% ; height=100%; padding = 0 0;
      """

    // FILTER BAR
    val fbar = document.createElement("div").asInstanceOf[Div]
    fbar.style.cssText = "display:flex;gap:12px;margin-bottom:16px;"

    // TABLE HOLDER
    val tableHolder = document.createElement("div").asInstanceOf[Div]


    val prevBtn  = navButton("< Prev Week")
    val nextBtn  = navButton("Next Week >")
    val weekLabel= document.createElement("span").asInstanceOf[Span]
    weekLabel.style.fontWeight = "bold"
    // draw logic
    def updateNav(): Unit = {
      val end = currentStart.plusDays(6)
      weekLabel.textContent = s"${currentStart.format(displayFmt)} → ${end.format(displayFmt)}"
      prevBtn.disabled = currentStart == weekStart0
      nextBtn.disabled = currentStart == lastStart
    }

    def drawWeek(): Unit = {
      tableHolder.innerHTML = "<em>Loading…</em>"
      updateNav()
      fetchSlots(currentStart).foreach { slots =>
        tableHolder.innerHTML = ""
        tableHolder.appendChild(buildTable(currentStart, slots))
      }
    }

    val clinicInput = document.createElement("input").asInstanceOf[Input]
    clinicInput.placeholder = "Clinic info contains…"
    clinicInput.oninput = (_: dom.Event) => {
      clinicFilter = clinicInput.value.trim
      drawWeek()
    }
    fbar.appendChild(clinicInput)

    // “From date” label
    val fromLabel = document.createElement("span").asInstanceOf[Span]
    fromLabel.textContent = "From date: "
    fromLabel.style.cssText = "margin-left: 12px; font-weight: bold;"
    fbar.appendChild(fromLabel)

    // from
    val fromInput = document.createElement("input").asInstanceOf[Input]
    fromInput.`type` = "datetime-local"
    fromInput.onchange = (_: dom.Event) => {
      fromFilter = Option(fromInput.value).filter(_.nonEmpty)
      drawWeek()
    }
    fbar.appendChild(fromInput)

    // “To date” label
    val toLabel = document.createElement("span").asInstanceOf[Span]
    toLabel.textContent = "To date: "
    toLabel.style.cssText = "margin-left: 12px; font-weight: bold;"
    fbar.appendChild(toLabel)

    // to
    val toInput = document.createElement("input").asInstanceOf[Input]
    toInput.`type` = "datetime-local"
    toInput.onchange = (_: dom.Event) => {
      toFilter = Option(toInput.value).filter(_.nonEmpty)
      drawWeek()
    }
    fbar.appendChild(toInput)

    container.appendChild(fbar)

    // WEEK NAV
    val navBar   = document.createElement("div").asInstanceOf[Div]
    navBar.style.cssText =
      """
        display: flex; justify-content: center; align-items: center;
        gap: 12px; margin-bottom: 15px; padding: 10px;
        background-color: #e0e0e0; border-radius: 5px;
        box-shadow: 0 1px 3px rgba(23,21,21,0.1);
      """
    navBar.appendChild(prevBtn)
    navBar.appendChild(weekLabel)
    navBar.appendChild(nextBtn)
    container.appendChild(navBar)

    container.appendChild(tableHolder)

    prevBtn.onclick = (_: dom.MouseEvent) => if (currentStart.isAfter(weekStart0)) { currentStart = currentStart.minusWeeks(1); drawWeek() }
    nextBtn.onclick = (_: dom.MouseEvent) => if (currentStart.isBefore(lastStart)) { currentStart = currentStart.plusWeeks(1); drawWeek() }

    drawWeek()
    container
  }

  private def createInput(
                           placeholder: String,
                           onChange: () => Unit,
                           inputType: String = "text"
                         ): Input = {
    val inp = document.createElement("input").asInstanceOf[Input]
    inp.`type` = inputType
    if (placeholder.nonEmpty) inp.placeholder = placeholder
    inp.onchange = (_: dom.Event) => onChange()
    inp.oninput  = (_: dom.Event) => onChange()
    inp
  }

  private def createSpan(text: String, css: String): Span = {
    val s = document.createElement("span").asInstanceOf[Span]
    s.textContent = text
    s.style.cssText = css
    s
  }

  private def fetchSlots(weekStart: LocalDate): Future[Seq[js.Dynamic]] = {
    val base  = "/api/slots/list"
    val parts = scala.collection.mutable.ArrayBuffer.empty[String]

    parts += "is_taken=false"
    if (clinicFilter.nonEmpty) parts += s"clinic_info=${encode(clinicFilter)}"
    fromFilter.foreach(f => parts += s"slot_time_gte=${encode(f + ":00Z")}")
    toFilter.foreach(t   => parts += s"slot_time_lte=${encode(t + ":00Z")}")
    parts += "limit=100"; parts += "offset=0"
    val url = base + "?" + parts.mkString("&")
    dom.fetch(url).toFuture.flatMap(_.json().toFuture).map(_.asInstanceOf[js.Array[js.Dynamic]].toSeq)
  }

  private def buildTable(weekStart: LocalDate, slots: Seq[js.Dynamic]): Table = {
    val byDay = slots.groupBy { s =>
      Instant.parse(s.slotTime.asInstanceOf[String]).atZone(zoneId).toLocalDate
    }
    val tbl = document.createElement("table").asInstanceOf[Table]
    tbl.style.cssText =
      """
      border-collapse: collapse;
      width: 100%;
      background: white;
      table-layout: fixed
      """
    // header
    val hdr = document.createElement("tr").asInstanceOf[TableRow]
    hdr.appendChild(th(""))
    (0 to 6).foreach(d => hdr.appendChild(th(weekStart.plusDays(d).getDayOfWeek.toString.take(3))))
    tbl.appendChild(hdr)
    // rows
    timeStrings.foreach { t =>
      val row = document.createElement("tr").asInstanceOf[TableRow]
      row.appendChild(th(t))
      (0 to 6).foreach { d =>
        val date = weekStart.plusDays(d)
        val list = byDay.getOrElse(date, Nil).filter { s =>
          Instant.parse(s.slotTime.asInstanceOf[String])
            .atZone(zoneId)
            .toLocalTime
            .format(DateTimeFormatter.ofPattern("HH:mm")) == t
        }
        if (list.nonEmpty) {
          val btn = document.createElement("button").asInstanceOf[Button]
          btn.textContent = s"${list.size} available"
          btn.style.cssText =
            "width:100%;background:#4caf50;color:white;border:none;cursor:pointer;padding:4px 0;"
          btn.onclick = (_: dom.MouseEvent) => showSlotListModal(list)
          val cell = document.createElement("td").asInstanceOf[TableCell]
          cell.style.cssText = "border:1px solid #ccc;padding:4px;background:#e0ffe0;text-align:center;"
          cell.appendChild(btn)
          row.appendChild(cell)
        } else {
          val cell = document.createElement("td").asInstanceOf[TableCell]
          cell.textContent = "-"
          cell.style.cssText = "border:1px solid #ccc;padding:4px;color:#999;text-align:center;"
          row.appendChild(cell)
        }
      }
      tbl.appendChild(row)
    }
    tbl
  }

  // ------------
  // MAP VIEW
  // ------------
  private def buildMapViewContent(): Div = {
    val wrapper = document.createElement("div").asInstanceOf[Div]
    wrapper.style.display = "flex"
    wrapper.style.setProperty("flex-direction", "column")
    wrapper.style.setProperty("align-items", "center")
    wrapper.style.setProperty("justify-content", "center")
    wrapper.style.width = "100%"

    // Toggle button for map style
    val toggleBtn = document.createElement("button").asInstanceOf[org.scalajs.dom.html.Button]
    toggleBtn.textContent = "Toggle Map Style"
    toggleBtn.style.margin = "16px"
    toggleBtn.id = "bookingMapToggleBtn"
    val mapDiv = document.createElement("div").asInstanceOf[Div]
    mapDiv.id = "booking-map"
    mapDiv.style.width = "80%"
    mapDiv.style.maxWidth = "900px"
    mapDiv.style.height = "80%"
    mapDiv.style.minHeight = "400px"
    mapDiv.style.border = "2px solid #e0e0e0"
    mapDiv.style.borderRadius = "18px"
    mapDiv.style.boxShadow = "0 2px 12px rgba(0,0,0,0.08)"
    mapDiv.style.margin = "0 auto"

    wrapper.appendChild(toggleBtn)
    wrapper.appendChild(mapDiv)

    // Initialize the map after the div is in the DOM
    dom.window.setTimeout(() => {
      val map = frontend.Leaflet
        .map("booking-map")
        .setView(js.Array(51.5, -0.09), 13) // TODO - CENTRE ON PATIENTS ADDRESS

      val osmLayer = frontend.Leaflet.tileLayer(
        "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
        js.Dynamic.literal(attribution = "&copy; OpenStreetMap contributors")
      )

      val simpleLayer = frontend.Leaflet.tileLayer(
        "https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png",
        js.Dynamic.literal(
          attribution = "&copy; <a href='https://carto.com/'>CARTO</a>",
          subdomains = js.Array("a", "b", "c", "d")
        )
      )

      // Start with OSM
      var currentLayer = osmLayer
      currentLayer.addTo(map)

      // Toggle functionality
      val btn = document.getElementById("bookingMapToggleBtn").asInstanceOf[org.scalajs.dom.html.Button]
      if (btn != null) {
        btn.onclick = (_: dom.MouseEvent) => {
          currentLayer.removeFrom(map)
          currentLayer = if (currentLayer == osmLayer) simpleLayer else osmLayer
          currentLayer.addTo(map)
        }
      }

      // Fetch available slots first
      val slotReq = dom.fetch("/api/slots/list?is_taken=false").toFuture
        .flatMap(_.json().toFuture)
        .map(_.asInstanceOf[js.Array[js.Dynamic]])

      slotReq.foreach { slots =>
        val clinicIdsWithAvailableSlots: Set[String] =
          slots
            .map(slot => slot.clinicId.asInstanceOf[String])
            .toSet

        // Now fetch clinics and add markers with correct icon and popup
        val clinicsReq =
          dom.fetch("/api/clinics?select=clinic_id,name,address,latitude,longitude")
            .toFuture
            .flatMap(_.json().toFuture)
            .map(_.asInstanceOf[js.Array[js.Dynamic]])

        clinicsReq.foreach { clinics =>
          clinics.foreach { clinic =>
            val clinicId = clinic.clinic_id.asInstanceOf[String]
            val isAvailable = clinicIdsWithAvailableSlots.contains(clinicId)

            val iconUrl =
              if (isAvailable) "/images/ClinicMapIconGreen.png"
              else "/images/ClinicMapIconRed.png"

            val icon = frontend.Leaflet.icon(js.Dynamic.literal(
              iconUrl = iconUrl,
              iconSize = js.Array(32, 32), // adjust as needed
              iconAnchor = js.Array(16, 32), // bottom center
              popupAnchor = js.Array(0, -32)
            ))

            val marker = frontend.Leaflet.marker(
              js.Array(clinic.latitude.asInstanceOf[Double], clinic.longitude.asInstanceOf[Double]),
              js.Dynamic.literal(icon = icon)
            )

            val popupText =
              if (isAvailable)
                s"<b>${clinic.name}</b><br>${clinic.address}<br><span style='color:green;font-weight:bold;'>Available Slots</span>"
              else
                s"<b>${clinic.name}</b><br>${clinic.address}<br><span style='color:red;font-weight:bold;'>No available slots</span>"

            marker.bindPopup(popupText)
            marker.addTo(map)
          }
        }
      }
    }, 0)

    wrapper
  }

  // --------------
  // DETAILS MODAL
  // --------------
  private def showSlotListModal(slots: Seq[js.Dynamic]): Unit = {
    val container = document.createElement("div").asInstanceOf[Div]
    container.style.cssText = "max-height:60vh;overflow-y:auto;padding:20px;display:flex;flex-direction:column;gap:8px;"
    slots.foreach { s =>
      val time = Instant.parse(s.slotTime.asInstanceOf[String])
        .atZone(zoneId).format(DateTimeFormatter.ofPattern("HH:mm  dd MMM yyyy"))
      val length = s.slotLength.asInstanceOf[Double].toLong
      val clinicId= s.clinicId.asInstanceOf[String]
      val clinicName = document.createElement("span").asInstanceOf[Span]
      clinicName.textContent = "Loading..."
      fetchClinicDetails(clinicId).foreach(c => clinicName.textContent = c.name)

      val entry = document.createElement("div").asInstanceOf[Div]
      entry.style.cssText = "border:1px solid #ddd;border-radius:4px;padding:8px;display:flex;justify-content:space-between;"
      val info = document.createElement("div").asInstanceOf[Div]
      info.innerHTML = s"<strong>Time:</strong> $time<br/><strong>Length:</strong> $length min<br/><strong>Clinic:</strong> "
      info.appendChild(clinicName)
      val btn = document.createElement("button").asInstanceOf[Button]
      btn.textContent = "Book"
      btn.style.cssText = "background:#4caf50;color:white;border:none;padding:6px 12px;border-radius:4px;cursor:pointer;"
      btn.onclick = (_: dom.MouseEvent) => {
        requestBooking(s)
        dom.window.alert("Requested!")
      }
      entry.appendChild(info)
      entry.appendChild(btn)
      container.appendChild(entry)
    }
    showModal(container)
  }

  // -------------------
  // BOOKING + HELPERS
  // -------------------
  private def requestBooking(slot: js.Dynamic): Unit = {
    val p = js.Dynamic.literal(
      slot_id    = slot.slotId.asInstanceOf[String],
      patient_id = dom.window.localStorage.getItem("userId"),
      clinic_id  = slot.clinicId.asInstanceOf[String]
    )
    val ri = new dom.RequestInit {
      method  = dom.HttpMethod.POST
      headers = new dom.Headers {
        append("Content-Type","application/json")
        Option(dom.window.localStorage.getItem("accessToken")).foreach(t => append("Authorization",s"Bearer $t"))
      }
      body = js.JSON.stringify(p)
    }
    dom.fetch("/api/bookings/request",ri).toFuture.foreach(_ => ())
  }

  private def th(txt: String): TableCell = {
    val c = document.createElement("th").asInstanceOf[TableCell]
    c.textContent = txt
    c.style.cssText = "border:1px solid #999;padding:6px;background:#f0f0f0;"
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
