package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import org.scalajs.dom.HTMLSpanElement

@js.native
trait NotificationResponse extends js.Object {
  val message: String
  val created_at: String
}

object Inbox {

  var notifications: List[NotificationResponse] = List()


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
      "uid" -> uid,
      "accessToken" -> accessToken
    )

    val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.POST
      headers = requestHeaders
      body = JSON.stringify(requestBody)
    }

    dom.fetch("/api/notifications", requestInit)
      .toFuture
      .flatMap(_.json().toFuture)
      .map { json =>
        notifications = json.asInstanceOf[js.Array[NotificationResponse]].toList
        onSuccess()
      }
      .recover {
        case e =>
          Spinner.hide()
          dom.window.alert(s"Error: ${e.getMessage}")
      }
  }

  def renderNotifications(notificationsBox: Div, notifications: List[NotificationResponse]): Unit = {
    notificationsBox.innerHTML = ""

    val sortedNotifications = notifications.sortBy(n => new js.Date(n.created_at).getTime())(Ordering[Double].reverse)

    if (sortedNotifications.isEmpty) {
      val noNotifications = document.createElement("p")
      noNotifications.textContent = "No notifications available."
      notificationsBox.appendChild(noNotifications)
    } else {
      val notificationsTitle = document.createElement("h2")
      notificationsTitle.textContent = "Notifications"
      notificationsBox.appendChild(notificationsTitle)
      sortedNotifications.foreach { notification =>
        val notificationItem = document.createElement("div").asInstanceOf[Div]
        val itemStyle = notificationItem.style
        itemStyle.display = "flex"
        itemStyle.setProperty("justify-content", "space-between")
        itemStyle.setProperty("align-items", "center")
        itemStyle.padding = "10px"
        itemStyle.borderBottom = "1px solid #ddd"

        val messageDiv = document.createElement("span").asInstanceOf[HTMLSpanElement]
        messageDiv.textContent = notification.message

        val timeDiv = document.createElement("span").asInstanceOf[HTMLSpanElement]
        val date = new js.Date(notification.created_at)
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

        notificationItem.addEventListener("click", (_: dom.MouseEvent) => {
          val contentHtml =
            s"""
              |<p><strong>Received:</strong> ${timeDiv.textContent}</p>
              |<hr/>
              |<p>${notification.message}</p>
            """.stripMargin
          showModal(contentHtml)
        })

        notificationItem.appendChild(messageDiv)
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
