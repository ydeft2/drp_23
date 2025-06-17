package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSON
import frontend.MapPage
import scalajs.js.JSConverters.JSRichFutureNonThenable
import scala.Option
import scala.Some
import scala.None
import scala.concurrent.Future
import scala.scalajs.js.timers._
import concurrent.duration.DurationInt

object HomePage {
  case class Booking(
    bookingId: String,
    clinicId: String,
    slotTime: String,
    clinicInfo: String,
    appointmentType: String
  )


  private var bookings: List[Booking] = List()

  private var unreadNotifications: Int = 0

  private var unreadChatCount: Int = 0

  private var es: dom.EventSource = _

  def render(): Unit = {
    Spinner.show()
    val userId = dom.window.localStorage.getItem("userId")

    fetchUnreadCount().foreach { unreadCount =>
      unreadNotifications = unreadCount
      

      val accountBtn = createHeaderButton("Account")
      accountBtn.addEventListener("click", (_: dom.MouseEvent) => Account.render())

      val inboxLabel = if (unreadNotifications > 0) s"Inbox ($unreadNotifications)" else "Inbox"
      val inboxBtn = createHeaderButton(inboxLabel)
      inboxBtn.id = "inbox-button" // Set an ID for the inbox button
      inboxBtn.addEventListener("click", (_: dom.MouseEvent) => Inbox.render())
      Layout.renderPage(
        leftButton = Some(accountBtn),
        rightButton = Some(inboxBtn),
        contentRender = () => {
          fetchBookings(userId).toFuture.foreach { fetchedBookings =>
            bookings = fetchedBookings
            document.body.appendChild(createFindClinicsButton())
            document.body.appendChild(buildBookingsBox())
            document.body.appendChild(createBookingButton())
            println("unreadChatCount: " + unreadChatCount)

            countUnreadMessages(userId).foreach { cnt =>
              unreadChatCount = cnt
              subscribeUnreadSSE(userId)
              document.body.appendChild(createChatButton(cnt))
            }

            
            Spinner.hide()
            setInterval(2.seconds) {
              refreshInboxBadge(userId)
            }
          }
        }
      )
    }
  }

  private def refreshInboxBadge(userId: String): Unit = {
    println("Refreshing inbox badge...")
    fetchUnreadCount().foreach { unreadCount =>
    unreadNotifications = unreadCount
    val inboxBtn = document.querySelector("#inbox-button").asInstanceOf[Button]
    inboxBtn.textContent = if (unreadNotifications > 0) s"Inbox ($unreadNotifications)" else "Inbox"
    }
  }


  private def buildBookingsBox(): Div = {
    val outerBox = document.createElement("div").asInstanceOf[Div]
    outerBox.id = "bookings-box" // Add this line
    outerBox.style.marginTop = "20px"
    outerBox.style.marginLeft = "auto"
    outerBox.style.marginRight = "auto"
    outerBox.style.width = "80%"
    outerBox.style.border = "1px solid #ccc"
    outerBox.style.borderRadius = "8px"
    outerBox.style.boxShadow = "0 2px 8px rgba(0, 0, 0, 0.1)"
    outerBox.style.backgroundColor = "#f9f9f9"
    outerBox.style.overflow = "hidden"

    val title = document.createElement("div").asInstanceOf[Div]
    title.textContent = "Your bookings"
    title.style.fontSize = "1.5em"
    title.style.fontWeight = "bold"
    title.style.padding = "20px"
    title.style.borderBottom = "1px solid #ddd"
    title.style.backgroundColor = "#f9f9f9"
    title.style.position = "sticky"
    title.style.top = "0"
    title.style.zIndex = "1"

    val scrollArea = document.createElement("div").asInstanceOf[Div]
    scrollArea.style.maxHeight = "400px"
    scrollArea.style.overflowY = "auto"
    scrollArea.style.padding = "20px"

    if (bookings.isEmpty) {
      val noBookingsMsg = document.createElement("div").asInstanceOf[Div]
      noBookingsMsg.textContent = "You have no outstanding bookings."
      noBookingsMsg.style.textAlign = "center"
      noBookingsMsg.style.fontSize = "1.2em"
      noBookingsMsg.style.marginBottom = "20px"

      val img = document.createElement("img").asInstanceOf[dom.html.Image]
      img.src = "images/Waiting.png"
      img.alt = "No bookings"
      img.style.display = "block"
      img.style.margin = "0 auto"
      img.style.width = "120px"
      img.style.height = "120px"

      scrollArea.appendChild(noBookingsMsg)
      scrollArea.appendChild(img)
    } else {
      bookings.foreach(b => scrollArea.appendChild(buildBookingEntry(b)))
    }

    outerBox.appendChild(title)
    outerBox.appendChild(scrollArea)

    outerBox
  }


  private def buildBookingEntry(booking: Booking): Div = {
    val entry = document.createElement("div").asInstanceOf[Div]
    entry.className = "booking-item"

    // a little separator helps visually
    entry.style.borderBottom = "1px solid #eee"
    entry.style.paddingBottom = "12px"
    entry.style.marginBottom = "12px"

    val isPending = booking.appointmentType == "NOT_SET"
    val titleText = if (isPending) "Pending Confirmation"
                    else booking.appointmentType.replace("_", " ").toLowerCase.capitalize

    // Create dot element
    val dot = document.createElement("span").asInstanceOf[dom.html.Span]
    dot.style.display = "inline-block"
    dot.style.width = "12px"
    dot.style.height = "12px"
    dot.style.borderRadius = "50%"
    dot.style.marginRight = "8px"
    dot.style.verticalAlign = "middle"
    dot.style.backgroundColor = if (isPending) "orange" else "green"

    // Title container
    val titleContainer = document.createElement("span").asInstanceOf[dom.html.Span]
    titleContainer.appendChild(dot)
    titleContainer.appendChild(document.createTextNode(titleText))

    val date = new js.Date(booking.slotTime)
    val months = js.Array("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val minutes = {
      val min = date.getMinutes().toInt
      if (min < 10) s"0$min" else s"$min"
    }
    val formattedTime = s" ${date.getHours()}:$minutes ${months(date.getMonth().toInt)} ${date.getDate()}"

    // Placeholder for clinic name
    val clinicNameSpan = document.createElement("span").asInstanceOf[dom.html.Span]
    clinicNameSpan.textContent = "Loading clinic..."

    // Compose entry HTML
    entry.innerHTML =
      s"""
        <span id="booking-dot-title"></span><br>
        <span><strong>Time:</strong> $formattedTime</span><br>
        <span><strong>Clinic:</strong> </span>
      """

    entry.querySelector("#booking-dot-title").appendChild(titleContainer)
    entry.querySelector("span + br + span + br + span").appendChild(clinicNameSpan)

    // Fetch and display clinic name
    fetchClinicDetails(booking.clinicId).foreach { clinic =>
      clinicNameSpan.textContent = clinic.name
    }

    entry.addEventListener("click", (_: dom.MouseEvent) => {
      fetchClinicDetails(booking.clinicId).foreach { clinic =>
        showModal(renderBookingDetails(booking, Some(clinic)))
        addCancelBookingButton(booking)
      }
    })

    entry
  }


  def renderBookingDetails(booking: Booking, clinicOpt: Option[Clinic] = None): String = {
    val title = if (booking.appointmentType == "NOT_SET") "Pending Confirmation"
                else booking.appointmentType.replace("_", " ").toLowerCase.capitalize
    val date = new js.Date(booking.slotTime)
    val months = js.Array("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val minutes = {
      val min = date.getMinutes().toInt
      if (min < 10) s"0$min" else s"$min"
    }
    val formattedTime = s"${date.getHours()}:$minutes ${months(date.getMonth().toInt)} ${date.getDate()}"

    val clinicName = clinicOpt.map(_.name).getOrElse("Loading...")
    val clinicAddress = clinicOpt.map(_.address).getOrElse("")

    s"""
      <h3>$title</h3>
      <p><strong>Time:</strong> $formattedTime</p>
      <p><strong>Clinic:</strong> $clinicName</p>
      ${if (clinicAddress.nonEmpty) s"<p><strong>Address:</strong> $clinicAddress</p>" else ""}
      <div id="cancel-booking-btn-container"></div>
    """
  }

  private def createChatButton(unread: Int): Div = {
    val chatButton = document.createElement("button").asInstanceOf[dom.html.Button]
    chatButton.id = "chat-button"
    chatButton.style.position = "fixed"
    chatButton.style.bottom = "80px"
    chatButton.style.right = "50px"
    chatButton.style.zIndex = "1000"
    chatButton.style.backgroundImage = "linear-gradient(135deg, #7b2ff7, #f107a3)"   
    chatButton.style.color = "#fff"
    chatButton.style.border = "none"
    chatButton.style.borderRadius = "50%"
    chatButton.style.width = "72px"
    chatButton.style.height = "72px"
    chatButton.style.boxShadow = "0 4px 16px rgba(0,0,0,0.18)"
    chatButton.style.cursor = "pointer"
    chatButton.style.display = "flex"
    chatButton.style.setProperty("align-items", "center")
    chatButton.style.setProperty("justify-content", "center")
    chatButton.style.padding = "0"

    val icon = document.createElement("img").asInstanceOf[dom.html.Image]
    icon.src = "images/icons/MessagesWhite.png"
    icon.alt = "Chats"
    icon.style.width = "40px"
    icon.style.height = "40px"
    icon.style.display = "block"
    icon.style.margin = "0"
    icon.style.padding = "0"
    icon.style.pointerEvents = "none" 

    chatButton.appendChild(icon)

    chatButton.addEventListener("mouseover", (_: dom.MouseEvent) => {
      chatButton.style.transform = "translateY(-4px)"
      chatButton.style.boxShadow = "0 8px 18px rgba(0,0,0,.25)"
    })

    chatButton.addEventListener("mouseout", (_: dom.MouseEvent) => {
      chatButton.style.transform = "translateY(0)"
      chatButton.style.boxShadow = "0 6px 14px rgba(0,0,0,.20)"
    })

    chatButton.addEventListener("click", (_: dom.MouseEvent) => {
      ChatPage.render()
    })
    
    if (unread > 0) {
      val badge = document.createElement("span").asInstanceOf[Span]
      badge.textContent = unread.toString
      badge.className   = "notification-badge"

      badge.style.position     = "absolute"
      badge.style.top          = "1px"
      badge.style.right        = "2px"
      badge.style.background   = "#7b2ff7"
      badge.style.color        = "white"
      badge.style.fontSize     = "1em"
      badge.style.lineHeight   = "1"
      badge.style.borderRadius = "50%"
      badge.style.padding      = "1px 2px"

      chatButton.appendChild(badge)
    }

    val wrapper = document.createElement("div").asInstanceOf[dom.html.Div]
    wrapper.appendChild(chatButton)
    wrapper
  }

  // The new "Find Clinics" button
  private def createFindClinicsButton(): Div = {
    val btn = document.createElement("div").asInstanceOf[Div]
    btn.textContent = "Find Clinics"
    btn.style.cssText =
      """
      position: fixed;
      top: 80px;
      right: 20px;
      background: #2ecc71;
      color: white;
      padding: 16px 24px;
      border-radius: 8px;
      cursor: pointer;
      font-weight: bold;
    """
    btn.onclick = (_: dom.MouseEvent) => MapPage.render()
    btn
  }


  private def createBookingButton(): Div = {
    val button = document.createElement("div").asInstanceOf[Div]
    button.textContent = "Create Booking"

    // Base styles
    button.style.position = "fixed"
    button.style.left = "50%"
    button.style.bottom = "80px"
    button.style.transform = "translate(-50%, 0)" 
    button.style.backgroundImage = "linear-gradient(135deg, #7b2ff7, #f107a3)"
    button.style.color = "white"
    button.style.padding = "24px 48px"
    button.style.fontSize = "1.2em"
    button.style.fontWeight = "bold"
    button.style.borderRadius = "60px"
    button.style.cursor = "pointer"
    button.style.boxShadow = "0 6px 14px rgba(0, 0, 0, 0.2)"
    button.style.transition = "transform 0.2s ease, box-shadow 0.2s ease, background-position 0.5s ease"
    button.style.backgroundSize = "200% 200%"
    button.style.backgroundRepeat = "no-repeat"
    button.style.setProperty("will-change", "transform") // Hint browser for performance

    // Prevent "hopping" by preserving horizontal transform
    button.addEventListener("mouseover", (_: dom.MouseEvent) => {
      button.style.transform = "translate(-50%, -4px)" // move up, stay centered
      button.style.boxShadow = "0 8px 18px rgba(0, 0, 0, 0.25)"
    })

    button.addEventListener("mouseout", (_: dom.MouseEvent) => {
      button.style.transform = "translate(-50%, 0)" // reset to original position
      button.style.boxShadow = "0 6px 14px rgba(0, 0, 0, 0.2)"
    })

    // Click handler
    button.addEventListener("click", (_: dom.MouseEvent) => {
      BookingPage.render()
    })

    button
  }

  // Add this after showing the modal, to inject the button and logic
  def addCancelBookingButton(booking: Booking): Unit = {
    val container = dom.document.getElementById("cancel-booking-btn-container")
    if (container != null) {
      val button = dom.document.createElement("button").asInstanceOf[dom.html.Button]
      button.textContent = "Cancel Booking"
      button.style.backgroundColor = "#e74c3c"
      button.style.color = "white"
      button.style.padding = "10px 20px"
      button.style.border = "none"
      button.style.borderRadius = "5px"
      button.style.cursor = "pointer"

      var confirmState = false

      button.onclick = (_: dom.MouseEvent) => {
        if (!confirmState) {
          button.textContent = "Confirm Cancellation"
          button.style.backgroundColor = "#c0392b"
          confirmState = true
        } else {
          cancelBooking(booking)
        }
      }

      container.appendChild(button)
    }
  }

  private def fetchBookings(userId: String): js.Promise[List[Booking]] = {
    val url = s"/api/bookings/list?patient_id=$userId"

    dom.fetch(url)
      .toFuture
      .flatMap(_.json().toFuture)
      .map { json =>
        val jsArr = json.asInstanceOf[js.Array[js.Dynamic]]
        jsArr.map { item =>
          Booking(
            bookingId = item.booking_id.toString,
            clinicId = item.clinic_id.toString,
            slotTime = item.slot_time.toString,
            clinicInfo = item.clinic_info.asInstanceOf[js.UndefOr[String]].getOrElse("No info"),
            appointmentType = item.appointment_type.toString
          )
        }.toList
      }
      .toJSPromise
  }

  def cancelBooking(booking: Booking): Unit = {
    replaceModalContent(
      """
      <h3>Cancellation Successful</h3>
      <p>Your booking has been successfully cancelled.</p>
      <img src="images/Sad.png" alt="Sad Icon" style="width: 50px; height: 50px;">
      """
    )

    val req = new dom.Request(
      s"/api/bookings/cancel/${booking.bookingId}",
      new dom.RequestInit {
        method = org.scalajs.dom.HttpMethod.DELETE
        headers = new dom.Headers {
          append("Content-Type", "application/json")
          val accessToken = dom.window.localStorage.getItem("accessToken")
          if (accessToken != null) {
            append("Authorization", s"Bearer $accessToken")
          }
        }
      }
    )
    dom.fetch(req).toFuture.onComplete { _ =>
      // Remove the cancelled booking from the list
      bookings = bookings.filterNot(_.bookingId == booking.bookingId)
      // Replace only the bookings box
      dom.document.getElementById("bookings-box") match {
        case box: Div =>
          val newBox = buildBookingsBox()
          box.parentNode.replaceChild(newBox, box)
        case _ => 
      }
    }
  }

    private def countUnreadMessages(userId: String): Future[Int] = {
    dom.fetch(s"/api/messages/unreadCount/$userId")
      .toFuture
      .flatMap(_.json().toFuture)
      .map { jsObj =>
        // parse out the integer
        jsObj.asInstanceOf[js.Dynamic].count.asInstanceOf[Double].toInt
      }
      .recover { case _ => 0 }
  }


   private def subscribeUnreadSSE(userId: String): Unit = {
    if (es != null) es.close()
    es = new dom.EventSource(s"/api/messages/stream/$userId")
    es.onmessage = (e: dom.MessageEvent) => {
      val raw = JSON.parse(e.data.asInstanceOf[String])
      val recv = raw.receiver_id.asInstanceOf[String]
      if (recv == userId) {
        unreadChatCount += 1
        updateChatBadge(unreadChatCount)
      }
    }
    es.onerror = (_: dom.Event) => {
      es.close()
      dom.window.setTimeout(() => subscribeUnreadSSE(userId), 2000)
    }
  }

  private def updateChatBadge(cnt: Int): Unit = {
    val btn = document.getElementById("chat-button").asInstanceOf[Div]
    // clear old badge
    Option(btn.querySelector(".notification-badge")).foreach(_.remove())
    if (cnt > 0) {
      val badge = document.createElement("span").asInstanceOf[Span]
      badge.className   = "notification-badge"
      badge.textContent = cnt.toString
      badge.style.position     = "absolute"
      badge.style.top          = "8px"
      badge.style.right        = "12px"
      badge.style.background   = "#007bff"
      badge.style.color        = "white"
      badge.style.fontSize     = "0.8em"
      badge.style.borderRadius = "50%"
      badge.style.padding      = "2px 6px"
      btn.appendChild(badge)
    }
  }
}

 

