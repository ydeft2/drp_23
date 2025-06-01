package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal
import scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalajs.dom.experimental.RequestInit



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
val supabaseUserUrl: String = "https://djkrryzafuofyevgcyic.supabase.co/auth/v1/user"

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
