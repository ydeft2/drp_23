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
    Layout.renderPage(
      leftButton  = None,
      rightButton = None,
      contentRender = () => {

        // 1) Outer wrapper
        val mainContainer = document.createElement("div").asInstanceOf[Div]
        mainContainer.style.cssText =
          """
            |display: flex;
            |flex-direction: column;
            |align-items: center;
            |gap: 10px;
            |padding-top: 20px;
          """.stripMargin

        // 2) Logo
        val logo = document.createElement("img").asInstanceOf[html.Image]
        logo.src = "images/DentanaTitleBlack.png"
        logo.alt = "Dentana Logo"
        logo.style.height = "40px"
        mainContainer.appendChild(logo)

        // 3) Card container
        val container = document.createElement("div").asInstanceOf[Div]
        container.classList.add("login-card")
        mainContainer.appendChild(container)

        // 4) Title + fun logo
        val title = document.createElement("div").asInstanceOf[Div]
        title.textContent = "Login"
        title.style.cssText = "font-weight:500;font-size:32px;text-align:center;margin-bottom:20px;"
        container.appendChild(title)
        container.appendChild(happyLogo())

        // 5) Inputs
        val emailInput = createFormField(container, "Email")
        val passwordInput = createPasswordInput(container, "Password")

        // 6) Error message placeholder
        val errorMessage = document.createElement("div").asInstanceOf[Div]
        errorMessage.style.cssText = "color:red;text-align:center;display:none;margin-bottom:10px;"
        container.appendChild(errorMessage)

        // 7) Login button
        val loginButton = createFormButton(container, "Login")

        // allow Enter to submit
        def handleEnterKey(e: dom.KeyboardEvent): Unit =
          if (e.key == "Enter") loginButton.click()
        emailInput.addEventListener("keydown", handleEnterKey _)
        passwordInput.addEventListener("keydown", handleEnterKey _)

        // 8) Your exact fetch/auth logic
        loginButton.addEventListener("click", (_: dom.Event) => {
          // build payload
          val data = literal(
            "email"    -> emailInput.value,
            "password" -> passwordInput.value
          )
          val requestOptions = literal(
            method  = "POST",
            headers = js.Dictionary(
              "Content-Type" -> "application/json",
              "apikey"       -> SUPABASE_ANON_KEY
            ),
            body = JSON.stringify(data)
          ).asInstanceOf[RequestInit]

          dom.fetch(supabaseAuthUrl, requestOptions).toFuture.flatMap { response =>
            if (response.ok) {
              response.json().toFuture.map { json =>
                val dyn         = json.asInstanceOf[js.Dynamic]
                val accessToken = dyn.access_token.asInstanceOf[String]
                val userId      = dyn.user.id.asInstanceOf[String]

                dom.window.localStorage.setItem("accessToken", accessToken)
                dom.window.localStorage.setItem("userId", userId)

                // now route to Home or Admin
                isPatient().foreach { isPatient =>
                  if (isPatient) HomePage.render()
                  else           AdminPage.render()
                }
              }
            } else {
              // wrong creds
              response.text().toFuture.map { _ =>
                errorMessage.textContent = "Incorrect email or password"
                errorMessage.style.display = "block"
              }
            }
          }.recover { case e =>
            dom.window.alert(s"An error occurred: ${e.getMessage}")
          }
        })

        // 9) “Sign up” link
        val registerText = document.createElement("div").asInstanceOf[Div]
        registerText.innerHTML =
          """Don't have an account? <span id="signup-link"
            |style="color:purple;cursor:pointer;text-decoration:underline;">Sign up</span>"""
            .stripMargin
        container.appendChild(registerText)
        document.getElementById("signup-link")
          .addEventListener("click", (_: dom.Event) => RegisterPage.render())

        // 10) Finally mount into #app
        document.body.appendChild(mainContainer)
      }
    )
  }
}