// package frontend

// import org.scalajs.dom
// import org.scalajs.dom.document
// import org.scalajs.dom.html._
// import scala.scalajs.js

// // object AdminPage {

// //   def render(): Unit = {
// //     document.body.innerHTML = ""

// //     document.body.appendChild(createAdminPageHeader())

// //     // Create a grey box container for the admin page
// //     val container = document.createElement("div")
// //     container.setAttribute("style", "margin: 100px auto; width: 600px; padding: 20px; background-color: lightgrey; border-radius: 5px;")
// //     document.body.appendChild(container)

// //     // Add admin content
// //     val adminContent = document.createElement("h2")
// //     adminContent.textContent = "Admin Dashboard"
// //     adminContent.setAttribute("style", "text-align: center;")
// //     container.appendChild(adminContent)

// //     // Add more admin functionalities here
// //   }
// // }

// object AdminPage {

//   def render(): Unit = {
//     document.body.innerHTML = ""

//     document.body.appendChild(createAdminPageHeader())

//     // Function to create a grey box container with a heading
//     def createDashboardBox(title: String): Element = {
//       val container = document.createElement("div").asInstanceOf[Div]
//       container.setAttribute("style", 
//         """
//         margin: 100px auto;
//         width: 600px;
//         padding: 20px;
//         background-color: lightgrey;
//         border-radius: 5px;
//         text-align: center;
//         """.stripMargin)

//       val heading = document.createElement("h2").asInstanceOf[Heading]
//       heading.textContent = title
//       container.appendChild(heading)

//       container
//     }

//     // Create and append Admin Dashboard box
//     val adminBox = createDashboardBox("Admin Dashboard")
//     document.body.appendChild(adminBox)

//     // Create and append Booking Dashboard box
//     val bookingBox = createDashboardBox("Booking Dashboard")
//     document.body.appendChild(bookingBox)
//   }
// }

package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.timers._
import concurrent.duration.DurationInt

object AdminPage {

  private var unreadNotifications: Int = 0
  private var unreadChatCount: Int = 0
  private var es: dom.EventSource = _

  case class DashboardItem(title: String, iconUrl: String, onClick: () => Unit)

  def render(): Unit = {
    Spinner.show()
   fetchUnreadCount().foreach { unreadCount =>
      unreadNotifications = unreadCount
      countUnreadMessages(dom.window.localStorage.getItem("userId")).foreach { cnt =>
        unreadChatCount = cnt
        println(s"Unread notifications: $unreadNotifications, Unread messages: $unreadChatCount")

        Layout.renderPage(
          contentRender = () =>
            buildAdminPage()
        )
        subscribeUnreadSSE(dom.window.localStorage.getItem("userId"))
        Spinner.hide()

        setInterval(2.seconds) {
            println("Refreshing inbox badge")
              refreshInboxBadge(dom.window.localStorage.getItem("userId"))
            }
      }
    }
  }

  private def refreshInboxBadge(userId: String): Unit = {
  
      val tiles = document.getElementsByClassName("dashboard-item")
      for i <- 0 until tiles.length do
        val t = tiles(i).asInstanceOf[Div]
        fetchUnreadCount().foreach { unreadCount =>
      unreadNotifications = unreadCount}
        if t.innerText.startsWith("Inbox") then
          // remove old badge
          Option(t.querySelector(".notification-badge")).foreach(_.remove())
          // append new if needed
          
          if unreadNotifications > 0 then
            val b = document.createElement("span").asInstanceOf[Span]
            b.className   = "notification-badge"
            b.textContent = unreadNotifications.toString
            t.querySelector(".icon-with-badge").appendChild(b)
  }

  private def updateChatBadge(cnt: Int): Unit = {
    val tiles = document.getElementsByClassName("dashboard-item")
    for i <- 0 until tiles.length do
      val t = tiles(i).asInstanceOf[Div]
      if t.innerText.startsWith("Messages") then
        Option(t.querySelector(".notification-badge")).foreach(_.remove())
        if cnt > 0 then
          val badge = document.createElement("span").asInstanceOf[Span]
          badge.className   = "notification-badge"
          badge.textContent = cnt.toString
          t.querySelector(".icon-with-badge").appendChild(badge)
  }

  private def subscribeUnreadSSE(userId: String): Unit = {
    if es != null then es.close()
    es = new dom.EventSource(s"/api/messages/stream/$userId")
    es.onmessage = (e: dom.MessageEvent) => {
      val raw = js.JSON.parse(e.data.asInstanceOf[String])
      val recv = raw.receiver_id.asInstanceOf[String]
      if recv == userId then
        unreadChatCount += 1
        updateChatBadge(unreadChatCount)
    }
    es.onerror = (_: dom.Event) => {
      es.close()
      dom.window.setTimeout(() => subscribeUnreadSSE(userId), 2000)
    }
  }

  private def buildAdminPage(): Unit = {
    val contentDiv = document.createElement("div").asInstanceOf[Div]
    contentDiv.className = "dashboard-grid"

    val items = Seq(
      DashboardItem("My Account", "images/icons/Account.png", () => AdminAccount.render()),
      DashboardItem(s"Inbox", "images/icons/Inbox.png", () => Inbox.render()),
      DashboardItem("Patient Bookings", "images/icons/Bookings.png", () => AdminPatientBookingsPage.render()),
      DashboardItem("Set Availability", "images/icons/Availability.png", () => BookingDashboard.render()),
      DashboardItem("Messages", "images/icons/Messages.png", () => ChatPage.render()),
      DashboardItem("Help", "images/icons/Help.png", () => HelpPage.render())
    )

    items.foreach { item =>
      val box = document.createElement("div").asInstanceOf[Div]
      box.className = "dashboard-item"

      val messageBadgeHtml = 
        if (item.title == "Messages" && unreadChatCount > 0) 
          s"""<span class="notification-badge">$unreadChatCount</span>"""
        else ""
      val badgeHtml = 
        if (item.title == "Inbox" && unreadNotifications > 0) 
          s"""<span class="notification-badge">$unreadNotifications</span>"""
        else ""

      box.innerHTML =
        s"""
          |<div class="dashboard-title">${item.title}</div>
          |<div class="dashboard-icon icon-with-badge">
          |  <img src="${item.iconUrl}" alt="${item.title} icon"/>
          |  $badgeHtml
          |  $messageBadgeHtml
          |</div>
        """.stripMargin

      box.addEventListener("click", (_: dom.MouseEvent) => item.onClick())
      contentDiv.appendChild(box)
    }


    document.body.appendChild(contentDiv)
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
}
