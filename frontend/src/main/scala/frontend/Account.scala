package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._

object Account {
  case class User(name: String, dob: String, address: String)
  private val currentUser = User("Jane Doe", "1996‑02‑14", "123 Maple St, Springfield")

  def render(): Unit = {
    clearPage()
    document.body.appendChild(createSubpageHeader("Dentana Account"))
    document.body.appendChild(buildProfileCard(currentUser))
  }



  private def buildProfileCard(user: User): Div = {
    val card = document.createElement("div").asInstanceOf[Div]
    card.style.marginTop = "70px"
    card.style.marginLeft = "auto"
    card.style.marginRight = "auto"
    card.style.width = "60%"
    card.style.maxHeight = "400px"
    card.style.border = "1px solid #ccc"
    card.style.borderRadius = "8px"
    card.style.padding = "20px"
    card.style.boxShadow = "0 2px 8px rgba(0, 0, 0, 0.1)"
    card.style.backgroundColor = "#f9f9f9"




    val heading = document.createElement("h2").asInstanceOf[Heading]
    heading.textContent    = "Account Details"
    heading.style.marginBottom = "24px"
    card.appendChild(heading)


    def infoRow(label: String, value: String): Div = {
      val row = styledDiv("marginBottom" -> "20px")
      row.innerHTML = s"<strong>$label</strong> $value"
      row
    }

    card.appendChild(infoRow("Name:", user.name))
    card.appendChild(infoRow("Date of Birth:", user.dob))
    card.appendChild(infoRow("Address:", user.address))

 
    val editBtn = document.createElement("button").asInstanceOf[Button]
    editBtn.textContent = "Edit Profile"
    styleButton(editBtn, background = "transparent", color = "purple", border = "2px solid purple")
    editBtn.style.display = "block"
    editBtn.style.margin  = "20px auto 0"
    editBtn.onclick = _ => dom.window.alert("Please contact your dental practice in order to edit your profile.")
    card.appendChild(editBtn)

    card
  }

  private def clearPage(): Unit = document.body.innerHTML = ""

  private def styledDiv(styles: (String, String)*): Div = {
    val d = document.createElement("div").asInstanceOf[Div]
    styles.foreach { case (k, v) => d.style.setProperty(k, v) }
    d
  }

  private def styleButton(b: Button, background: String, color: String, border: String): Unit = {
    b.style.background   = background
    b.style.color        = color
    b.style.border       = border
    b.style.padding      = "10px 24px"
    b.style.cursor       = "pointer"
    b.style.borderRadius = "4px"
    b.style.fontSize     = "16px"
  }
}
