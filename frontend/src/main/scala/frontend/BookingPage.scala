package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.{Button, Div, Element, Input, Span, Table, TableCell, TableRow}

import scala.scalajs.js
import scala.scalajs.js.Dynamic
import java.time.*
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BookingPage {
  // --- Common state for both views ---
  private val timeStrings = Seq("08:00","09:00","10:00","11:00","12:00", "13:00","14:00","15:00","16:00", "17:00")
  private val zoneId      = ZoneOffset.UTC
  private val displayFmt  = DateTimeFormatter.ofPattern("dd MMM yyyy")

  // Filters used only by list view:
  private var clinicFilter = ""
  private var fromFilter   = Option.empty[String]
  private var toFilter     = Option.empty[String]

  private var isMap: Boolean = _
  private var listViewClientIdFilter: Option[String] = _

  // Content area under toggle
  private val listLabel = document.createElement("span").asInstanceOf[Span]
  private val mapLabel = document.createElement("span").asInstanceOf[Span]
  private val contentArea = document.createElement("div").asInstanceOf[Div]

  // view switch logic
  private def renderView(): Unit = {
    // style labels
    if (isMap) {
      mapLabel.style.background = "#1976d2";
      mapLabel.style.color = "white"
      listLabel.style.background = "transparent";
      listLabel.style.color = "#333"
    } else {
      listLabel.style.background = "#1976d2";
      listLabel.style.color = "white"
      mapLabel.style.background = "transparent";
      mapLabel.style.color = "#333"
    }
    contentArea.innerHTML = ""
    contentArea.appendChild(
      if (isMap) buildMapViewContent()
      else buildListViewContent(listViewClientIdFilter)
    )
  }


  // The one and only `render()`
  def render(): Unit = {
    Layout.renderPage(
      leftButton    = Some(createHomeButton()),
      contentRender = () => {
        val root = document.createElement("div").asInstanceOf[Div]
          // make everything inside root centered at 90% / max-1200px
        root.style.cssText =
        """
          display: block;
          width: 90%;
          max-width: 1200px;
          margin: 0 auto 0 auto;
        """

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
        root.appendChild(toggleWrapper)


        // FILTER BAR
        val fbar = document.createElement("div").asInstanceOf[Div]
        fbar.style.cssText =
        """
          gap:12px;
          margin:0 auto 16px auto;
          width:100%;
        """

        val clinicInput = document.createElement("input").asInstanceOf[Input]
        clinicInput.placeholder = "Clinic info contains…"
        clinicInput.oninput = (_: dom.Event) => {
          clinicFilter = clinicInput.value.trim
          renderView()
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
        fromInput.defaultValue = fromFilter.getOrElse("")
        fromInput.onchange = (_: dom.Event) => {
          fromFilter = Option(fromInput.value).filter(_.nonEmpty)
          renderView()
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
        toInput.defaultValue = toFilter.getOrElse("")
        toInput.onchange = (_: dom.Event) => {
          toFilter = Option(toInput.value).filter(_.nonEmpty)
          renderView()
        }
        fbar.appendChild(toInput)

        root.appendChild(fbar)


        // Content area under toggle
//        val contentArea = document.createElement("div").asInstanceOf[Div]
        contentArea.style.marginTop = "30px"  // leave room for toggle
        contentArea.style.width = "100%"
        contentArea.style.marginLeft = "auto"
        contentArea.style.marginRight = "auto"
        contentArea.style.padding = "0 0"
        root.appendChild(contentArea)

        def toggleViewClicked(newIsMap: Boolean): Unit = {
          listViewClientIdFilter = None
          isMap = newIsMap
          renderView()
        }

        listLabel.onclick = (_: dom.MouseEvent) => { toggleViewClicked(false) }
        mapLabel .onclick = (_: dom.MouseEvent) => { toggleViewClicked(true) }

        // initial
        isMap = false
        listViewClientIdFilter = None

        document.body.appendChild(root)
        renderView()
      }
    )
  }

  // ------------------------
  // LIST VIEW IMPLEMENTATION
  // ------------------------
  private def buildListViewContent(clinic_id: Option[String]): Div = {
    // compute week bounds
    val today        = LocalDate.now(Clock.systemUTC())
    val daysFromSun  = today.getDayOfWeek.getValue % 7
    val weekStart0   = today.minusDays(daysFromSun.toLong)
    var currentStart = weekStart0
    val lastStart    = weekStart0.plusWeeks(52)
    val given_clinic_id = clinic_id.isDefined

    // page elements
    val container    = document.createElement("div").asInstanceOf[Div]
    container.style.cssText =
      """
        width=100% ; height=100%; padding = 0 0;
      """

    // Clinic specific header (for when coming back from map view)
    clinic_id.foreach { cid =>
      val title = document.createElement("h2").asInstanceOf[Element]
      title.textContent = "Loading clinic timetable..." // if necessary
      title.style.cssText = "margin-bottom:12px"
      container.appendChild(title)

      fetchClinicDetails(cid).foreach { clinic =>
        title.textContent = s"Timetable for ${clinic.name}"
      }

    }

//    val tableHolder = document.createElement("div").asInstanceOf[Div]
//    tableHolder

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

    def drawWeek(jumpToFirst: Boolean): Unit = {
      tableHolder.innerHTML = "<em>Loading…</em>"

      /*
      IMPORTANT - if currentStart is seemingly before the actual week we as humans are living in...
      We can add the the following defensive code:

      if (!jumpToFirst && currentStart.isBefore(weekStart0)) currentStart = weekStart0

      tableHolder.innerHTML = "<em>Loading...</em>"

       */

//      fetchSlots().foreach { slots =>
//        tableHolder.innerHTML = ""
//        tableHolder.appendChild(buildTable(currentStart, slots))
//      }
      val slotsFuture = clinic_id match {
        case Some(cid) => fetchSlotsByClinicId(cid)
        case None => fetchSlots()
      }

      slotsFuture.foreach { slots =>
        val byDay = slots.groupBy { s =>
          Instant.parse(s.slotTime.asInstanceOf[String]).atZone(zoneId).toLocalDate
        }
        if (jumpToFirst) { // jump to first bug fix
          val startDate = byDay.keySet.min
          val daysFromSun = startDate.getDayOfWeek.getValue % 7
          val candidateDate = startDate.minusDays(daysFromSun.toLong)
          currentStart = if (candidateDate.isBefore(weekStart0)) weekStart0 else candidateDate
        }
        updateNav()
        tableHolder.innerHTML = ""
        tableHolder.appendChild(buildTable(currentStart, byDay))
      }
    }

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

    prevBtn.onclick = (_: dom.MouseEvent) => if (currentStart.isAfter(weekStart0)) { currentStart = currentStart.minusWeeks(1); drawWeek(false) }
    nextBtn.onclick = (_: dom.MouseEvent) => if (currentStart.isBefore(lastStart)) { currentStart = currentStart.plusWeeks(1); drawWeek(false) }

    drawWeek(true) // TODO: used to say given_clinic_id
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

  private def fetchSlots(): Future[Seq[js.Dynamic]] = {
    val base  = "/api/slots/list"
    val parts = scala.collection.mutable.ArrayBuffer.empty[String]

    parts += "is_taken=false"
    if (clinicFilter.nonEmpty) parts += s"clinic_info=${encode(clinicFilter)}"
    fromFilter.foreach(f =>
      val inst  = Instant.parse(f + ":00Z")
      val lower = if (inst.isBefore(Instant.now())) Instant.now() else inst
      parts += s"slot_time_gte=${encode(lower.toString)}"
    )
    toFilter.foreach(t =>
      parts += s"slot_time_lte=${encode(t + ":00Z")}")
    parts += "limit=100"; parts += "offset=0"
    val url = base + "?" + parts.mkString("&")
    dom.fetch(url).toFuture.flatMap(_.json().toFuture).map(_.asInstanceOf[js.Array[js.Dynamic]].toSeq)
  }

  private def fetchSlotsByClinicId(clinicId: String): Future[Seq[js.Dynamic]] = {
    val base = s"/api/slots/list"
    val parts = scala.collection.mutable.ArrayBuffer.empty[String]

    parts += s"clinic_id=$clinicId"
    parts += "is_taken=false"
    if (clinicFilter.nonEmpty) parts += s"clinic_info=${encode(clinicFilter)}"
    fromFilter.foreach(f => parts += s"slot_time_gte=${encode(f + ":00Z")}")
    toFilter.foreach(t => parts += s"slot_time_lte=${encode(t + ":00Z")}")
    parts += "limit=100";
    parts += "offset=0"
    val url = base + "?" + parts.mkString("&")
    dom.fetch(url).toFuture.flatMap(_.json().toFuture).map(_.asInstanceOf[js.Array[js.Dynamic]].toSeq)
  }

  // Builds the 7×N table; in each cell we count available slots and
  // show that number. Clicking opens a modal with the detailed list.
  private def buildTable(weekStart: LocalDate, byDay:  Map[LocalDate, Seq[js.Dynamic]]): Table = {

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
    (0 to 6).foreach { d =>
      hdr.appendChild(th(weekStart.plusDays(d).getDayOfWeek.toString.take(3)))
    }
    tbl.appendChild(hdr)

    // rows
    timeStrings.foreach { t =>
      val row = document.createElement("tr").asInstanceOf[TableRow]
      row.appendChild(th(t))

      (0 to 6).foreach { d =>
        val date = weekStart.plusDays(d)
        // all slots at that day + time
        val list = byDay
          .getOrElse(date, Nil)
          .filter { s =>
            Instant.parse(s.slotTime.asInstanceOf[String])
              .atZone(zoneId)
              .toLocalTime
              .format(DateTimeFormatter.ofPattern("HH:mm")) == t
          }

        if (list.nonEmpty) {
          val btn = document.createElement("button").asInstanceOf[Button]
          btn.textContent = s"${list.size} available"
          btn.style.cssText =
            """
              width: 100%; height: 100%;
              background: #4caf50;
              color: white;
              border: none;
              cursor: pointer;
              padding: 4px 0;      /* only a little vertical padding */
            """
          btn.onclick = (_: dom.MouseEvent) => showSlotListModal(list)

          val cell = document.createElement("td").asInstanceOf[TableCell]
          cell.style.cssText = "border:1px solid #ccc;padding:4px;background:#e0ffe0;text-align: center;" /* dropped padding */
          cell.appendChild(btn)
          row.appendChild(cell)
        } else {
          val cell = document.createElement("td").asInstanceOf[TableCell]
          cell.textContent = "-"
          cell.style.cssText = "border:1px solid #ccc;padding:4px;color:#999;text-align: center;"
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
//    wrapper.style.display = "flex"
//    wrapper.style.setProperty("flex-direction", "column")
//    wrapper.style.setProperty("align-items", "center")
//    wrapper.style.setProperty("justify-content", "center")
    wrapper.style.width = "100%"
    wrapper.style.height = "100%"
    wrapper.style.padding = "16px 0"

    // Toggle button for map style
    val toggleBtn = document.createElement("button").asInstanceOf[org.scalajs.dom.html.Button]
    toggleBtn.textContent = "Toggle Map Style"
    toggleBtn.style.margin = "16px"
    toggleBtn.id = "bookingMapToggleBtn"
    val mapDiv = document.createElement("div").asInstanceOf[Div]
    mapDiv.id = "booking-map"
    mapDiv.style.width = "100%"
//    mapDiv.style.maxWidth = "900px"
    mapDiv.style.height = "100%"
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
      val slotReq = fetchSlots()

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
              iconSize = js.Array(48, 48), // adjust as needed
              iconAnchor = js.Array(24, 48), // bottom center
              popupAnchor = js.Array(0, -48)
            ))

            // 1) Create the popup container
            val popupDiv = document.createElement("div").asInstanceOf[Div]
            popupDiv.style.cssText = "display:flex;flex-direction:column;gap:4px;"

            // 2) Add clinic name & address
            val title = document.createElement("strong")
            title.textContent = clinic.name.asInstanceOf[String]
            popupDiv.appendChild(title)

            val addr = document.createElement("div").asInstanceOf[Div]
            addr.textContent = clinic.address.asInstanceOf[String]
            popupDiv.appendChild(addr)

            // 4) Your new button
            val bookBtn = document.createElement("button").asInstanceOf[org.scalajs.dom.html.Button]
            bookBtn.textContent = if (isAvailable) "Available Slots" else "No available slots"
            bookBtn.style.cssText = """
              margin-top:2px;
              padding:2px 2;
              color:white;
              border:none;
              border-radius:4px;
              cursor:pointer;
            """
            bookBtn.style.background = if (isAvailable) "#4caf50" else "red"
            bookBtn.disabled = !isAvailable  // disable if no slots
            bookBtn.onclick = (_: dom.MouseEvent) => {
//              buildListViewContent(Some(clinicId)) /// THIS LINE DOESN'T CHANGE THE VIEW
              listViewClientIdFilter = Some(clinicId)
              isMap = false
              renderView()
            }
            popupDiv.appendChild(bookBtn)

            // 5) Bind the popup DOM node
            val marker = frontend.Leaflet.marker(
              js.Array(clinic.latitude.asInstanceOf[Double], clinic.longitude.asInstanceOf[Double]),
              js.Dynamic.literal(icon = icon)
            )
            marker.bindPopup(popupDiv)
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
  // Pop up a scrollable modal listing each slot with its details + a Book button
  private def showSlotListModal(slots: Seq[js.Dynamic]): Unit = {
    val container = document.createElement("div").asInstanceOf[Div]
    container.style.cssText =
      "max-height:60vh;overflow-y:auto;padding:50px;display:flex;flex-direction:column;gap:8px;"
    slots.foreach { s =>
      // parse & re‐format
      val timeInstant = Instant.parse(s.slotTime.asInstanceOf[String])
      val time = timeInstant
        .atZone(zoneId)
        .format(DateTimeFormatter.ofPattern("HH:mm  dd MMM yyyy"))

      val length = s.slotLength.asInstanceOf[Double].toLong
      val clinic = s.clinicId.asInstanceOf[String]
      // Placeholder for clinic name
      val clinicNameSpan = document.createElement("span").asInstanceOf[dom.html.Span]
      clinicNameSpan.textContent = "Loading clinic..."

      fetchClinicDetails(clinic).foreach { clinic =>
        clinicNameSpan.textContent = clinic.name
      }

      val entry = document.createElement("div").asInstanceOf[Div]
      entry.style.cssText = "padding:8px;border:1px solid #ddd;border-radius:4px;display:flex;justify-content:space-between;"

      val info = document.createElement("div").asInstanceOf[Div]
      info.innerHTML = s"""<strong>Time:</strong> $time<br/>
                          |<strong>Length:</strong> $length min<br/>
                          |<strong>Clinic:</strong>
                       """.stripMargin
      //      info.querySelector("strong + br + strong + br + span + strong").appendChild(clinicNameSpan)
      info.appendChild(clinicNameSpan)

      entry.appendChild(info)

      val bookBtn = document.createElement("button").asInstanceOf[Button]
      bookBtn.textContent = "Book"
      bookBtn.style.cssText =
        "background:#4caf50;color:white;border:none;padding:6px 12px;border-radius:4px;cursor:pointer;"
      bookBtn.onclick = (_: dom.MouseEvent) => {
        requestBooking(s)
        dom.window.alert("Requested!") // you could also refresh
      }

      entry.appendChild(bookBtn)
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