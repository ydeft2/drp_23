package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.Div
import scala.scalajs.js
import scala.concurrent.ExecutionContext.Implicits.global


object BookingPage {
  def render(): Unit = {
    Layout.renderPage(
      leftButton = Some(createHomeButton()),
      contentRender = () => {
        document.body.appendChild(buildBookingPage())
      }
    )
  }

  def buildBookingPage(): Div = {
    val container = document.createElement("div").asInstanceOf[Div]
    container.style.width = "100%"
    container.style.margin = "0 auto"
    container.style.padding = "20px"
    container.style.position = "relative"

    // Toggle wrapper for top-right positioning (fixed)
    val toggleWrapper = document.createElement("div").asInstanceOf[Div]
    toggleWrapper.style.position = "fixed"
    toggleWrapper.style.top = "80px"
    toggleWrapper.style.right = "40px"
    toggleWrapper.style.zIndex = "1000" // higher than content
    toggleWrapper.style.background = "transparent" // optional

    // Toggle container (same as before)
    val toggleContainer = document.createElement("div").asInstanceOf[Div]
    toggleContainer.style.display = "flex"
    toggleContainer.style.setProperty("align-items", "center")
    toggleContainer.style.setProperty("justify-content", "center")
    toggleContainer.style.setProperty("gap", "0px")
    toggleContainer.style.background = "#eee"
    toggleContainer.style.borderRadius = "20px"
    toggleContainer.style.overflow = "hidden"
    toggleContainer.style.width = "fit-content"
    toggleContainer.style.boxShadow = "0 2px 8px rgba(0,0,0,0.07)"

    // Labels
    val listLabel = document.createElement("span").asInstanceOf[org.scalajs.dom.html.Span]
    listLabel.textContent = "List View"
    listLabel.style.cursor = "pointer"
    listLabel.style.fontWeight = "bold"
    listLabel.style.padding = "8px 28px"
    listLabel.style.borderRadius = "20px 0 0 20px"
    listLabel.style.transition = "background 0.2s, color 0.2s"
    listLabel.style.setProperty("user-select", "none")

    val mapLabel = document.createElement("span").asInstanceOf[org.scalajs.dom.html.Span]
    mapLabel.textContent = "Map View"
    mapLabel.style.cursor = "pointer"
    mapLabel.style.fontWeight = "bold"
    mapLabel.style.padding = "8px 28px"
    mapLabel.style.borderRadius = "0 20px 20px 0"
    mapLabel.style.transition = "background 0.2s, color 0.2s"
    mapLabel.style.setProperty("user-select", "none")

    // Content area
    val contentArea = document.createElement("div").asInstanceOf[Div]

    def setView(isMap: Boolean): Unit = {
      if (isMap) {
        mapLabel.style.background = "#1976d2"
        mapLabel.style.color = "white"
        listLabel.style.background = "transparent"
        listLabel.style.color = "#333"
        contentArea.innerHTML = ""
        contentArea.appendChild(buildMapViewContent())
      } else {
        listLabel.style.background = "#1976d2"
        listLabel.style.color = "white"
        mapLabel.style.background = "transparent"
        mapLabel.style.color = "#333"
        contentArea.innerHTML = ""
        contentArea.appendChild(buildListViewContent())
      }
    }

    // Initial state
    setView(isMap = false)

    // Clicking labels toggles view
    listLabel.onclick = (_: org.scalajs.dom.MouseEvent) => setView(false)
    mapLabel.onclick = (_: org.scalajs.dom.MouseEvent) => setView(true)

    toggleContainer.appendChild(listLabel)
    toggleContainer.appendChild(mapLabel)
    toggleWrapper.appendChild(toggleContainer)

    // Append toggleWrapper directly to body so it's always fixed
    document.body.appendChild(toggleWrapper)
    container.appendChild(contentArea)

    container
  }

  // Dummy content builders for now
  def buildListViewContent(): Div = {
    val div = document.createElement("div").asInstanceOf[Div]
    div.textContent = "This is the List View content."
    div
  }
  def buildMapViewContent(): Div = {
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

    // Map div
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
}