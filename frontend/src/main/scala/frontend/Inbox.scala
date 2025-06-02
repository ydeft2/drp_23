package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object Inbox {

  var notifications: List[String] = List()


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
        notifications = json.asInstanceOf[js.Array[String]].toList
        onSuccess()
      }
      .recover {
        case e =>
          Spinner.hide()
          dom.window.alert(s"Error: ${e.getMessage}")
      }
  }

  def renderNotifications(notificationsBox: Div, notifications: List[String]): Unit = {
    notificationsBox.innerHTML = ""

    if (notifications.isEmpty) {
      val noNotifications = document.createElement("p")
      noNotifications.textContent = "No notifications available."
      notificationsBox.appendChild(noNotifications)
    } else {
      val notificationsTitle = document.createElement("h2")
      notificationsTitle.textContent = "Notifications"
      notificationsBox.appendChild(notificationsTitle)
      notifications.foreach { notification =>
        val notificationItem = document.createElement("div").asInstanceOf[Div]
        notificationItem.textContent = notification
        notificationItem.style.padding = "10px"
        notificationItem.style.borderBottom = "1px solid #ddd"
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
