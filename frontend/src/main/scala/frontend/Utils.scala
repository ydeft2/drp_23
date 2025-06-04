package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
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

    // Home button
    val homeBtn = createHeaderButton("Home")
    homeBtn.onclick = (_: dom.MouseEvent) => HomePage.render()

    val title = document.createElement("div")
    title.textContent = name
    title.asInstanceOf[Div].style.fontSize = "20px"
    title.asInstanceOf[Div].style.fontWeight = "bold"
    title.asInstanceOf[Div].style.margin = "0 auto"
    title.asInstanceOf[Div].style.position = "absolute"
    title.asInstanceOf[Div].style.left = "50%"
    title.asInstanceOf[Div].style.transform = "translateX(-50%)"

    header.appendChild(homeBtn)
    header.appendChild(title)

    header
}

def createBlankHeaderWithTitle(): Div = {
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
    title.textContent = "Dentana"
    title.asInstanceOf[Div].style.fontSize = "20px"
    title.asInstanceOf[Div].style.fontWeight = "bold"

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
    spinner.style.width = "60px"
    spinner.style.height = "60px"
    spinner.style.marginLeft = "-30px"
    spinner.style.marginTop = "-30px"
    spinner.style.border = "6px solid #f3f3f3"
    spinner.style.borderTop = "6px solid #3498db"
    spinner.style.borderRadius = "50%"
    spinner.style.animation = "spin 1s linear infinite"
    spinner.style.zIndex = "9999"

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

  val requestBody = js.Dynamic.literal(
    "uid" -> currentUid,
    "accessToken" -> accessToken
  )

  val requestHeaders = js.Dictionary(
      "Content-Type" -> "application/json",
      "apikey" -> SUPABASE_ANON_KEY
  )

  val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.POST
      headers = requestHeaders
      body = JSON.stringify(requestBody)
  }



  dom.fetch("/api/roles", requestInit)
      .toFuture
      .flatMap(_.json().toFuture)
      .map { json =>
        val roleInfo = json.asInstanceOf[js.Dynamic]
        val isPatient = roleInfo.is_patient.asInstanceOf[String] == "true"
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

  val modalWindow = document.getElementById("modal-window")
  overlay.addEventListener("click", (e: dom.MouseEvent) => {
    if (!modalWindow.contains(e.target.asInstanceOf[dom.Node])) {
      hideModal()
    }
  })


  val closeBtn = document.getElementById("modal-close")
  if (closeBtn != null) {
    closeBtn.addEventListener("click", (_: dom.Event) => hideModal())
  }
}

def showModal(contentHtml: String): Unit = {
  createModal()
  val overlay = document.getElementById("modal-overlay")
  val content = document.getElementById("modal-content")
  content.innerHTML = contentHtml
  overlay.classList.remove("hidden")
}

def hideModal(): Unit = {
  val overlay = document.getElementById("modal-overlay")
  if (overlay != null) overlay.classList.add("hidden")
}

