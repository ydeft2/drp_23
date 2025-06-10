package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.Div
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.Dynamic.{ literal => jsObj, global => jsGlobal }

object MapPage {

  def render(): Unit = {
    // Clear out previous content
    document.body.innerHTML = ""

    // Back button
    val back = document.createElement("button").asInstanceOf[dom.html.Button]
    back.textContent = "← Back"
    back.onclick = (_: dom.MouseEvent) => HomePage.render()
    back.style.margin = "16px"
    document.body.appendChild(back)

    // Map container
    val mapDiv = document.createElement("div").asInstanceOf[Div]
    mapDiv.id = "map"
    mapDiv.style.width  = "100vw"
    mapDiv.style.height = "90vh"
    document.body.appendChild(mapDiv)

    // Delay initializing until CSS/layout applied
    dom.window.setTimeout(() => initMap(), 100)
  }

  private def initMap(): Unit = {
    // Initialize Leaflet map at a default center/zoom
    val map = jsGlobal.L.map("map").callMethod("setView", js.Array(js.Array(51.5, -0.09), 13))

    // Add OpenStreetMap tiles
    jsGlobal.L.tileLayer(
      "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
      jsObj(attribution = "&copy; OpenStreetMap contributors")
    ).callMethod("addTo", js.Array(map))

    // Fetch clinics with lat/lng
    dom.fetch("/api/clinics?select=clinic_id,name,address,latitude,longitude")
      .toFuture
      .flatMap(_.json().toFuture)
      .foreach { data =>
        val clinics = data.asInstanceOf[js.Array[js.Dynamic]]
        clinics.foreach { c =>
          val lat     = c.latitude.asInstanceOf[Double]
          val lon     = c.longitude.asInstanceOf[Double]
          val name    = c.name.asInstanceOf[String]
          val address = c.address.asInstanceOf[String]
          val id      = c.clinic_id.asInstanceOf[String]

          // Tooth icon
          val toothIcon = jsGlobal.L.icon(jsObj(
            iconUrl    = "/images/tooth.png",
            iconSize   = js.Array(40, 40),
            iconAnchor = js.Array(20, 40)
          ))

          val marker = jsGlobal.L.marker(js.Array(lat, lon), jsObj(icon = toothIcon))
            .callMethod("addTo", js.Array(map))

          // Popup HTML with Register Interest button
          val popupHtml =
            s"""
              <strong>$name</strong><br/>
              $address<br/>
              <button id="interest-$id">Register Interest</button>
            """
          marker.callMethod("bindPopup", js.Array(popupHtml))

          // Hook up button when popup opens
          marker.callMethod("on", js.Array("popupopen", (e: js.Dynamic) => {
            val btn = document.getElementById(s"interest-$id")
            if (btn != null) {
              btn.addEventListener("click", (_: dom.MouseEvent) => registerInterest(id))
            }
          }))
        }
        Spinner.hide()
      }
  }

  private def registerInterest(clinicId: String): Unit = {
    val payload = jsObj(clinic_id = clinicId)
    dom.fetch(
        "/api/interests",
        jsObj(
          method  = "POST",
          headers = jsObj("Content-Type" -> "application/json"),
          body    = JSON.stringify(payload)
        ).asInstanceOf[dom.RequestInit]
      ).toFuture
      .flatMap(_.text().toFuture)
      .foreach { _ =>
        dom.window.alert("Interest registered!")
      }
  }
}
