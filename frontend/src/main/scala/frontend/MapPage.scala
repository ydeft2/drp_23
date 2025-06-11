package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.Div
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.Dynamic.{ literal => jsObj, global => jsGlobal }

import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSGlobal


@js.native
@JSGlobal("L")
object Leaflet extends js.Object {
  def map(id: String): js.Dynamic = js.native

  def tileLayer(url: String, options: js.Dynamic): js.Dynamic = js.native

  def icon(options: js.Dynamic): js.Dynamic = js.native

  def marker(latlng: js.Array[Double], options: js.Dynamic): js.Dynamic = js.native
}

object MapPage {
  def render(): Unit = {
    document.body.innerHTML = ""
    val back = document.createElement("button").asInstanceOf[dom.html.Button]
    back.textContent = "← Back"
    back.onclick = (_: dom.MouseEvent) => HomePage.render()
    back.style.margin = "16px"
    document.body.appendChild(back)

    val mapDiv = document.createElement("div").asInstanceOf[Div]
    mapDiv.id = "map"
    mapDiv.style.width = "100vw"
    mapDiv.style.height = "90vh"
    document.body.appendChild(mapDiv)

    // Give the browser a moment to apply styles
    dom.window.setTimeout(() => initMap(), 0)
  }

  private def initMap(): Unit = {
    // 1) create the map and immediately set view
    val map = Leaflet
      .map("map")
      .setView(js.Array(51.5, -0.09), 13)

    // 2) add OSM tiles
    Leaflet
      .tileLayer(
        "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
        js.Dynamic.literal(attribution = "&copy; OpenStreetMap contributors")
      )
      .addTo(map)

    // 3) fetch clinics
    dom.fetch("/api/clinics?select=clinic_id,name,address,latitude,longitude")
      .toFuture
      .flatMap(_.json().toFuture)
      .foreach { raw =>
        val clinics = raw.asInstanceOf[js.Array[js.Dynamic]]
        clinics.filter(c =>
          c.latitude.asInstanceOf[UndefOr[Double]].isDefined &&
            c.longitude.asInstanceOf[UndefOr[Double]].isDefined
        ).foreach { c =>
          val lat = c.latitude.asInstanceOf[Double]
          val lon = c.longitude.asInstanceOf[Double]
          val name = c.name.asInstanceOf[String]
          val address = c.address.asInstanceOf[UndefOr[String]].getOrElse("")

          // 4) drop tooth icon markers
          val toothIcon = Leaflet.icon(js.Dynamic.literal(
            // TODO: we want to
            iconUrl = "/images/DentanaLogoHappy.png",
            iconSize = js.Array(40, 40),
            iconAnchor = js.Array(20, 40)
          ))

          val marker = Leaflet
            .marker(js.Array(lat, lon), js.Dynamic.literal(icon = toothIcon))
            .addTo(map)

          // 5) bind popup with a button
          val popupHtml =
            s"""
                <strong>${name}</strong><br/>
                <em>${address}</em><br/>
                <button id="interest-$c.clinic_id">Register Interest</button>
              """
          marker.bindPopup(popupHtml)

          marker.on("popupopen", (_: js.Dynamic) => {
            val btnId = s"interest-${c.clinic_id.toString}"
            val btn = document.getElementById(btnId)
            if (btn != null) btn.addEventListener("click", (_: dom.MouseEvent) => {
              registerInterest(c.clinic_id.asInstanceOf[String])
            })
          })
        }
        Spinner.hide()
      }
  }

  private def registerInterest(clinicId: String): Unit = {
    val payload = js.Dynamic.literal(clinic_id = clinicId)

    // Build a JS literal and cast it to RequestInit
    val init = js.Dynamic.literal(
      method  = "POST",
      headers = js.Dynamic.literal("Content-Type" -> "application/json"),
      body    = JSON.stringify(payload)
    ).asInstanceOf[dom.RequestInit]

    dom.fetch("/api/interests", init)
      .toFuture
      .flatMap(_.text().toFuture)
      .foreach(_ => dom.window.alert("Interest registered!"))
  }
}
