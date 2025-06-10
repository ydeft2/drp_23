package frontend

import scala.concurrent.Future
import scala.scalajs.js
import org.scalajs.dom
import org.scalajs.dom.html
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Success, Failure}

object MessagesPage {

  private var messages: js.Array[js.Dynamic] = js.Array()

  def render(): Unit = {
    Spinner.show()
    fetchMessages { () =>
      Layout.renderPage(
        leftButton = Some(createHomeButton()),
        contentRender = () => 
        {
          buildMessagesPage()
          Spinner.hide()
        }
      )
    }
  
  }

  def fetchMessages(onSuccess: () => Unit): Unit = {
    val accessToken = dom.window.localStorage.getItem("accessToken")
    val uid = dom.window.localStorage.getItem("userId")

    if (accessToken == null || uid == null) {
      dom.window.alert("You are not logged in.")
      return Future.failed(new Exception("User not logged in"))
    }

    val requestHeaders = new dom.Headers()
    requestHeaders.append("Content-Type", "application/json")
    requestHeaders.append("Authorization", s"Bearer $accessToken")

    val requestInit = new dom.RequestInit {
      method = dom.HttpMethod.GET
      headers = requestHeaders
    }

    dom.fetch(s"/api/messages/fetch/$uid", requestInit).toFuture.flatMap { response =>
      if (response.ok) {
        response.json().toFuture.map { json =>
          messages = json.asInstanceOf[js.Array[js.Dynamic]]
          onSuccess()
        }
      } else {
        response.text().toFuture.flatMap { text =>
          dom.window.alert(s"Failed to fetch messages: $text")
          Future.failed(new Exception(s"Failed to fetch messages: $text"))
        }
      }
    }.recoverWith {
      case ex: Throwable =>
        dom.window.alert(s"An error occurred: ${ex.getMessage}")
        Future.failed(ex)
    }
  }

  def buildMessagesPage(): Unit = {
    val contentDiv = dom.document.createElement("div").asInstanceOf[html.Div]
    contentDiv.className = "messages-container"

    messages.foreach { message =>
      val messageDiv = dom.document.createElement("div").asInstanceOf[html.Div]
      messageDiv.className = "message-item"

      val senderId = message.sender_id.asInstanceOf[String]
      val receiverId = message.receiver_id.asInstanceOf[String]
      val msgText = message.message.asInstanceOf[String]
      val sentAt = message.sent_at.asInstanceOf[String]

      messageDiv.innerHTML =
        s"""
           |<p><strong>From:</strong> $senderId</p>
           |<p><strong>To:</strong> $receiverId</p>
           |<p>${msgText}</p>
           |<p><small><em>Sent at: $sentAt</em></small></p>
           |<hr/>
           |""".stripMargin

      contentDiv.appendChild(messageDiv)
    }

    dom.document.body.appendChild(contentDiv)
  }
  
}
