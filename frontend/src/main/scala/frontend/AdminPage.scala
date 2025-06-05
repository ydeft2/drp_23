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

object AdminPage {

  def render(): Unit = {
    document.body.innerHTML = ""

    // Create and append the header
    document.body.appendChild(createAdminPageHeader())

    // Create and append the Admin Dashboard box
    val adminBox = createAdminDashboardBox()
    document.body.appendChild(adminBox)

    // Create and append the Booking Dashboard box
    val bookingBox = createBookingDashboardBox()
    document.body.appendChild(bookingBox)
  }



  // Admin Dashboard box
  def createAdminDashboardBox(): Div = {
    val box = document.createElement("div").asInstanceOf[Div]
    box.setAttribute("style",
      """
        margin: 50px auto;
        width: 600px;
        padding: 20px;
        background-color: lightgrey;
        border-radius: 5px;
        text-align: center;
      """.stripMargin
    )

    val heading = document.createElement("h2").asInstanceOf[Heading]
    heading.textContent = "Admin Dashboard"
    box.appendChild(heading)

    box
  }

  // Booking Dashboard box
  def createBookingDashboardBox(): Div = {
    val box = document.createElement("div").asInstanceOf[Div]
    box.setAttribute("style",
      """
        margin: 50px auto;
        width: 600px;
        padding: 20px;
        background-color: lightgrey;
        border-radius: 5px;
        text-align: center;
      """.stripMargin
    )

    val heading = document.createElement("h2").asInstanceOf[Heading]
    heading.textContent = "Booking Dashboard"
    box.appendChild(heading)

    val button = document.createElement("button").asInstanceOf[Button]
    button.textContent = "Go to Booking Dashboard"
    button.setAttribute("style", "margin-top: 20px; padding: 10px 20px;")
    button.onclick = (_: dom.MouseEvent) => BookingDashboard.render()
    box.appendChild(button)

    box
  }
}
