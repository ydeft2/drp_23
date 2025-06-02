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

    document.body.appendChild(createBlankHeaderWithTitle())
  
    // Create a grey box container for the registration form
    val container = document.createElement("div")
    container.setAttribute("style", "margin: 100px auto 50px auto; width: 300px; padding: 20px; background-color: lightgrey; border-radius: 5px;")
    document.body.appendChild(container)

    val firstNameInput = createFormField(container, "First Name")
    val lastNameInput = createFormField(container, "Last Name")
    
    val dobInput = createFormField(container, "", "date")
    // TODO ADDRESSES!
    val emailInput = createFormField(container, "Email")
    val passwordInput = createPasswordInput(container, "Password")
    val confirmPasswordInput = createPasswordInput(container, "Confirm Password")

    // Append inputs to the container

    val registerButton = createFormButton(container, "Register")

    // Add click event listener to the Register button
    registerButton.addEventListener("click", { (_: dom.Event) =>

      dom.console.log(dobInput.value)

      val data = js.Dynamic.literal(
        "firstName" -> firstNameInput.value,
        "lastName" -> lastNameInput.value,
        "dob" -> dobInput.value,
        "email" -> emailInput.value,
        "password" -> passwordInput.value
      )

      // Validate inputs
      val isInvalid = data.firstName.asInstanceOf[String].isEmpty ||
          data.lastName.asInstanceOf[String].isEmpty ||
          data.dob.asInstanceOf[String].isEmpty ||
          data.email.asInstanceOf[String].isEmpty ||
          data.password.asInstanceOf[String].isEmpty ||
          data.password.asInstanceOf[String].length < 6 ||
          data.password.asInstanceOf[String] != confirmPasswordInput.value
      
      if (isInvalid) {
        dom.window.alert("Please fill in all fields correctly, ensure passwords are at least 6 characters and match.")
      } else {
        dom.window.fetch("/api/register", literal(
          method = "POST",
          body = js.JSON.stringify(data),
          headers = js.Dictionary("Content-Type" -> "application/json")
        ).asInstanceOf[RequestInit]).toFuture.map { response =>
          if (response.ok) {
            dom.window.alert("Registration successful!")
            LoginPage.render() // Redirect to login page
          } else {
            dom.window.alert("Registration failed. Please try again.")
          }
        }.recover {
          case e: Throwable => dom.window.alert(s"An error occurred: ${e.getMessage}")
        }
      } 
    })
  }
}
