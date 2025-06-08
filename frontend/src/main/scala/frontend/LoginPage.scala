package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
import org.scalajs.dom.html
import org.scalajs.dom.experimental._
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.Thenable.Implicits._
import scala.scalajs.js.annotation._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.concurrent.ExecutionContext.Implicits.global

object LoginPage {
  def render(): Unit = {

    val mainContainer = document.createElement("div")
    mainContainer.setAttribute("style", "display: flex; flex-direction: column; align-items: center; gap: 10px;")


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
        |margin-bottom: 70px;
      """.stripMargin.replaceAll("\n", "")
    )
    logoWrapper.appendChild(logo)
    mainContainer.appendChild(logoWrapper)

    // Create a grey box container for the login form
    val container = document.createElement("div")
    container.setAttribute("class", "login-card")

    document.body.appendChild(container)
    val welcomeText = dom.document.createElement("div").asInstanceOf[Div]
    welcomeText.innerHTML = "Login"
    welcomeText.setAttribute(
      "style",
      "font-weight: bold; font-size: 32px"
    )
    container.appendChild(welcomeText)
    val logoContainer = document.createElement("div")
    
    logoContainer.appendChild(happyLogo())
    container.appendChild(logoContainer)
    // Create an email input field
    val emailInput = createFormField(container, "Email")
    
    // Create a password input field
    val passwordInput = createPasswordInput(container, "Password")
    
    val errorMessage = document.createElement("div")
    errorMessage.setAttribute("style", "color: red; text-align: center; margin-bottom: 10px; display: none;")
    container.appendChild(errorMessage)

    // Create the Login button
    val loginButton = createFormButton(container, "Login")

    
    // Add click event listener to the Login button
    loginButton.addEventListener("click", { (_: dom.Event) =>
      
      val data = literal(
        "email" -> emailInput.value,
        "password" -> passwordInput.value
      )
      
      // Prepare request options by casting a literal to RequestInit
      val requestOptions = literal(
        method = "POST",
        headers = js.Dictionary(
          "Content-Type" -> "application/json",
          "apikey" -> SUPABASE_ANON_KEY),
        body = JSON.stringify(data)
      ).asInstanceOf[RequestInit]
      
      // Send a HTTPS fetch request to verify the credentials
      dom.fetch(supabaseAuthUrl, requestOptions).toFuture.flatMap { response =>
        if(response.ok) {
          response.json().toFuture.map {json =>
            val dyn = json.asInstanceOf[js.Dynamic]
            val accessToken = dyn.access_token.asInstanceOf[String]
            val userId = dyn.user.id.asInstanceOf[String]
            
            dom.window.localStorage.setItem("accessToken", accessToken)
            dom.window.localStorage.setItem("userId", userId)
            
            isPatient().map { isPatient =>
              if (isPatient) {
                HomePage.render()
              } else {
                AdminPage.render()
              }
            }.recover {
              case e: Throwable => dom.window.alert(s"An error occurred: ${e.getMessage}")
            }
        }
      } else {
          response.text().toFuture.map { errorText =>
            errorMessage.textContent = "Incorrect email or password"
            errorMessage.setAttribute("style", "color: red; text-align: center; margin-bottom: 10px; display: block;")
          }
        }
      }.recover {
        case e: Throwable => dom.window.alert(s"An error occurred: ${e.getMessage}")
      }
    })

    val registerText = document.createElement("div")
    registerText.setAttribute("style", "text-align: center; margin-top: 20px; font-size: 14px; font-family: 'Poppins', sans-serif;")

    registerText.innerHTML =
      """Don't have an account? <span id="signup-link" style="color: purple; cursor: pointer; text-decoration: underline;">Sign up</span>"""

    container.appendChild(registerText)

    val signUpLink = document.getElementById("signup-link")
    signUpLink.addEventListener("click", (_: dom.Event) => {
      RegisterPage.render()
    })

    mainContainer.appendChild(container)

    document.body.appendChild(mainContainer)

    Layout.renderFooter()
  }
}