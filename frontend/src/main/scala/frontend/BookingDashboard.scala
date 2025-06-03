package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation._

object BookingDashboard {

  def render(): Unit = {
    clearPage()

    document.body.appendChild(createBlankHeader("Booking Dashboard"))

    val container = document.createElement("div").asInstanceOf[Div]
    container.style.margin = "20px"
    container.style.maxWidth = "400px"
    container.style.display = "flex"
    container.style.marginTop = "80px" // leave space for header


    // Date input
    val dateLabel = document.createElement("label")
    dateLabel.textContent = "Select date: "
    val dateInput = document.createElement("input").asInstanceOf[Input]
    dateInput.`type` = "date"
    dateInput.required = true
    dateLabel.appendChild(dateInput)

    // Time input
    val timeLabel = document.createElement("label")
    timeLabel.textContent = " Select time: "
    val timeInput = document.createElement("input").asInstanceOf[Input]
    timeInput.`type` = "time"
    timeInput.min = "09:00"
    timeInput.max = "17:00"
    timeInput.required = true
    timeLabel.appendChild(timeInput)

    // Booking button
    val bookButton = document.createElement("button").asInstanceOf[Button]
    bookButton.textContent = "Create Booking"
    bookButton.style.marginTop = "15px"
    bookButton.addEventListener("click", { (_: dom.MouseEvent) =>
      createBooking(dateInput.value, timeInput.value)
    })

    container.appendChild(dateLabel)
    container.appendChild(timeLabel)
    container.appendChild(bookButton)

    document.body.appendChild(container)
  }

  def createBooking(date: String, time: String): Unit = {
    if (date.isEmpty || time.isEmpty) {
      dom.window.alert("Please select both date and time")
    } else {  
      // Valid booking
    dom.window.alert(s"Booking created for $date at $time")
        // TODO: implement actual backend POST request here if needed
      }
    }
  
  // Utilities, copy from your existing code or implement here

  def clearPage(): Unit = {
    document.body.innerHTML = ""
  }

  def createBlankHeader(name: String): Div = {
    val header = document.createElement("div").asInstanceOf[Div]
    header.style.backgroundColor = "purple"
    header.style.color = "white"
    header.style.padding = "10px"
    header.style.textAlign = "center"
    header.style.position = "fixed"
    header.style.top = "0"
    header.style.left = "0"
    header.style.right = "0"
    header.style.height = "50px"
    header.style.zIndex = "1"

    val title = document.createElement("div")
    title.textContent = name
    title.asInstanceOf[Div].style.fontSize = "20px"
    title.asInstanceOf[Div].style.fontWeight = "bold"

    header.appendChild(title)

    header
  }

  
}
