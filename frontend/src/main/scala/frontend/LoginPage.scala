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
    // Clear the document body
    document.body.innerHTML = ""
    
    // Create purple banner with the title "Dentana"
    val banner = document.createElement("div")
    banner.innerHTML = "Dentana"
    banner.setAttribute("style", "background-color: purple; color: white; text-align: center; padding: 20px; font-size: 24px;")
    document.body.appendChild(banner)
    
    // Create a grey box container for the login form
    val container = document.createElement("div")
    container.setAttribute("style", "margin: 50px auto; width: 300px; padding: 20px; background-color: lightgrey; border-radius: 5px;")
    document.body.appendChild(container)
    
    // Create an email input field
    val emailInput = document.createElement("input").asInstanceOf[Input]
    emailInput.placeholder = "Email"
    emailInput.setAttribute("style", "display: block; width: 100%; margin-bottom: 10px; padding: 10px; box-sizing: border-box;")
    container.appendChild(emailInput)
    
    // Create a password input field
    val passwordInput = document.createElement("input").asInstanceOf[Input]
    passwordInput.`type` = "password"
    passwordInput.placeholder = "Password"
    passwordInput.setAttribute("style", "display: block; width: 100%; margin-bottom: 10px; padding: 10px; box-sizing: border-box;")
    container.appendChild(passwordInput)
    
    // Create the Login button
    val loginButton = document.createElement("button")
    loginButton.textContent = "Login"
    loginButton.setAttribute("style", "display: block; width: 100%; margin-bottom: 10px; padding: 10px; box-sizing: border-box;")
    container.appendChild(loginButton)
    
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
        headers = js.Dictionary("Content-Type" -> "application/json"),
        body = JSON.stringify(data)
      ).asInstanceOf[RequestInit]
      
      // Send a HTTPS fetch request to verify the credentials
      dom.fetch("/api/login", requestOptions).toFuture.flatMap { response =>
        response.text().toFuture
      }.foreach { text =>
        dom.console.log(s"Login response: $text")
      }
    })
    
    // Add click event listener to the Register button
    registerButton.addEventListener("click", { (_: dom.Event) =>
      RegisterPage.render()
    })

    // THE FOLLOWING BUTTON IS FOR TESTING PURPOSES ONLY
    val secretBypassButton = document.createElement("button")
    secretBypassButton.textContent = "Bypass Login"
    secretBypassButton.setAttribute("style", "display: block; width: 100%; padding: 10px; box-sizing: border-box;")
    secretBypassButton.setAttribute("style", "position: fixed; bottom: 0; left: 0; right: 0; display: block; padding: 10px; box-sizing: border-box;")
    document.body.appendChild(secretBypassButton)
    secretBypassButton.addEventListener("click", { (_: dom.Event) =>
      HomePage.render()
    })
  }
}