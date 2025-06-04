

//   def createBooking(date: String, time: String): Unit = {
//     if (date.isEmpty || time.isEmpty) {
//       dom.window.alert("Please select both date and time")
//     } else {  
//       // Valid booking
//     dom.window.alert(s"Booking created for $date at $time")
//         // TODO: implement actual backend POST request here if needed
//       }
//     }
  


package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import org.scalajs.dom.experimental._
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object BookingDashboard {

  val SUPABASE_URL = "https://djkrryzafuofyevgcyic.supabase.co"

  
    def render(): Unit = {
    clearPage()
    document.body.appendChild(createBlankHeader("Booking Dashboard"))

    // Outer container
    val container = document.createElement("div").asInstanceOf[Div]
    container.setAttribute("style",
        """
        |display: flex;
        |flex-direction: column;
        |align-items: flex-start;
        |gap: 15px;
        |margin: 50px auto;
        |max-width: 500px;
        |padding: 20px;
        |border: 1px solid #ccc;
        |border-radius: 10px;
        |box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        """.stripMargin)

    // Helper function for labeled input rows
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

    // Date input
    val dateInput = document.createElement("input").asInstanceOf[Input]
    dateInput.setAttribute("type", "date")
    dateInput.setAttribute("required", "true")
    container.appendChild(createRow("Select date:", dateInput))

    // Time input
    val timeInput = document.createElement("input").asInstanceOf[Input]
    timeInput.setAttribute("type", "time")
    timeInput.setAttribute("min", "09:00")
    timeInput.setAttribute("max", "17:00")
    timeInput.setAttribute("required", "true")
    container.appendChild(createRow("Select time:", timeInput))

    // Clinic select
    val clinicSelect = document.createElement("select").asInstanceOf[Select]
    val clinicOptions = List(
        ("Clinic A", "e77e3084-132b-4d29-a2c9-2f8b18651e74"),
        ("Clinic B", "aeaeef6e-f07b-43c0-a423-8bb7f52c16e3")
    )
    clinicOptions.foreach { case (name, id) =>
        val option = document.createElement("option").asInstanceOf[Option]
        option.textContent = name
        option.setAttribute("value", id)
        clinicSelect.appendChild(option)
    }
    container.appendChild(createRow("Clinic:", clinicSelect))

    // Slot Length input
    val lengthInput = document.createElement("input").asInstanceOf[Input]
    lengthInput.setAttribute("type", "number")
    lengthInput.setAttribute("min", "5")
    lengthInput.setAttribute("value", "30")
    container.appendChild(createRow("Length (minutes):", lengthInput))

    // Booking button
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
        // Hook up your createSlot function here
    })
    container.appendChild(bookButton)

    document.body.appendChild(container)
    }

//   def createSlot(date: String, time: String, clinicId: String, length: Long): Unit = {
//     if (date.isEmpty || time.isEmpty) {
//       dom.window.alert("Please select both date and time.")
//     } else {
//       val slotTime = s"${date}T${time}:00Z"

//       val data = literal(
//         "slot_time" -> slotTime,
//         "clinic_id" -> clinicId,
//         "slot_length" -> length
//       )

//       val requestOptions = literal(
//         method = "POST",
//         headers = js.Dictionary(
//           "Content-Type" -> "application/json",
//           "apikey" -> SUPABASE_ANON_KEY
//         ),
//         body = JSON.stringify(data)
//       ).asInstanceOf[RequestInit]

//       dom.fetch(s"$SUPABASE_URL/rest/v1/slots", requestOptions)
//         .toFuture
//         .flatMap { response =>
//           if (response.ok) {
//             dom.window.alert("Slot created successfully!")
//             response.text().toFuture
//           } else {
//             response.text().toFuture.map { errorText =>
//               dom.window.alert(s"Failed to create slot: $errorText")
//             }
//           }
//         }
//         .recover {
//           case e: Throwable =>
//             dom.window.alert(s"Error: ${e.getMessage}")
//         }
//     }
//   }

}
