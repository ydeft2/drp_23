package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue



object Account {
  case class User(name: String, dob: String, address: String)

  private var currentUser = User("", "", "")

  def fetchPatientDetails(onSuccess: () => Unit): Unit = {
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

    dom.fetch("/api/accountDetails", requestInit)
      .toFuture
      .flatMap(_.json().toFuture)
      .map { json =>
        val user = json.asInstanceOf[js.Dynamic]
        val name = s"${user.first_name.asInstanceOf[String]} ${user.last_name.asInstanceOf[String]}"
        val dob = user.dob.asInstanceOf[String]
        currentUser = User(name, dob, "placeholder address")
        onSuccess()
      }
      .recover {
        case e =>
          Spinner.hide()
          dom.window.alert(s"Error: ${e.getMessage}")
      }
  }



  def render(): Unit = {
    clearPage()
    document.body.appendChild(createSubpageHeader("Dentana Account"))

    Spinner.show()

    fetchPatientDetails { () =>
      Spinner.hide()
      val card = buildProfileCard(currentUser)
      document.body.appendChild(card)
      document.body.appendChild(buildDeleteAccountButton())
    }
  }



  private def buildProfileCard(user: User): Div = {
    val card = document.createElement("div").asInstanceOf[Div]
    card.style.marginTop = "70px"
    card.style.marginLeft = "auto"
    card.style.marginRight = "auto"
    card.style.width = "60%"
    card.style.maxHeight = "400px"
    card.style.border = "1px solid #ccc"
    card.style.borderRadius = "8px"
    card.style.padding = "20px"
    card.style.boxShadow = "0 2px 8px rgba(0, 0, 0, 0.1)"
    card.style.backgroundColor = "#f9f9f9"




    val heading = document.createElement("h2").asInstanceOf[Heading]
    heading.textContent    = "Account Details"
    heading.style.marginBottom = "24px"
    card.appendChild(heading)


    def infoRow(label: String, value: String): Div = {
      val row = styledDiv("marginBottom" -> "20px")
      row.innerHTML = s"<strong>$label</strong> $value"
      row
    }

    card.appendChild(infoRow("Name:", user.name))
    card.appendChild(infoRow("Date of Birth:", user.dob))
    card.appendChild(infoRow("Address:", user.address))

 
    val editBtn = document.createElement("button").asInstanceOf[Button]
    editBtn.textContent = "Edit Profile"
    styleButton(editBtn, background = "transparent", color = "purple", border = "2px solid purple")
    editBtn.style.display = "block"
    editBtn.style.margin  = "20px auto 0"
    editBtn.onclick = _ => dom.window.alert("Please contact your dental practice in order to edit your profile.")
    card.appendChild(editBtn)

    val logOutButton = document.createElement("button").asInstanceOf[Button]
    logOutButton.textContent = "Log Out"
    styleButton(logOutButton, background = "red", color = "white", border = "none")
    logOutButton.onclick = (_: dom.MouseEvent) => {
      dom.window.localStorage.removeItem("accessToken")
      dom.window.localStorage.removeItem("userId")
      dom.window.location.href = "/"
    }
    card.appendChild(logOutButton)

    card
  }

  private def clearPage(): Unit = document.body.innerHTML = ""

  private def styledDiv(styles: (String, String)*): Div = {
    val d = document.createElement("div").asInstanceOf[Div]
    styles.foreach { case (k, v) => d.style.setProperty(k, v) }
    d
  }

  private def styleButton(b: Button, background: String, color: String, border: String): Unit = {
    b.style.background   = background
    b.style.color        = color
    b.style.border       = border
    b.style.padding      = "10px 24px"
    b.style.cursor       = "pointer"
    b.style.borderRadius = "4px"
    b.style.fontSize     = "16px"
  }

  private def buildDeleteAccountButton(): Button = {
    val deleteBtn = document.createElement("button").asInstanceOf[Button]
    deleteBtn.textContent = "Delete Account"
    styleButton(deleteBtn, background = "red", color = "white", border = "none")
    deleteBtn.onclick = (_: dom.MouseEvent) => {
      // Ask for confirmation before deleting
      val confirmed = dom.window.confirm("Are you sure you want to delete your account? This action cannot be undone.")
      if (confirmed) {
        val accessToken = dom.window.localStorage.getItem("accessToken")
        val uid = dom.window.localStorage.getItem("userId")

        if (accessToken == null || uid == null) {
          dom.window.alert("You are not logged in.")
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

        dom.fetch("/api/deleteAccount", requestInit)
          .toFuture
          .flatMap(_.json().toFuture)
          .map { json =>
            dom.window.alert("Your account has been successfully deleted.")
            dom.window.localStorage.removeItem("accessToken")
            dom.window.localStorage.removeItem("userId")
            dom.window.location.href = "/"
          }
          .recover {
            case e =>
              dom.window.alert(s"Error: ${e.getMessage}")
          }
      }
      else {
        dom.window.alert("Account deletion cancelled.")
      }
    }
    deleteBtn
  }
}
