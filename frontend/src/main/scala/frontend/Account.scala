package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._

object Account {
  // Fake user model – replace with real data source when available.
  case class User(name: String, dob: String, address: String)
  private val currentUser = User("Jane Doe", "1996‑02‑14", "123 Maple St, Springfield")

  def render(): Unit = {
    clearPage()
    document.body.appendChild(buildHeader())
    document.body.appendChild(buildProfileCard(currentUser))
  }

 
  private def buildHeader(): Div = {
    val header = styledDiv(
      "backgroundColor" -> "purple",
      "color"            -> "white",
      "padding"          -> "10px",
      "display"          -> "flex",
      "justify-content"  -> "space-between",
      "align-items"      -> "center",
      "position"         -> "fixed",
      "top"              -> "0",
      "left"             -> "0",
      "right"            -> "0",
      "height"           -> "50px",
      "zIndex"           -> "1"
    )

    val homeBtn = navButton("Home") { Main.render() }

    val title = document.createElement("div").asInstanceOf[Div]
    title.textContent       = "Account"
    title.style.fontSize    = "20px"
    title.style.fontWeight  = "bold"
    title.style.margin      = "0 auto"
    title.style.position    = "absolute"
    title.style.left        = "50%"
    title.style.transform   = "translateX(-50%)"

    header.appendChild(homeBtn)
    header.appendChild(title)
    header
  }

  private def navButton(label: String)(onClick: => Unit): Button = {
    val btn = document.createElement("button").asInstanceOf[Button]
    btn.textContent = label
    styleButton(btn, background = "transparent", color = "white", border = "none")
    btn.style.fontSize = "16px"
    btn.onclick = _ => onClick
    btn
  }

  private def buildProfileCard(user: User): Div = {
    val card = styledDiv(
      "marginTop"       -> "80px",
      "marginLeft"      -> "auto",
      "marginRight"     -> "auto",
      "width"           -> "80%",
      "maxWidth"        -> "500px",
      "border"          -> "1px solid #ccc",
      "borderRadius"    -> "8px",
      "padding"         -> "24px",
      "boxShadow"       -> "0 2px 8px rgba(0,0,0,0.1)",
      "backgroundColor" -> "#ffffff",
      "boxSizing"       -> "border-box"
    )

    // ── Top row: avatar + title ─────────────────────────────────────────────
    val topRow = styledDiv("display" -> "flex", "align-items" -> "center", "marginBottom" -> "32px")

    val avatar = styledDiv(
      "width"            -> "64px",
      "height"           -> "64px",
      "borderRadius"     -> "50%",
      "backgroundColor"  -> "#e0e0e0",
      "display"          -> "flex",
      "justify-content"  -> "center",
      "align-items"      -> "center",
      "fontSize"         -> "32px",
      "color"            -> "#666"
    )
    avatar.textContent = user.name.headOption.map(_.toString).getOrElse("?")

    val heading = document.createElement("h2").asInstanceOf[Heading]
    heading.textContent = "Me"
    heading.style.marginLeft = "16px"

    topRow.appendChild(avatar)
    topRow.appendChild(heading)

    // ── Details ─────────────────────────────────────────────────────────────
    def labelValue(label: String, value: String): Div = {
      val row = styledDiv("marginBottom" -> "20px")
      row.innerHTML = s"<strong>$label</strong> $value"
      row
    }

    val nameRow    = labelValue("Name:",    user.name)
    val dobRow     = labelValue("DOB:",     user.dob)
    val addressRow = labelValue("Address:", user.address)

    // ── Edit button ─────────────────────────────────────────────────────────
    val editBtn = document.createElement("button").asInstanceOf[Button]
    editBtn.textContent = "Edit Profile"
    styleButton(editBtn, background = "transparent", color = "#555", border = "1px solid #555")
    editBtn.style.display = "block"
    editBtn.style.margin  = "20px auto 0"
    editBtn.onclick = _ => dom.window.alert("Edit form coming soon …")

    // assemble card
    card.appendChild(topRow)
    card.appendChild(nameRow)
    card.appendChild(dobRow)
    card.appendChild(addressRow)
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
