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

object AdminPage {

  private var unreadNotifications: Int = 0
  case class DashboardItem(title: String, iconUrl: String, onClick: () => Unit)

  def render(): Unit = {
    Spinner.show()
    fetchUnreadCount().foreach { unreadCount =>
      unreadNotifications = unreadCount
      Layout.renderPage(
        contentRender = () =>
        {
          buildAdminPage()
          Spinner.hide()
        }
      )
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
      DashboardItem("Messages", "images/icons/Messages.png", () => MessagesPage.render()),
      DashboardItem("Help", "images/icons/Help.png", () => HelpPage.render())
    )

    items.foreach { item =>
      val box = document.createElement("div").asInstanceOf[Div]
      box.className = "dashboard-item"

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
          |</div>
        """.stripMargin

      box.addEventListener("click", (_: dom.MouseEvent) => item.onClick())
      contentDiv.appendChild(box)
    }


    document.body.appendChild(contentDiv)
  }
}
