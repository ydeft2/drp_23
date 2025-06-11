package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import org.scalajs.dom.html
import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal
import scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalajs.dom.experimental.RequestInit
import org.scalajs.dom.experimental.Fetch
import org.scalajs.dom.experimental.Headers
import scala.concurrent.Future
import scala.scalajs.js.JSON
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
trait Role extends js.Object {
  val uid: String
  val is_patient: Boolean
}

 final case class User(
   name: String,
   dob: String,
   address: String,
)


def clearPage(): Unit = document.body.innerHTML = ""


def createHeaderButton(name : String) : Button = {
  val button = document.createElement("button").asInstanceOf[Button]
    button.textContent = name
    button.style.background = "transparent"
    button.style.color = "white"
    button.style.border = "none"
    button.style.cursor = "pointer"
    button.style.fontSize = "16px"

    button
}

def createHomeButton(): Button = {
  val homeBtn = createHeaderButton("Home")
  homeBtn.onclick = (_: dom.MouseEvent) => {
    isPatient().foreach { patient =>
      if (patient) {
        HomePage.render()
      } else {
        AdminPage.render()
      }
    }
  }
  homeBtn
}

def createSubpageHeader(name: String): Div = {
    val header = document.createElement("div").asInstanceOf[Div]
    header.style.backgroundColor = "purple"
    header.style.color = "white"
    header.style.padding = "10px"
    header.style.display = "flex"
    header.style.setProperty("justify-content", "space-between")
    header.style.setProperty("align-items", "center")
    header.style.position = "fixed"
    header.style.top = "0"
    header.style.left = "0"
    header.style.right = "0"
    header.style.height = "50px"
    header.style.zIndex = "1"
  

    val title = document.createElement("div")
    title.textContent = name
    title.asInstanceOf[Div].style.fontSize = "20px"
    title.asInstanceOf[Div].style.fontWeight = "bold"
    title.asInstanceOf[Div].style.margin = "0 auto"
    title.asInstanceOf[Div].style.position = "absolute"
    title.asInstanceOf[Div].style.left = "50%"
    title.asInstanceOf[Div].style.transform = "translateX(-50%)"

    header.appendChild(createHomeButton())
    header.appendChild(title)

    header
}

def createBlankHeaderWithTitle(): Div = {
    val header = document.createElement("div").asInstanceOf[Div]
    header.style.backgroundColor = "purple"
    header.style.color = "white"
    header.style.padding = "10px"
    header.style.display = "flex"
    header.style.setProperty("justify-content", "space-between")
    header.style.setProperty("align-items", "center")
    header.style.position = "fixed"
    header.style.top = "0"
    header.style.left = "0"
    header.style.right = "0"
    header.style.height = "50px"
    header.style.zIndex = "1"

    val title = document.createElement("div").asInstanceOf[Div]
    title.textContent = "Dentana"
    title.style.fontSize = "20px"
    title.style.fontWeight = "bold"
    title.style.margin = "0 auto"
    title.style.position = "absolute"
    title.style.left = "50%"
    title.style.transform = "translateX(-50%)"

    header.appendChild(title)

    header
}

def createBlankHeader(name: String): Div = {
    val header = document.createElement("div").asInstanceOf[Div]
    header.style.backgroundColor = "purple"
    header.style.color = "white"
    header.style.padding = "10px"
    header.style.textAlign = "center"
    header.style.position = "fixed"
    header.style.top = "0"
    header.style.left = "0"
    header.style.right = "0"
    header.style.height = "50px"
    header.style.zIndex = "1"

    val title = document.createElement("div")
    title.textContent = name
    title.asInstanceOf[Div].style.fontSize = "20px"
    title.asInstanceOf[Div].style.fontWeight = "bold"
    header.appendChild(title)

    header
  }

// Helper function to create input fields
def createFormField(container: org.scalajs.dom.Element, placeholder: String, inputType: String = "text"): Input = {
  val input = document.createElement("input").asInstanceOf[Input]
  input.placeholder = placeholder
  input.`type` = inputType
  input.setAttribute("style", "display: block; width: 100%; margin-bottom: 10px; padding: 10px; box-sizing: border-box;")
  container.appendChild(input)
  input
}

// Helper function to create buttons
def createFormButton(container: org.scalajs.dom.Element, text: String): Button = {
  val button = document.createElement("button").asInstanceOf[Button]
  button.textContent = text
  button.setAttribute("style", "display: block; width: 100%; padding: 10px; box-sizing: border-box;")
  container.appendChild(button)
  button
}

val SUPABASE_ANON_KEY: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRqa3JyeXphZnVvZnlldmdjeWljIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg0ODM1OTAsImV4cCI6MjA2NDA1OTU5MH0.s53JxFGfcdKELvKvjs7qqFbPK6DFwqt4k5GMTXFD1Vc"
val supabaseAuthUrl: String = "https://djkrryzafuofyevgcyic.supabase.co/auth/v1/token?grant_type=password"
lazy val supabaseUrl: String = "https://djkrryzafuofyevgcyic.supabase.co/auth/v1"
val supabaseUserUrl: String = supabaseUrl + "/user"
val supabaseRoleUrl: String = supabaseUrl + "/role"

def verifyToken(accessToken: String): scala.concurrent.Future[Boolean] = {
  val requestOptions = literal(
    method = "GET",
    headers = js.Dictionary(
      "Authorization" -> s"Bearer $accessToken",
      "apikey" -> SUPABASE_ANON_KEY
    )
  ).asInstanceOf[RequestInit]

  dom.fetch(supabaseUserUrl, requestOptions).toFuture.flatMap { response =>
    if (response.ok) scala.concurrent.Future.successful(true)
    else scala.concurrent.Future.successful(false)
  }.recover { case _ => false }
}

// account pages

  def buildProfileCard(user: User, isPatient:Boolean): Div = {
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
    if (isPatient)
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


   def buildDeleteAccountButton(): Button = {
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

        dom.fetch("/api/auth/deleteAccount", requestInit)
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

  def fetchUserDetails(userId: String): Future[User] = {
    val accessToken = dom.window.localStorage.getItem("accessToken")

    if (accessToken == null) {
      dom.window.alert("You are not logged in.")
      User("Unknown", "Unknown", "Unknown")
    }

    val requestHeaders = new dom.Headers()
    requestHeaders.append("Content-Type", "application/json")
    requestHeaders.append("Authorization", s"Bearer $accessToken")

    val requestBody = userId.toString

    val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.POST
      headers = requestHeaders
      body = JSON.stringify(requestBody)
    }

    dom.fetch("/api/auth/accountDetails", requestInit)
      .toFuture
      .flatMap(_.json().toFuture)
      .map { json =>
        val user = json.asInstanceOf[js.Dynamic]
        val name = s"${user.first_name.asInstanceOf[String]} ${user.last_name.asInstanceOf[String]}"
        val dob = user.dob.asInstanceOf[String]
        val address = "placeholder address" 
        User(name, dob, address)
      }
      .recover {
        case e =>
          Spinner.hide()
          dom.window.alert(s"Error: ${e.getMessage}")
          User("Unknown", "Unknown", "Unknown")
      }
  }

case class Clinic(
  clinicId: String,
  name: String,
  latitude: Double,
  longitude: Double,
  address: String
)

def fetchClinicDetails(clinicId: String): Future[Clinic] = {
  val accessToken = dom.window.localStorage.getItem("accessToken")
  val uid = clinicId

  if (accessToken == null || uid == null) {
    dom.window.alert("You are not logged in.")
    return Future.successful(Clinic("unknown", "Unknown Clinic", 0.0, 0.0, ""))
  }

  val requestHeaders = new dom.Headers()
  requestHeaders.append("Content-Type", "application/json")
  requestHeaders.append("Authorization", s"Bearer $accessToken")
  requestHeaders.append("apikey", SUPABASE_ANON_KEY)

  val requestInit = new dom.RequestInit {
    method = dom.HttpMethod.GET
    headers = requestHeaders
  }
  val requestUrl = s"https://djkrryzafuofyevgcyic.supabase.co/rest/v1/clinics?select=clinic_id,name,latitude,longitude,address&clinic_id=eq.${uid}"
  dom.fetch(requestUrl, requestInit)
    .toFuture
    .flatMap(_.json().toFuture)
    .map { json =>
      val clinics = json.asInstanceOf[js.Array[js.Dynamic]]
      if (clinics.nonEmpty) {
        val c = clinics(0)
        Clinic(
          clinicId = c.clinic_id.asInstanceOf[String],
          name = c.name.asInstanceOf[String],
          latitude = c.latitude.asInstanceOf[Double],
          longitude = c.longitude.asInstanceOf[Double],
          address = c.address.asInstanceOf[String]
        )
      } else {
        Clinic("unknown", "Unknown Clinic", 0.0, 0.0, "")
      }
    }
    .recover {
      case e =>
        dom.window.alert(s"Error: ${e.getMessage}")
        Clinic("unknown", "Unknown Clinic", 0.0, 0.0, "")
    }
}


object Spinner {

  private val spinnerId = "global-loading-spinner"
  private val overlayId = "global-spinner-overlay"

  def show(): Unit = {
    if (dom.document.getElementById(spinnerId) != null) return

    // Overlay to dim background
    val overlay = dom.document.createElement("div").asInstanceOf[Div]
    overlay.id = overlayId
    overlay.style.position = "fixed"
    overlay.style.top = "0"
    overlay.style.left = "0"
    overlay.style.width = "100%"
    overlay.style.height = "100%"
    overlay.style.backgroundColor = "rgba(255, 255, 255, 0.7)"
    overlay.style.zIndex = "9998"

    // Spinner
    val spinner = dom.document.createElement("div").asInstanceOf[Div]
    spinner.id = spinnerId
    spinner.style.position = "fixed"
    spinner.style.top = "50%"
    spinner.style.left = "50%"
    spinner.style.width = "80px"
    spinner.style.height = "80px"
    spinner.style.marginLeft = "-30px"
    spinner.style.marginTop = "-30px"
    spinner.style.border = "6px solid #f3f3f3"
    spinner.style.borderTop = "6px solidrgb(128, 0, 128)"
    spinner.style.borderRadius = "50%"
    spinner.style.animation = "spin 1s linear infinite"
    spinner.style.zIndex = "9999"

    val img = dom.document.createElement("img").asInstanceOf[dom.html.Image]
    img.src = "images/Waiting.png"
    img.style.position = "absolute"
    img.style.top = "50%"
    img.style.left = "50%"
    img.style.transform = "translate(-50%, -50%)"
    img.style.width = "60px"
    img.style.height = "60px"

    spinner.appendChild(img)

    // Add @keyframes if not already present
    if (dom.document.getElementById("spinner-style") == null) {
      val styleTag = dom.document.createElement("style")
      styleTag.id = "spinner-style"
      styleTag.innerHTML =
        """
          |@keyframes spin {
          |  0% { transform: rotate(0deg); }
          |  100% { transform: rotate(360deg); }
          |}
        """.stripMargin
      dom.document.head.appendChild(styleTag)
    }

    dom.document.body.appendChild(overlay)
    dom.document.body.appendChild(spinner)
  }

  def hide(): Unit = {
    Option(dom.document.getElementById(spinnerId)).foreach(_.remove())
    Option(dom.document.getElementById(overlayId)).foreach(_.remove())
  }
}

def isPatient(): scala.concurrent.Future[Boolean] = {
  //debugging print
  dom.console.log("Checking if user is a patient...")

  val currentUid = dom.window.localStorage.getItem("userId") match {
    case null => ""
    case uid => uid
  }

  val accessToken = dom.window.localStorage.getItem("accessToken") match {
    case null => ""
    case token => token
  }

  if (currentUid.isEmpty || accessToken.isEmpty) {
    return Future.successful(false)
  }

  val requestBody = currentUid.toString

  val requestHeaders = js.Dictionary(
      "Content-Type" -> "application/json",
      "apikey" -> SUPABASE_ANON_KEY
  )

  val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.POST
      headers = requestHeaders
      body = JSON.stringify(requestBody)
  }



  dom.fetch("/api/auth/roles", requestInit)
      .toFuture
      .flatMap(_.json().toFuture)
      .map { json =>
        val roleInfo = json.asInstanceOf[js.Dynamic]
        val isPatient = roleInfo.is_patient.asInstanceOf[Boolean] == true
        isPatient
      }
      .recover {
        case e =>
          dom.window.alert(s"Error: ${e.getMessage}")
          false
      }

}

def createPasswordInput(container: dom.Element, placeholder: String): Input = {
  val wrapper = document.createElement("div").asInstanceOf[Div]
  wrapper.setAttribute("style",
    """
    position: relative;
    width: 100%;
    margin-bottom: 10px;
    """.stripMargin)

  val input = document.createElement("input").asInstanceOf[Input]
  input.placeholder = placeholder
  input.`type` = "password"
  input.setAttribute("style",
    """
    width: 100%;
    padding: 10px 40px 10px 10px;
    box-sizing: border-box;
    """.stripMargin)

  val toggleButton = document.createElement("button").asInstanceOf[Button]
  toggleButton.innerHTML = "&#128065;"
  toggleButton.setAttribute("aria-label", "Toggle password visibility")
  toggleButton.setAttribute("style",
    """
    position: absolute;
    top: 50%;
    right: 10px;
    transform: translateY(-50%);
    background: none;
    border: none;
    cursor: pointer;
    font-size: 18px;
    padding: 0;
    """.stripMargin)

  toggleButton.onclick = (_: dom.MouseEvent) => {
    input.`type` = if (input.`type` == "password") "text" else "password"
  }

  wrapper.appendChild(input)
  wrapper.appendChild(toggleButton)
  container.appendChild(wrapper)

  input
}

def createModal(): Unit = {
  if (document.getElementById("modal-overlay") != null) return

  val overlay = document.createElement("div")
  overlay.id = "modal-overlay"
  overlay.asInstanceOf[dom.html.Element].className = "hidden"
  overlay.setAttribute("style",
    """position: fixed; top: 0; left: 0; right: 0; bottom: 0;
      |background: rgba(0,0,0,0.4); display: flex;
      |align-items: center; justify-content: center;
      |z-index: 1000;""".stripMargin)

  overlay.innerHTML =
    """<div id="modal-window" style="background: white; border-radius: 10px; padding: 20px; width: 90%; max-width: 500px; max-height: 80%; overflow-y: auto; position: relative;">
      |  <button id="modal-close" style="position: absolute; top: 10px; right: 10px;">Back</button>
      |  <div id="modal-content"></div>
      |</div>""".stripMargin

  document.body.appendChild(overlay)

  overlay.addEventListener("click", (e: dom.MouseEvent) => {
    val modalWindow = dom.document.getElementById("modal-window")
    modalWindow.addEventListener("click", (e: dom.MouseEvent) => {
      e.stopPropagation()
    })
    val target = e.target.asInstanceOf[dom.Node]
    if (!modalWindow.contains(target)) {
      hideModal()
    }
  })


  val closeBtn = document.getElementById("modal-close")
  if (closeBtn != null) {
    closeBtn.addEventListener("click", (_: dom.Event) => hideModal())
  }
}

def showModal(contentElement: dom.html.Element): Unit = {
  createModal()
  val overlay = document.getElementById("modal-overlay")
  val content = document.getElementById("modal-content")

  if (content != null) {
    // Clear existing content
    content.innerHTML = ""
    // Append the DOM element
    content.appendChild(contentElement)
  }

  if (overlay != null) {
    overlay.classList.remove("hidden")
  }
}

def showModal(contentHtml: String, forceReplace: Boolean = true): Unit = {
  createModal()
  val overlay = document.getElementById("modal-overlay")
  val content = document.getElementById("modal-content")

  if (content != null && forceReplace) {
    content.innerHTML = contentHtml
  }

  if (overlay != null) {
    overlay.classList.remove("hidden")
  }
}

def replaceModalContent(newContentHtml: String): Unit = {
  println("Replacing modal content...")
  val content = dom.document.getElementById("modal-content")
  if (content != null) {
    content.innerHTML = newContentHtml
    println("Modal content updated.")
  } else {
    println("Modal content container not found!")
  }
}

def hideModal(): Unit = {
  val overlay = document.getElementById("modal-overlay")
  if (overlay != null) {
    overlay.classList.add("hidden")
    val content = document.getElementById("modal-content")
    if (content != null) content.innerHTML = "" // clear contents
  }
}

def happyLogo(): html.Element = {
  val img = document.createElement("img").asInstanceOf[html.Image]
  img.src = "images/DentanaLogoHappy.png"
  img.alt = "Dentana Happy Tooth"
  img.width = 50
  img.style.margin = "20px"
  img
}

private def fetchUnreadCount(): Future[Int] = {
  val accessToken = dom.window.localStorage.getItem("accessToken")
  val uid = dom.window.localStorage.getItem("userId")

  if (accessToken == null || uid == null) {
    dom.window.alert("You are not logged in.")
    Future.successful(0)
  } else {
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
      .flatMap(_.json().toFuture)
      .map { jsValue =>
        if (js.Array.isArray(jsValue)) {
          val arr = jsValue.asInstanceOf[js.Array[js.Dynamic]]
          val parsed = Inbox.parseNotifications(arr)
          parsed.count(!_.isRead)
        } else {
          println("Unexpected response format")
          0
        }
      }
      .recover {
        case e =>
          println(s"Failed to fetch unread notifications: ${e.getMessage}")
          0
      }
  }
}

def formatSlotTime(slotTime: String): String = {
    val dt = new js.Date(slotTime)
    val year = dt.getUTCFullYear()
    val month = (dt.getUTCMonth() + 1).toInt
    val day = dt.getUTCDate().toInt
    val hour = dt.getUTCHours().toInt
    val minute = dt.getUTCMinutes().toInt
    f"$year-$month%02d-$day%02d $hour%02d:$minute%02d UTC"
}

def buildClinicProfileCard(clinic: Clinic): Div = {
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
  heading.textContent = "Clinic Details"
  heading.style.marginBottom = "24px"
  card.appendChild(heading)

  def infoRow(label: String, value: String): Div = {
    val row = styledDiv("marginBottom" -> "20px")
    row.innerHTML = s"<strong>$label</strong> $value"
    row
  }

  card.appendChild(infoRow("Clinic Name:", clinic.name))
  card.appendChild(infoRow("Address:", clinic.address))


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