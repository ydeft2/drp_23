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
  isRead: Boolean
)

object Inbox {

  var notifications: List[NotificationResponse] = List()

  def parseNotifications(jsArray: js.Array[js.Dynamic]): List[NotificationResponse] = {
    jsArray.toList.flatMap { jsObj =>
      try {

        Some(NotificationResponse(
          notificationId = UUID.fromString(jsObj.notification_id.asInstanceOf[String]),
          userId = UUID.fromString(jsObj.user_id.asInstanceOf[String]),
          message = jsObj.message.asInstanceOf[String],
          createdAt = jsObj.created_at.asInstanceOf[String],
          isRead = jsObj.is_read.asInstanceOf[Boolean]
        ))

      } catch {
        case e: Throwable =>
          println(s"Failed to parse notification: ${e.getMessage}")
          None
      }
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
      "userId" -> uid,
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

    val requestBody = js.Dynamic.literal(
      "uid" -> uid.toString,
      "id" -> notificationId.toString
    )

    val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.POST
      headers = requestHeaders
      body = JSON.stringify(requestBody)
    }

    // Optimistically update local state
    notifications.find(_.notificationId == notificationId).foreach { n =>
      n.asInstanceOf[js.Dynamic].updateDynamic("is_read")(true)
    }
    renderNotifications(notificationsBox, notifications)

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
      notificationsBox.appendChild(noNotifications)
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
          val contentHtml =
            s"""
              |<p><strong>Received:</strong> ${timeDiv.textContent}</p>
              |<hr/>
              |<p>${notification.message}</p>
            """.stripMargin
          showModal(contentHtml)
        })

        notificationItem.appendChild(leftSide)
        notificationItem.appendChild(timeDiv)
        notificationsBox.appendChild(notificationItem)
      }
    }
  }

  
  def render(): Unit = {
    clearPage()

    document.body.appendChild(createSubpageHeader("Dentana Notifications"))

    // Notifications box
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
    
    document.body.appendChild(notificationsBox)
    
    Spinner.show()
    
    fetchNotifications { () =>
      Spinner.hide()
      renderNotifications(notificationsBox, notifications)
    }
  }
}
