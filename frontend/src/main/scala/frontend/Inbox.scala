package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import org.scalajs.dom.HTMLSpanElement
import java.util.UUID
import java.time.Instant



case class NotificationResponse(
  notificationId: UUID,
  userId: UUID,
  message: String,
  createdAt: String,
  isRead: Boolean,
  clinicId: scala.Option[String],
  slotId: scala.Option[String]
)

object Inbox {

  var notifications: List[NotificationResponse] = List()

  def parseNotifications(jsArray: js.Array[js.Dynamic]): List[NotificationResponse] =
    jsArray.toList.flatMap { jsObj =>
      try {
        // Pull out the bare fields
        val notificationId = UUID.fromString(jsObj.notification_id.asInstanceOf[String])
        val userId = UUID.fromString(jsObj.user_id.asInstanceOf[String])
        val message = jsObj.message.asInstanceOf[String]
        val createdAt = jsObj.created_at.asInstanceOf[String]
        val isRead = jsObj.is_read.asInstanceOf[Boolean]

        // Now metadata
        val mdDyn = jsObj.selectDynamic("metadata").asInstanceOf[js.Dynamic]
        // Some driver libraries return `undefined` if it doesn't exist
        val (clinicId, slotId) =
          if (mdDyn != null && !js.isUndefined(mdDyn)) {
            val cid = Option(mdDyn.clinicId.asInstanceOf[String])
            val sid = Option(mdDyn.slotId.asInstanceOf[String])
            (cid, sid)
          } else {
            (None, None)
          }

        Some(NotificationResponse(
          notificationId = notificationId,
          userId = userId,
          message = message,
          createdAt = createdAt,
          isRead = isRead,
          clinicId = clinicId,
          slotId = slotId
        ))
      } catch {
        case e: Throwable =>
          println(s"[Inbox] parseNotifications error: ${e.getMessage}")
          None
      }
    }

  def fetchNotifications(onSuccess: () => Unit): Unit = {
    val accessToken = dom.window.localStorage.getItem("accessToken")
    val uid = dom.window.localStorage.getItem("userId")

    if (accessToken == null || uid == null) {
      dom.window.alert("You are not logged in.")
      return
    }

    val requestHeaders = new dom.Headers()
    requestHeaders.append("Content-Type", "application/json")
    requestHeaders.append("Authorization", s"Bearer $accessToken")

    val requestBody = js.Dynamic.literal(
      "user_id" -> uid,
      "message" -> ""
    )

    val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.POST
      headers = requestHeaders
      body = JSON.stringify(requestBody)
    }

    dom.fetch("/api/notifications/fetch", requestInit)
      .toFuture
      .flatMap(_.json().toFuture) // get raw response text
      .map { jsValue =>
        println(s"Raw response: $jsValue")
        if (js.Array.isArray(jsValue)) {
          val arr = jsValue.asInstanceOf[js.Array[js.Dynamic]]
          notifications = parseNotifications(arr)
          onSuccess()
        } else {
          Spinner.hide()
          dom.window.alert("Invalid response format.")
        }
      }
      .recover {
        case e =>
          Spinner.hide()
          dom.window.alert(s"Error: ${e.getMessage}")
      }
  }

  def markNotificationRead(
    notificationId: UUID,
    notificationsBox: Div,
    notifications: List[NotificationResponse],
    onSuccess: () => Unit
  ): Unit = {

    val accessToken = dom.window.localStorage.getItem("accessToken")
    val uid = dom.window.localStorage.getItem("userId")

    if (accessToken == null || uid == null) return

    val requestHeaders = new dom.Headers()
    requestHeaders.append("Content-Type", "application/json")
    requestHeaders.append("Authorization", s"Bearer $accessToken")

    val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.POST
      headers = requestHeaders
      body = JSON.stringify(notificationId.toString)
    }

    // Optimistically update local state
    val updatedNotifications = notifications.map { n =>
      if (n.notificationId == notificationId) n.copy(isRead = true)
      else n
    }

    Inbox.notifications = Inbox.notifications.map { n =>
      if (n.notificationId == notificationId) n.copy(isRead = true) else n
    }
    renderNotifications(notificationsBox, Inbox.notifications)


    dom.fetch("/api/notifications/markNotificationRead", requestInit)
      .toFuture
      .map(_ => onSuccess())
  }
  def renderNotifications(notificationsBox: Div, notifications: List[NotificationResponse]): Unit = {
    notificationsBox.innerHTML = ""

    val sortedNotifications = notifications.sortBy(n => new js.Date(n.createdAt).getTime())(Ordering[Double].reverse)

    if (sortedNotifications.isEmpty) {
      val noNotifications = document.createElement("p")
      noNotifications.textContent = "No notifications available."
      noNotifications.setAttribute("style", "text-align: center; margin-top: 20px;")
      notificationsBox.appendChild(noNotifications)
      val img = document.createElement("img").asInstanceOf[dom.html.Image]
      img.src = "images/Waiting.png"
      img.style.display = "block"
      img.style.marginLeft = "auto"
      img.style.marginRight = "auto"
      img.style.width = "250px"
      notificationsBox.appendChild(img)
    } else {
      val notificationsTitle = document.createElement("h2")
      notificationsTitle.textContent = "Notifications"
      notificationsBox.appendChild(notificationsTitle)

      val markAllReadButton = document.createElement("button").asInstanceOf[Button]
      markAllReadButton.textContent = "Mark all as read"
      markAllReadButton.style.marginBottom = "10px"
      markAllReadButton.addEventListener("click", (_: dom.MouseEvent) => {
        sortedNotifications.foreach { notification =>
          if (!notification.isRead) {
            markNotificationRead(notification.notificationId, notificationsBox, notifications, () => ())
          }
        }
      })
      notificationsBox.appendChild(markAllReadButton)

      sortedNotifications.foreach { notification =>
        val notificationItem = document.createElement("div").asInstanceOf[Div]
        val itemStyle = notificationItem.style
        itemStyle.display = "flex"
        itemStyle.setProperty("justify-content", "space-between")
        itemStyle.setProperty("align-items", "center")
        itemStyle.padding = "10px"
        itemStyle.borderBottom = "1px solid #ddd"

        // Group left side: dot + message
        val leftSide = document.createElement("div").asInstanceOf[Div]
        leftSide.style.display = "flex"
        leftSide.style.setProperty("align-items", "center")
        leftSide.style.setProperty("flex", "1")


        val dot = document.createElement("span").asInstanceOf[HTMLSpanElement]
        dot.textContent = "●"
        dot.style.color = "#2196f3"
        dot.style.marginRight = "10px"
        dot.style.minWidth = "16px"
        if (notification.isRead) {
          dot.style.visibility = "hidden"
        }

        val messageDiv = document.createElement("span").asInstanceOf[HTMLSpanElement]
        messageDiv.textContent = notification.message
        messageDiv.style.textAlign = "left"

        leftSide.appendChild(dot)
        leftSide.appendChild(messageDiv)

        // Right side: time
        val timeDiv = document.createElement("span").asInstanceOf[HTMLSpanElement]
        val date = new js.Date(notification.createdAt)
        val months = Array("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val month = months(date.getMonth().toInt)
        val day = f"${date.getDate().toInt}%02d"
        val hour = f"${date.getHours().toInt}%02d"
        val minute = f"${date.getMinutes().toInt}%02d"
        timeDiv.textContent = s"$month $day - $hour:$minute"

        val timeStyle = timeDiv.style
        timeStyle.fontSize = "0.9em"
        timeStyle.color = "#888"
        timeStyle.marginLeft = "20px"
        timeStyle.whiteSpace = "nowrap"

        notificationItem.addEventListener("click", (_: dom.MouseEvent) => {
          if (!notification.isRead) {
            markNotificationRead(notification.notificationId, notificationsBox, notifications, () => ())
          }

          val modalContent = document.createElement("div").asInstanceOf[Div]

          modalContent.innerHTML =
            s"""
               |<p><strong>Received:</strong> ${timeDiv.textContent}</p>
               |<hr/>
               |<p>${notification.message}</p>
          """.stripMargin

          for {
            cid <- notification.clinicId
            sid <- notification.slotId
          } {

            val goBtn = document.createElement("button").asInstanceOf[Button]
            goBtn.textContent = "<insert emoji idk> Take me there"
            goBtn.style.cssText =
              """
                |margin-top: 12px;
                |padding: 8px 16px;
                |background: #d32f2f;
                |color: white;
                |border: none;
                |border-radius: 4px;
                |cursor: pointer;
            """.stripMargin

            goBtn.onclick = (_: dom.MouseEvent) => {
              hideModal()
              BookingPage.render()
              BookingPage.showClinicAndSlot(cid, sid)
            }

            modalContent.appendChild(goBtn)
          }

          showModal(modalContent)
        })



        notificationItem.appendChild(leftSide)
        notificationItem.appendChild(timeDiv)
        notificationItem.appendChild(deleteButton(notification, notificationsBox))
        notificationsBox.appendChild(notificationItem)
      }
    }
  }

  private def deleteButton(notification: NotificationResponse, notificationsBox: Div): Button = {
    val deleteBtn = document.createElement("button").asInstanceOf[Button]
    deleteBtn.innerHTML = "🗑️"
    deleteBtn.title = "Delete notification"
    deleteBtn.style.border = "none"
    deleteBtn.style.cursor = "pointer"
    deleteBtn.style.fontSize = "1.2em"
    deleteBtn.style.color = "#888"
    deleteBtn.style.marginLeft = "15px"
    deleteBtn.style.transition = "color 0.3s ease, transform 0.3s ease"
    deleteBtn.style.outline = "none"
    deleteBtn.style.padding = "2px"
    
    deleteBtn.onmouseover = (_: dom.MouseEvent) => {
      deleteBtn.style.color = "#e53935"
      deleteBtn.style.transform = "scale(1.1)"
    }
    deleteBtn.onmouseout = (_: dom.MouseEvent) => {
      deleteBtn.style.color = "#888"
      deleteBtn.style.transform = "scale(1)"
    }

    var confirming = false

    deleteBtn.addEventListener("click", (e: dom.MouseEvent) => {
      e.stopPropagation()
      if (!confirming) {
        confirming = true
        deleteBtn.innerHTML = "confirm delete"
        js.timers.setTimeout(3000) {
          confirming = false
          deleteBtn.innerHTML = "🗑️"
        }
      } else {
        deleteNotification(notification.notificationId, notificationsBox)
      }
    })

    deleteBtn
  }

  def deleteNotification(notificationId: UUID, notificationsBox: Div): Unit = {
    val accessToken = dom.window.localStorage.getItem("accessToken")
    val uid = dom.window.localStorage.getItem("userId")

    if (accessToken == null || uid == null) {
      dom.window.alert("You are not logged in.")
      return
    }

    val requestHeaders = new dom.Headers()
    requestHeaders.append("Content-Type", "application/json")
    requestHeaders.append("Authorization", s"Bearer $accessToken")

    val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.POST
      headers = requestHeaders
      body = JSON.stringify(notificationId.toString)
    }

    // Optimistically remove notification from local state and rerender
    Inbox.notifications = Inbox.notifications.filterNot(_.notificationId == notificationId)
    renderNotifications(notificationsBox, Inbox.notifications)

    dom.fetch("/api/notifications/delete", requestInit)
      .toFuture
      .map { response =>
        if (!response.ok) {
          dom.window.alert("Failed to delete notification.")
        }
      }
      .recover { case e =>
        dom.window.alert(s"Error deleting notification: ${e.getMessage}")
      }
  }
  def renderNotificationsBox(): Div = {
    val notificationsBox = document.createElement("div").asInstanceOf[Div]
    notificationsBox.style.marginTop = "70px"
    notificationsBox.style.marginLeft = "auto"
    notificationsBox.style.marginRight = "auto"
    notificationsBox.style.width = "80%"
    notificationsBox.style.maxHeight = "400px"
    notificationsBox.style.overflowY = "scroll"
    notificationsBox.style.border = "1px solid #ccc"
    notificationsBox.style.borderRadius = "8px"
    notificationsBox.style.padding = "20px"
    notificationsBox.style.boxShadow = "0 2px 8px rgba(0, 0, 0, 0.1)"
    notificationsBox.style.backgroundColor = "#f9f9f9"
    notificationsBox
  }
  def render(): Unit = {
    Spinner.show()
    
    fetchNotifications { () =>
      Layout.renderPage(
        leftButton = Some(createHomeButton()),
        contentRender = () => 
        {
          Spinner.hide()
          val notificationsBox = renderNotificationsBox()
          document.body.appendChild(notificationsBox)
          renderNotifications(notificationsBox, notifications)
        }
      )
    }
  }
}
