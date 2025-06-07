package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._
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
    Layout.renderPage (contentRender = () => {
      // Create a grey box container for the login form
      val container = document.createElement("div")
      container.setAttribute("style", "position: relative; margin: 200px auto 50px auto; width: 300px; padding: 120px 20px 20px 20px; background-color: lightgrey; border-radius: 5px;border: 2px solidrgb(128, 127, 127);")

      document.body.appendChild(container)
      val welcomeText = dom.document.createElement("div").asInstanceOf[Div]
      welcomeText.innerHTML = "Login"
      welcomeText.setAttribute(
        "style",
        "font-weight: bold; font-size: 32px; text-align: left; position: absolute; top: 20px; left: 20px;"
      )
      container.appendChild(welcomeText)
      val logoContainer = document.createElement("div")
      logoContainer.setAttribute(
        "style",
        "position: absolute; top: 10px; right: 10px;"
      )
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
      

      // Create the Register button
      val registerButton = document.createElement("button")
      registerButton.textContent = "New Here? Register!"
      registerButton.setAttribute(
        "style",
        "display: block; width: 40%; padding: 10px; box-sizing: border-box; " +
        "background-color: purple; color: white; border: none; border-radius: 4px; " +
        "font-size: 16px; cursor: pointer; margin: 10px auto 0 auto; box-shadow: 0 4px 6px rgba(0,0,0,0.1);"
      )
      document.body.appendChild(registerButton)

      
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
      
      // Add click event listener to the Register button
      registerButton.addEventListener("click", { (_: dom.Event) =>
        RegisterPage.render()
      })
    })
  }
}