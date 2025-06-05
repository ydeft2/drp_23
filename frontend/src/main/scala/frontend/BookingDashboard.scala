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

  val SUPABASE_URL = "https://djkrryzafuofyevgcyic.supabase.co"
  var dateInput: Input = _
  var timeInput: Input = _
  var clinicSelect: Select = _
  var lengthInput: Input = _

  var slotsContainer: Div = _
  // Mapping of clinic id to clinic 
  var clinicMap: Map[String, String] = Map.empty


  
  def render(): Unit = {
    clearPage()
    document.body.appendChild(createSubpageHeader("Booking Dashboard"))

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
    dateInput = document.createElement("input").asInstanceOf[Input]
    dateInput.setAttribute("type", "date")
    dateInput.setAttribute("required", "true")
    container.appendChild(createRow("Select date:", dateInput))

    // Time input
    timeInput = document.createElement("input").asInstanceOf[Input]
    timeInput.setAttribute("type", "time")
    timeInput.setAttribute("required", "true")
    container.appendChild(createRow("Select time:", timeInput))

    // Clinic select (empty initially, to be populated dynamically)
    clinicSelect = document.createElement("select").asInstanceOf[Select]
    container.appendChild(createRow("Clinic:", clinicSelect))

    // Slot Length input
    lengthInput = document.createElement("input").asInstanceOf[Input]
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
      createSlot(
        dateInput.value,
        timeInput.value,
        clinicSelect.value,
        lengthInput.value.toLong
      )
    })
    container.appendChild(bookButton)

    document.body.appendChild(container)

        // Slots container
    slotsContainer = document.createElement("div").asInstanceOf[Div]
    slotsContainer.setAttribute("style",
      """
        |margin-top: 30px;
        |max-width: 500px;
        |border-top: 1px solid #ccc;
        |padding-top: 20px;
        """.stripMargin)
    document.body.appendChild(slotsContainer)

    fetchClinics(clinicSelect).foreach { _ =>
      fetchAndDisplaySlots()
    }


  }

  def fetchClinics(clinicSelect: Select): scala.concurrent.Future[Unit] = {
    val clinicsApiUrl = s"$SUPABASE_URL/rest/v1/clinics?select=clinic_id,name"

    val requestHeaders = new dom.Headers()
    requestHeaders.append("apikey", SUPABASE_ANON_KEY)
    requestHeaders.append("Authorization", s"Bearer $SUPABASE_ANON_KEY")
    requestHeaders.append("Content-Type", "application/json")

    val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.GET
      headers = requestHeaders
    }

    dom.fetch(clinicsApiUrl, requestInit).toFuture
      .flatMap { response =>
        if (response.ok) response.json().toFuture
        else {
          dom.window.alert(s"Failed to fetch clinics: ${response.statusText}")
          scala.concurrent.Future.failed(new Exception("Fetch clinics failed"))
        }
      }
      .map { json =>
        val clinics = json.asInstanceOf[js.Array[js.Dynamic]]
        clinicSelect.innerHTML = "" // Clear old options
        clinics.foreach { clinic =>
          val clinicId = clinic.clinic_id.asInstanceOf[String]
          val clinicName = clinic.name.asInstanceOf[String]

          val option = document.createElement("option").asInstanceOf[Option]
          option.textContent = clinicName
          option.value = clinicId
          clinicSelect.appendChild(option)
          clinicMap += (clinicId -> clinicName)
        }
      }
  }


  def fetchAndDisplaySlots(): Unit = {
    val slotsApiUrl = s"$SUPABASE_URL/rest/v1/slots?select=slot_id,slot_time,clinic_id,slot_length,is_taken"

    val requestHeaders = new dom.Headers()
    requestHeaders.append("apikey", SUPABASE_ANON_KEY)
    requestHeaders.append("Authorization", s"Bearer $SUPABASE_ANON_KEY")

    val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.GET
      this.headers = requestHeaders
    }

    dom.fetch(slotsApiUrl, requestInit).toFuture
      .flatMap { response =>
        if (response.ok) response.json().toFuture
        else {
          dom.window.alert(s"Failed to fetch slots: ${response.statusText}")
          scala.concurrent.Future.failed(new Exception("Fetch slots failed"))
        }
      }
      .foreach { json =>
        val slots = json.asInstanceOf[js.Array[js.Dynamic]]

        slotsContainer.innerHTML = "" // Clear previous list

        if (slots.isEmpty) {
          val noSlotsMsg = document.createElement("p")
          noSlotsMsg.textContent = "No slots available."
          slotsContainer.appendChild(noSlotsMsg)
        } else {
          // sorting slots by date 
          val sortedSlots = slots.sort((a, b) => {
            val timeA = js.Date.parse(a.slot_time.asInstanceOf[String])
            val timeB = js.Date.parse(b.slot_time.asInstanceOf[String])
            if (timeA < timeB) -1
            else if (timeA > timeB) 1
            else 0
          })
          sortedSlots.foreach { slot =>
            val slotDiv = document.createElement("div").asInstanceOf[Div]
            slotDiv.style.marginTop = "20px"
            slotDiv.style.marginLeft = "auto"
            slotDiv.style.marginRight = "auto"
            slotDiv.style.width = "80%"
            slotDiv.style.padding = "20px"
            slotDiv.style.border = "1px solid #ccc"
            slotDiv.style.borderRadius = "8px"
            slotDiv.style.backgroundColor = "#f9f9f9"
            slotDiv.style.boxShadow = "0 2px 8px rgba(0,0,0,0.1)"

            val clinicName = clinicMap.getOrElse(slot.clinic_id.asInstanceOf[String], "Unknown Clinic")
            val formattedTime = formatSlotTime(slot.slot_time.asInstanceOf[String])

            val title = document.createElement("h3")
            title.textContent = s"$clinicName"
            slotDiv.appendChild(title)

            val timeP = document.createElement("p")
            timeP.textContent = formattedTime
            slotDiv.appendChild(timeP)

            val lengthP = document.createElement("p")
            lengthP.textContent = s"Length: ${slot.slot_length} minutes"
            slotDiv.appendChild(lengthP)

            val isTaken = slot.is_taken.asInstanceOf[Boolean]
            val statusP = document.createElement("p")
            statusP.textContent = if (!isTaken) "Status: Currently Available" else "Status: Booked"
            slotDiv.appendChild(statusP)

            slotDiv.addEventListener("click", { (_: dom.MouseEvent) =>
                showSlotDetails(slot)
              })
              slotsContainer.appendChild(slotDiv)
          }
        }
      }
  }



  def createSlot(date: String, time: String, clinicId: String, length: Long): Unit = {
    
    if (date.isEmpty || time.isEmpty) {
      dom.window.alert("Please select both date and time.")
    } else if (clinicId.isEmpty) {
      dom.window.alert("Please select a clinic.")
    } else {
      if (!validateDateTime(time, date)) {
        return
      }

    
        
      val slotTime = s"${date}T${time}:00Z"

      val data = literal(
        "slot_time" -> slotTime,
        "clinic_id" -> clinicId,
        "slot_length" -> length
      )

      val headers = js.Dictionary(
        "Content-Type" -> "application/json",
        "apikey" -> SUPABASE_ANON_KEY,
        "Authorization" -> s"Bearer $SUPABASE_ANON_KEY"
      )

      val requestOptions = literal(
        method = "POST",
        headers = headers,
        body = JSON.stringify(data)
      ).asInstanceOf[dom.RequestInit]

      dom.fetch(s"$SUPABASE_URL/rest/v1/slots", requestOptions)
        .toFuture
        .flatMap { response =>
          if (response.ok) {
            dom.window.alert("Slot created successfully!")
              // 💡 Reset fields here:
            dateInput.value = ""
            timeInput.value = ""
            clinicSelect.selectedIndex = 0
            lengthInput.value = "30"
            
            fetchAndDisplaySlots()

            response.text().toFuture
          } else {
            response.text().toFuture.map { errorText =>
              dom.window.alert(s"Failed to create slot: $errorText")
            }
          }
        }
        .recover {
          case e: Throwable =>
            dom.window.alert(s"Error: ${e.getMessage}")
        }
    }
  }

  def showSlotDetails(slot: js.Dynamic): Unit = {
    slotsContainer.innerHTML = ""  // Clear the container for details view

    val detailsDiv = document.createElement("div").asInstanceOf[Div]
    detailsDiv.setAttribute("style",
      """
        |padding: 20px;
        |border: 1px solid #ccc;
        |border-radius: 8px;
        |background-color: #f9f9f9;
        |box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        |margin-top: 20px;
        |text-align: left;
        """.stripMargin)

    val clinicName = clinicMap.getOrElse(slot.clinic_id.asInstanceOf[String], "Unknown Clinic")
    val formattedTime = formatSlotTime(slot.slot_time.asInstanceOf[String])

    val title = document.createElement("h2")
    title.textContent = s"$clinicName"
    detailsDiv.appendChild(title)

    val timeP = document.createElement("p")
    timeP.textContent = s"Time: $formattedTime"
    detailsDiv.appendChild(timeP)

    val lengthP = document.createElement("p")
    lengthP.textContent = s"Length: ${slot.slot_length} minutes"
    detailsDiv.appendChild(lengthP)

    val statusP = document.createElement("p")
    statusP.textContent = if (!slot.is_taken.asInstanceOf[Boolean]) "Status: Available" else "Status: Booked"
    detailsDiv.appendChild(statusP)

    // Delete button
    val deleteButton = document.createElement("button").asInstanceOf[Button]
    deleteButton.textContent = "Delete Slot"
    deleteButton.setAttribute("style",
      """
        |margin-right: 10px;
        |padding: 8px 16px;
        |background-color: #b22222;
        |color: white;
        |border: none;
        |border-radius: 6px;
        |cursor: pointer;
        """.stripMargin)

    deleteButton.addEventListener("click", { (_: dom.MouseEvent) =>
      deleteSlot(slot.slot_id.asInstanceOf[String])
    })
    detailsDiv.appendChild(deleteButton)

    // Back button
    val backButton = document.createElement("button").asInstanceOf[Button]
    backButton.textContent = "Back"
    backButton.setAttribute("style",
      """
        |padding: 8px 16px;
        |background-color: #333;
        |color: white;
        |border: none;
        |border-radius: 6px;
        |cursor: pointer;
        """.stripMargin)

    backButton.addEventListener("click", { (_: dom.MouseEvent) =>
      fetchAndDisplaySlots()
    })
    detailsDiv.appendChild(backButton)

    slotsContainer.appendChild(detailsDiv)
  }

  def deleteSlot(slotId: String): Unit = {
    val confirmed = dom.window.confirm("Are you sure you want to delete this slot?")
    if (confirmed) {
      val headers = js.Dictionary(
        "apikey" -> SUPABASE_ANON_KEY,
        "Authorization" -> s"Bearer $SUPABASE_ANON_KEY"
      )

      val requestOptions = literal(
        method = "DELETE",
        headers = headers
      ).asInstanceOf[dom.RequestInit]

      dom.fetch(s"$SUPABASE_URL/rest/v1/slots?slot_id=eq.$slotId", requestOptions)
        .toFuture
        .flatMap { response =>
          if (response.ok) {
            dom.window.alert("Slot deleted successfully.")
            fetchAndDisplaySlots()
            response.text().toFuture
          } else {
            response.text().toFuture.map { errorText =>
              dom.window.alert(s"Failed to delete slot: $errorText")
            }
          }
        }
        .recover {
          case e: Throwable =>
            dom.window.alert(s"Error deleting slot: ${e.getMessage}")
        }
    }
  }




  def validateDateTime(time: String, date: String): Boolean = {
    val timeParts = time.split(":").map(_.toInt)
    val hours = timeParts(0)
    val minutes = timeParts(1)

    val dateTime = new js.Date(s"${date}T${time}:00Z")
    val dayOfWeek = dateTime.getUTCDay().toInt  // 0 = Sunday, 6 = Saturday
    println(s"Day of the week is $dayOfWeek")

    if (dayOfWeek == 0 || dayOfWeek == 6) {
      dom.window.alert("Please select a weekday. Appointments can't be booked on weekends.")
      false
    } else if (hours < 9 || (hours >= 17 && minutes > 0)) {
      dom.window.alert("Please select a time between 09:00 and 17:00.")
      false 
    } else {
      true
    }
  }


  def formatSlotTime(isoTime: String): String = {
    val date = new js.Date(isoTime)

    val day = f"${date.getUTCDate().toInt}%02d"
    val month = f"${(date.getUTCMonth() + 1).toInt}%02d"  // Months are zero-based
    val year = date.getUTCFullYear().toInt

    val hours = f"${date.getUTCHours().toInt}%02d"
    val minutes = f"${date.getUTCMinutes().toInt}%02d"

    s"Date: $day/$month/$year Time: $hours:$minutes"
  }



}