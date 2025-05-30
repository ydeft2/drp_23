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


object RegisterPage {

  def render(): Unit = {

    document.body.innerHTML = ""

    // Create purple banner with the title "Dentana"
    val banner = document.createElement("div")
    banner.innerHTML = "Dentana"
    banner.setAttribute("style", "background-color: purple; color: white; text-align: center; padding: 20px; font-size: 24px;")
    document.body.appendChild(banner)  
  
    // Create a grey box container for the registration form
    val container = document.createElement("div")
    container.setAttribute("style", "margin: 50px auto; width: 300px; padding: 20px; background-color: lightgrey; border-radius: 5px;")
    document.body.appendChild(container)
    // Create a firstname input field
    val firstNameInput = document.createElement("input").asInstanceOf[Input]
    firstNameInput.placeholder = "First Name"
    firstNameInput.setAttribute("style", "display: block; width: 100%; margin-bottom: 10px; padding: 10px; box-sizing: border-box;")
    container.appendChild(firstNameInput)

    // Create a last name input field
    val lastNameInput = document.createElement("input").asInstanceOf[Input]
    lastNameInput.placeholder = "Last Name"
    lastNameInput.setAttribute("style", "display: block; width: 100%; margin-bottom: 10px; padding: 10px; box-sizing: border-box;")
    container.appendChild(lastNameInput)

    // Create a DOB input field
    val dobInput = document.createElement("input").asInstanceOf[Input]
    dobInput.placeholder = "Date of Birth (YYYY-MM-DD)"
    dobInput.setAttribute("style", "display: block; width: 100%; margin-bottom: 10px; padding: 10px; box-sizing: border-box;")
    container.appendChild(dobInput)
    
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

    // Create the Register button
    val registerButton = document.createElement("button").asInstanceOf[Button]
    registerButton.textContent = "Register"
    registerButton.setAttribute("style", "display: block; width: 100%; padding: 10px; box-sizing: border-box;")
    container.appendChild(registerButton)
    // Add click event listener to the Register button
    registerButton.addEventListener("click", { (_: dom.Event) =>
      val data = js.Dynamic.literal(
        "firstName" -> firstNameInput.value,
        "lastName" -> lastNameInput.value,
        "dob" -> dobInput.value,
        "email" -> emailInput.value,
        "password" -> passwordInput.value
      )

      dom.window.fetch("/api/register", literal(
        method = "POST",
        body = js.JSON.stringify(data),
        headers = js.Dictionary("Content-Type" -> "application/json"),
      ).asInstanceOf[dom.RequestInit]).toFuture.map { response =>
        if (response.ok) {
          dom.window.alert("Registration successful!")
          LoginPage.render() // Redirect to login page
        } else {
          dom.window.alert("Registration failed. Please try again.")
        }
      }.recover {
        case e: Throwable => dom.window.alert(s"An error occurred: ${e.getMessage}")
      }
    })
  }


}
