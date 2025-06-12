package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.Div
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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
    val back = document.createElement("button").asInstanceOf[dom.html.Button]
    back.textContent = "← Back"
    back.onclick = (_: dom.MouseEvent) => HomePage.render()
    back.style.margin = "16px"

    val toggleBtn = document.createElement("button").asInstanceOf[dom.html.Button]
    toggleBtn.id = "toggleMapBtn"
    toggleBtn.textContent = "Toggle Map Style"
    toggleBtn.style.margin = "16px"

    Layout.renderPage(
      leftButton = Some(back),
      rightButton = Some(toggleBtn),
      contentRender = () => {

        val mapDiv = document.createElement("div").asInstanceOf[Div]
        mapDiv.id = "map"
        mapDiv.style.width = "80%"
        mapDiv.style.maxWidth = "900px"
        mapDiv.style.height = "80%"
        mapDiv.style.minHeight = "400px"
        mapDiv.style.border = "2px solid #e0e0e0"
        mapDiv.style.borderRadius = "18px"
        mapDiv.style.boxShadow = "0 2px 12px rgba(0,0,0,0.08)"

        document.body.appendChild(mapDiv)

        // Give the browser a moment to apply styles
        dom.window.setTimeout(() => initMap(), 0)
      }
    )
  }

  private def initMap(): Unit = {
    val map = Leaflet
      .map("map")
      .setView(js.Array(51.5, -0.09), 13)

    // Define both tile layers
    val osmLayer = Leaflet.tileLayer(
      "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
      js.Dynamic.literal(attribution = "&copy; OpenStreetMap contributors")
    )

    val simpleLayer = Leaflet.tileLayer(
      "https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png",
      js.Dynamic.literal(
        attribution = "&copy; <a href='https://carto.com/'>CARTO</a>",
        subdomains = js.Array("a", "b", "c", "d")
      )
    )

    // Start with OSM
    var currentLayer = osmLayer
    currentLayer.addTo(map)

    // Add toggle functionality
    val toggleBtn =document.getElementById("toggleMapBtn").asInstanceOf[dom.html.Button]
    if (toggleBtn != null) {
      toggleBtn.onclick = (_: dom.MouseEvent) => {
        currentLayer.removeFrom(map)
        currentLayer = if (currentLayer == osmLayer) simpleLayer else osmLayer
        currentLayer.addTo(map)
      }
    }

    val interestsReq = {
      val init = js.Dynamic.literal(
        method = "GET",
        headers = js.Dynamic.literal("Content-Type" -> "application/json")
      ).asInstanceOf[dom.RequestInit]
      dom.fetch("/api/interests?patient_id=" + jsGlobal.localStorage.getItem("userId"), init)
        .toFuture
        .flatMap(_.json().toFuture)
        .map(_.asInstanceOf[js.Array[String]])
    }

    val clinicsReq =
      dom.fetch("/api/clinics?select=clinic_id,name,address,latitude,longitude")
        .toFuture
        .flatMap(_.json().toFuture)
        .map(_.asInstanceOf[js.Array[js.Dynamic]])

    for {
      interests <- interestsReq
      clinics   <- clinicsReq
    } {
      // Store markers by clinicId
      val markerMap = scala.collection.mutable.Map.empty[String, js.Dynamic]
      // Store current interests in a mutable set for easy update
      val interestsSet = scala.collection.mutable.Set(interests.toSeq: _*)

      def updateMarkerAndPopup(clinic: js.Dynamic): Unit = {
        val clinicId = clinic.clinic_id.asInstanceOf[String]
        val alreadyInterested = interestsSet.contains(clinicId)
        val iconUrl = if (alreadyInterested) "/images/ClinicMapIconGreen.png" else "/images/ClinicMapIconOrange.png"
        val toothIcon = Leaflet.icon(js.Dynamic.literal(
          iconUrl = iconUrl,
          iconSize = js.Array(70, 70),
          iconAnchor = js.Array(20, 40)
        ))
        val marker = markerMap(clinicId)
        marker.setIcon(toothIcon)
        val name = clinic.name.asInstanceOf[String]
        val address = clinic.address.asInstanceOf[UndefOr[String]].getOrElse("")
        val btnLabel = if (alreadyInterested) "Unregister Interest" else "Register Interest"
        val popupHtml =
          s"""
            <strong>${name}</strong><br/>
            <em>${address}</em><br/>
            <button id="interest-${clinicId}">${btnLabel}</button>
            <button id="chat-${clinicId}" style="margin-left:6px;">Chat</button>
            """

        marker.setPopupContent(popupHtml)

        marker.on("popupopen", (_: js.Dynamic) => {
          val interestBtn = document.getElementById(s"interest-$clinicId")
          val chatBtn     = document.getElementById(s"chat-$clinicId")

          if (interestBtn != null)
            interestBtn.addEventListener("click", (_: dom.MouseEvent) => {
              val isInterested = interestsSet.contains(clinicId)
              if (isInterested) unregisterInterest(clinicId, clinic)
              else              registerInterest(clinicId, clinic)
              marker.closePopup()
            })

          if (chatBtn != null)
            chatBtn.addEventListener("click", (_: dom.MouseEvent) => {
              println("Opening chat for clinic: " + clinicId)
              ChatPage.createChat(clinicId, name)
            })
        })
      }

      clinics.filter(c =>
        c.latitude.asInstanceOf[UndefOr[Double]].isDefined &&
          c.longitude.asInstanceOf[UndefOr[Double]].isDefined
      ).foreach { c =>
        val lat = c.latitude.asInstanceOf[Double]
        val lon = c.longitude.asInstanceOf[Double]
        val clinicId = c.clinic_id.asInstanceOf[String]
        val alreadyInterested = interestsSet.contains(clinicId)
        val iconUrl = if (alreadyInterested) "/images/ClinicMapIconGreen.png" else "/images/ClinicMapIconOrange.png"
        val toothIcon = Leaflet.icon(js.Dynamic.literal(
          iconUrl = iconUrl,
          iconSize = js.Array(70, 70),
          iconAnchor = js.Array(20, 40)
        ))
        val marker = Leaflet
          .marker(js.Array(lat, lon), js.Dynamic.literal(icon = toothIcon))
          .addTo(map)
        markerMap(clinicId) = marker

        val name = c.name.asInstanceOf[String]
        val address = c.address.asInstanceOf[UndefOr[String]].getOrElse("")
        val btnLabel = if (alreadyInterested) "Unregister Interest" else "Register Interest"
        val popupHtml =
          s"""
              <strong>${name}</strong><br/>
              <em>${address}</em><br/>
              <button id="interest-${clinicId}">${btnLabel}</button>
              <button id="chat-${clinicId}" style="margin-left:6px;">Chat</button>
            """

        marker.bindPopup(popupHtml)

        marker.on("popupopen", (_: js.Dynamic) => {
          val btnId = s"interest-$clinicId"
          val interestBtn = document.getElementById(btnId)
          val chatBtn     = document.getElementById(s"chat-$clinicId")
          if (interestBtn != null) interestBtn.addEventListener("click", (_: dom.MouseEvent) => {
            val isInterested = interestsSet.contains(clinicId)
            if (isInterested) {
              unregisterInterest(clinicId, c)
            } else {
              registerInterest(clinicId, c)
            }
            marker.closePopup()
          })

          if (chatBtn != null) chatBtn.addEventListener("click", (_: dom.MouseEvent) => {
            println("Opening chat for clinic: " + clinicId)
            ChatPage.createChat(clinicId, name)
          })
        })
      }
      Spinner.hide()

      // Redefine register/unregister to update UI
      def registerInterest(clinicId: String, clinic: js.Dynamic): Unit = {
        val payload = js.Dynamic.literal(
          clinic_id = clinicId,
          patient_id = jsGlobal.localStorage.getItem("userId")
        )
        val init = js.Dynamic.literal(
          method  = "POST",
          headers = js.Dynamic.literal("Content-Type" -> "application/json"),
          body    = JSON.stringify(payload)
        ).asInstanceOf[dom.RequestInit]
        dom.fetch("/api/interests", init)
          .toFuture
          .flatMap(_.text().toFuture)
          .foreach { _ =>
            interestsSet += clinicId
            updateMarkerAndPopup(clinic)
          }
      }

      def unregisterInterest(clinicId: String, clinic: js.Dynamic): Unit = {
        val payload = js.Dynamic.literal(
          clinic_id = clinicId,
          patient_id = jsGlobal.localStorage.getItem("userId")
        )
        val init = js.Dynamic.literal(
          method  = "DELETE",
          headers = js.Dynamic.literal("Content-Type" -> "application/json"),
          body    = JSON.stringify(payload)
        ).asInstanceOf[dom.RequestInit]
        dom.fetch("/api/interests", init)
          .toFuture
          .flatMap(_.text().toFuture)
          .foreach { _ =>
            interestsSet -= clinicId
            updateMarkerAndPopup(clinic)
          }
      }
    }
  }
}
