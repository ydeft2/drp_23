package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html
import org.scalajs.dom.html.{Div, Button, Image, Input}
import org.scalajs.dom.experimental._
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.Thenable.Implicits._
import scala.scalajs.js.annotation._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object RegisterPage {

  def render(): Unit = {

    document.body.innerHTML = ""

    val mainContainer = document.createElement("div")
    mainContainer.setAttribute("style", "display: flex; flex-direction: column; align-items: center; gap: 10px;")
    document.body.appendChild(mainContainer)
    document.body.appendChild(fancyBackButton())

    val logo = document.createElement("img").asInstanceOf[html.Image]
    logo.src = "images/DentanaTitleBlack.png"
    logo.alt = "Dentana Logo"
    logo.setAttribute("style", "height: 40px;")

    val logoWrapper = document.createElement("div")
    logoWrapper.setAttribute(
      "style",
      """
        |width: 100%;
        |text-align: center;
        |margin-top: 20px;
        |margin-bottom: 20px;
      """.stripMargin.replaceAll("\n", "")
    )
    logoWrapper.appendChild(logo)
    mainContainer.appendChild(logoWrapper)
  
    // Create a grey box container for the registration form
    val container = document.createElement("div")
    container.setAttribute("class", "login-card")
    mainContainer.appendChild(container)

    val welcomeText = dom.document.createElement("div").asInstanceOf[Div]
    welcomeText.innerHTML = "Register"
    welcomeText.setAttribute(
      "style",
      "font-weight: bold; font-size: 32px; margin-bottom: 20px"
    )
    container.appendChild(welcomeText)

    val firstNameInput = createFormField(container, "First Name")
    val lastNameInput = createFormField(container, "Last Name")
    
    val dobInput = createFormField(container, "", "date")
    val houseNumberInput = createFormField(container, "House Number")
    val streetInput = createFormField(container, "Street")
    val townInput = createFormField(container, "Town/City")
    val postcodeInput = createFormField(container, "Postcode")
    val emailInput = createFormField(container, "Email")
    val passwordInput = createPasswordInput(container, "Password")
    val confirmPasswordInput = createPasswordInput(container, "Confirm Password")

    val registerButton = createFormButton(container, "Register")

    // Add click event listener to the Register button
    registerButton.addEventListener("click", { (_: dom.Event) =>

      val rawAddress =
        s"${houseNumberInput.value.trim} ${streetInput.value.trim.split(" ").map(_.capitalize).mkString(" ")}, ${townInput.value.trim.split(" ").map(_.capitalize).mkString(" ")}, ${postcodeInput.value.trim.toUpperCase}"

      val isInvalidInput =
        firstNameInput.value.trim.isEmpty ||
        lastNameInput.value.trim.isEmpty ||
        dobInput.value.trim.isEmpty ||
        emailInput.value.trim.isEmpty ||
        passwordInput.value.trim.length < 6 ||
        passwordInput.value != confirmPasswordInput.value ||
        houseNumberInput.value.trim.isEmpty ||
        streetInput.value.trim.isEmpty ||
        townInput.value.trim.isEmpty ||
        postcodeInput.value.trim.isEmpty


      if (isInvalidInput) {
        dom.window.alert("Please fill in all fields correctly, including a valid address.")
      } else {
        geocodeAddress(rawAddress).foreach {
          case Some((lat, lon)) =>
            val data = js.Dynamic.literal(
              "first_name" -> firstNameInput.value,
              "last_name" -> lastNameInput.value,
              "dob" -> dobInput.value,
              "email" -> emailInput.value,
              "password" -> passwordInput.value,
              "address" -> rawAddress,
              "latitude" -> js.Any.fromFloat(lat),
              "longitude" -> js.Any.fromFloat(lon)
            )

            dom.fetch("/api/auth/register", literal(
              method = "POST",
              body = js.JSON.stringify(data),
              headers = js.Dictionary("Content-Type" -> "application/json")
            ).asInstanceOf[RequestInit]).toFuture.map { response =>
              if (response.ok) {
                dom.window.alert("Registration successful!")
                LoginPage.render()
              } else {
                dom.window.alert("Registration failed. Please try again.")
              }
            }.recover {
              case e: Throwable => dom.window.alert(s"An error occurred: ${e.getMessage}")
            }

          case None =>
            dom.window.alert("Address could not be geocoded. Please enter a valid address.")
        }
      }
    })

    // Add the container to the main container
    mainContainer.appendChild(container)

  }

  def fancyBackButton(): dom.Element = {
    val backButtonWrapper = document.createElement("div")
    backButtonWrapper.setAttribute(
      "style",
      """
        |position: absolute;
        |top: 20px;
        |left: 20px;
        |z-index: 10;
      """.stripMargin.replaceAll("\n", "")
    )

    val backButton = document.createElement("button").asInstanceOf[html.Button]
    backButton.textContent = "Back to Login"
    backButton.setAttribute(
      "style",
      """
        |background-color: white;
        |color: #7a5fa4;
        |border: 1px solid #ccc;
        |border-radius: 10px;
        |padding: 10px 20px;
        |font-size: 16px;
        |font-weight: 600;
        |cursor: pointer;
        |font-family: 'Poppins', sans-serif;
        |box-shadow: 0 4px 10px rgba(0,0,0,0.1);
        |transition: background-color 0.2s ease;
      """.stripMargin.replaceAll("\n", "")
    )

    backButton.addEventListener("mouseover", (_: dom.Event) => {
      backButton.style.backgroundColor = "#f0f0f0"
    })
    backButton.addEventListener("mouseout", (_: dom.Event) => {
      backButton.style.backgroundColor = "white"
    })

    backButton.addEventListener("click", (_: dom.Event) => {
      document.body.innerHTML = "" // Clear the body content
      LoginPage.render()
    })

    backButtonWrapper.appendChild(backButton)
    backButtonWrapper
  }

  def geocodeAddress(address: String): Future[Option[(Float, Float)]] = {
    val encodedAddress = js.URIUtils.encodeURIComponent(address)
    val url = s"https://nominatim.openstreetmap.org/search?q=$encodedAddress&format=json&limit=1"

    dom.fetch(url, new RequestInit {
      method = HttpMethod.GET
      headers = js.Dictionary("User-Agent" -> "DentanaApp/1.0 (guyconnolly15@gmail.com)")
    }).toFuture.flatMap(_.text().toFuture).map { jsonStr =>
      val results = JSON.parse(jsonStr).asInstanceOf[js.Array[js.Dynamic]]
      if (results.length > 0) {
        val lat = results(0).lat.asInstanceOf[String].toFloat
        val lon = results(0).lon.asInstanceOf[String].toFloat
        Some((lat, lon))
      } else {
        None
      }
    }
  }
}
