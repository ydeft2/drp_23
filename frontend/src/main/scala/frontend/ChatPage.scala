package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.{Div, Button, TextArea, UList, LI, Span}
import scala.scalajs.js
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import java.util.UUID
import java.time.*
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

case class MessageResponse(
  messageId    : UUID,
  senderId     : UUID,
  receiverId   : UUID,
  message      : String,
  sentAt       : Instant,
  senderName   : String,   
  receiverName : String    
)

object ChatPage {

  private val safeZone: ZoneId =
    try ZoneId.systemDefault() catch case _: DateTimeException => ZoneOffset.UTC

  private var all       : List[MessageResponse]            = Nil
  private var grouped   : Map[UUID, List[MessageResponse]] = Map.empty
  private var peerName  : Map[UUID, String]                = Map.empty  
  private var currentPeer: Option[UUID]                    = None
  private var selfId    : UUID                             = _
  

  private var listPane: Div = _
  private var convPane: Div = _


  def render(): Unit = {
    Spinner.show()
    println("ChatPage.render() called")

    // todo get user name and peername lists from supabase 
    /*val token = dom.window.localStorage.getItem("accessToken")
    val uid   = dom.window.localStorage.getItem("userId")
    if token == null || uid == null then { dom.window.alert("Not logged in."); return }

    val hdr = new dom.Headers(); hdr.append("Authorization", s"Bearer $token")
    val init = new dom.RequestInit { method = dom.HttpMethod.GET; headers = hdr }

    dom.fetch(s"/api/messages/fetch/$uid", init) */

    val uidStr = dom.window.localStorage.getItem("userId")
    if uidStr == null then { dom.window.alert("You are not logged in."); return }
    selfId = UUID.fromString(uidStr)

    fetchMessages { () =>
      buildThreads()
      Layout.renderPage(
        leftButton = Some(createHomeButton()),
        contentRender = () => {
          val box = renderMessagesBox()
          document.body.appendChild(box)
          renderList()
          currentPeer.orElse(grouped.keys.headOption).foreach(showConversation)
          Spinner.hide()
        }
      )
    }
  }

  private def parseIsoToInstant(s: String): Instant =
    Instant.parse(s.takeWhile(_ != '['))

  private def parseMessages(arr: js.Array[js.Dynamic]): List[MessageResponse] =
    arr.toList.flatMap { o =>
      try Some(
        MessageResponse(
          UUID.fromString(o.message_id.asInstanceOf[String]),
          UUID.fromString(o.sender_id.asInstanceOf[String]),
          UUID.fromString(o.receiver_id.asInstanceOf[String]),
          o.message.asInstanceOf[String],
          parseIsoToInstant(o.sent_at.asInstanceOf[String]),
          o.sender_name  .asInstanceOf[String],   
          o.receiver_name.asInstanceOf[String]    
        )
      ) catch { case _ => None }
    }

  private def fetchMessages(onSuccess: () => Unit): Unit = {
    val token = dom.window.localStorage.getItem("accessToken")
    val uid   = dom.window.localStorage.getItem("userId")
    if token == null || uid == null then { dom.window.alert("Not logged in."); return }

    val hdr = new dom.Headers(); hdr.append("Authorization", s"Bearer $token")
    val init = new dom.RequestInit { method = dom.HttpMethod.GET; headers = hdr }

    dom.fetch(s"/api/messages/fetch/$uid", init)
      .toFuture.flatMap(_.json().toFuture).foreach {
        case arr: js.Array[js.Dynamic] => all = parseMessages(arr); onSuccess()
        case other                     => Spinner.hide(); dom.window.alert("Invalid response: " + other)
      }
  }

  private def sendMessage(peer: UUID, text: String, onOK: () => Unit): Unit = {
    println("ChatPage.sendMessage called")
    Option(dom.window.localStorage.getItem("accessToken")) match
    case None =>
      dom.window.alert("No token")

    case Some(token) =>
      
      val work =
        for {
          patient <- isPatient()                                 

          
          (selfNameF, receiverNameF) =
            if patient then
              (fetchUserDetails(selfId.toString).map(_.name),
               fetchClinicDetails(peer.toString).map(_.name))
            else
              (fetchClinicDetails(selfId.toString).map(_.name),
               fetchUserDetails(peer.toString).map(_.name))

          selfName     <- selfNameF                               
          receiverName <- receiverNameF                           

          _ <- {
            val hdr = new dom.Headers()
            hdr.append("Content-Type", "application/json")
            hdr.append("Authorization", s"Bearer $token")

            val payload = js.Dynamic.literal(
              "sender_id"     -> selfId.toString,
              "receiver_id"   -> peer.toString,
              "message"       -> text,
              "sender_name"   -> selfName,
              "receiver_name" -> receiverName
            )

            val init = new dom.RequestInit {
              method = dom.HttpMethod.POST
              headers = hdr
              this.body = js.JSON.stringify(payload)
            }
            println("ChatPage.sendMessage: sending message with payload: " + payload)

            dom.fetch("/api/messages/send", init).toFuture        
          }
        } yield ()

      
      work.foreach(_ => onOK())
      work.failed.foreach(e => dom.window.alert(s"An error occurred: ${e.getMessage}"))
  }

  /* =================== threads =================== */

  private def buildThreads(): Unit = {
    grouped = all.groupBy(m => if m.senderId == selfId then m.receiverId else m.senderId)
                .view.mapValues(_.sortBy(_.sentAt)).toMap

                println("ChatPage.buildThreads: grouped messages: " + grouped)

    
    peerName = grouped.map { case (peerId, msgs) =>
      val first = msgs.head
      val name  =
        if first.senderId == peerId then first.senderName
        else                         first.receiverName
      peerId -> name
    }
  }

  /* =================== list pane =================== */

  private def renderList(): Unit = {
    listPane.innerHTML = ""
    if grouped.isEmpty then
      listPane.appendChild(span("You have no messages yet.", center = true, grey = true))
      convPane.innerHTML = ""
      return

    val ul = document.createElement("ul").asInstanceOf[UList]
    ul.style.listStyle = "none"; ul.style.padding = "0"; ul.style.margin = "0"
    val fmt = DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(safeZone)

    grouped.toSeq.sortBy(_._2.last.sentAt).reverse.foreach { (peer, msgs) =>
      val li = document.createElement("li").asInstanceOf[LI]
      li.style.cursor  = "pointer"; li.style.padding = "10px"
      if currentPeer.contains(peer) then li.style.backgroundColor = "#eef4ff"
      li.onclick = (_: dom.MouseEvent) => { currentPeer = Some(peer); renderList(); showConversation(peer) }

      li.appendChild(span(peerName(peer), bold = true))
      li.appendChild(span(" " + msgs.last.message.take(5) + (if msgs.last.message.length > 5 then "…" else "")))
      li.appendChild(span(fmt.format(msgs.last.sentAt), right = true))
      ul.appendChild(li)
    }
    listPane.appendChild(ul)
  }

  private def showConversation(peer: UUID): Unit = {
    convPane.innerHTML = ""
    convPane.appendChild(span(peerName(peer), bold = true, center = true))

    val msgs = grouped.getOrElse(peer, Nil)
    val scrollBox = div(); scrollBox.style.setProperty("flex-grow", "1")
    scrollBox.style.overflowY = "auto"; scrollBox.style.paddingRight = "6px"
    convPane.appendChild(scrollBox)

    val dayFmt  = DateTimeFormatter.ofPattern("MMM d").withZone(safeZone)
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(safeZone)
    var lastDay = ""

    msgs.foreach { m =>
      val d = dayFmt.format(m.sentAt)
      if d != lastDay then { scrollBox.appendChild(span(d, center = true, grey = true)); lastDay = d }

      val bubble = div()
      bubble.appendChild(dom.document.createTextNode(m.message + "  "))
      val timeLabel = span(timeFmt.format(m.sentAt), grey = true)
      timeLabel.style.fontSize = "0.78em"
      bubble.appendChild(timeLabel)
      bubble.style.maxWidth = "70%"; bubble.style.margin = "4px"; bubble.style.padding = "6px 8px"
      bubble.style.borderRadius = "6px"
      if m.senderId == selfId then
        bubble.style.marginLeft = "auto"; bubble.style.backgroundColor = "#d0e6ff"
      else
        bubble.style.marginRight = "auto"; bubble.style.backgroundColor = "#e6e6e6"
      scrollBox.appendChild(bubble)
    }

    val bar = div(); bar.style.display = "flex"; bar.style.marginTop = "8px"
    val ta  = document.createElement("textarea").asInstanceOf[TextArea]; ta.rows = 2; ta.style.setProperty("flex-grow", "1")
    val send = button("Send")
    send.onclick = (_: dom.MouseEvent) => {
      val txt = ta.value.trim
      if txt.nonEmpty then sendMessage(peer, txt, () => {
        ta.value = ""; fetchMessages(() => { buildThreads(); renderList(); showConversation(peer) })
      })
    }
    bar.appendChild(ta); bar.appendChild(send); convPane.appendChild(bar)
    scrollBox.scrollTop = scrollBox.scrollHeight
  }

  private def renderMessagesBox(): Div = {
    val outer = div(); outer.style
      .setProperty("margin", "70px auto 0 auto")
    outer.style.width = "80%"; outer.style.height = "500px"; outer.style.display = "flex"
    outer.style.border = "1px solid #ccc"; outer.style.borderRadius = "8px"
    outer.style.boxShadow = "0 2px 8px rgba(0,0,0,0.1)"; outer.style.backgroundColor = "#f9f9f9"

    listPane = div("35%"); listPane.style.borderRight = "1px solid #ddd"
    convPane = div(); convPane.style.display = "flex"
    convPane.style.setProperty("flex-direction", "column")
    convPane.style.setProperty("flex-grow", "1")
    convPane.style.padding = "12px"

    outer.appendChild(listPane); outer.appendChild(convPane); outer
  }

  private inline def div(width: String = ""): Div =
    val d = document.createElement("div").asInstanceOf[Div]
    if width.nonEmpty then d.style.width = width; d

  private inline def span(txt: String, bold: Boolean = false,
                          right:Boolean = false, center:Boolean = false, grey:Boolean = false): Span = {
    val s = document.createElement("span").asInstanceOf[Span]
    s.textContent = txt
    if bold   then s.style.fontWeight = "bold"
    if right  then { s.style.setProperty("float", "right"); s.style.color = "#666" }
    if center then { s.style.textAlign = "center"; s.style.display = "block" }
    if grey   then { s.style.color = "#999";  s.style.display = "block"; s.style.margin = "8px 0" }
    s
  }

  private inline def button(lbl: String): Button =
    val b = document.createElement("button").asInstanceOf[Button]; b.textContent = lbl; b

  def createChat(clinic_id: String, clinic_name: String): Unit = {
    println(s"Creating chat with clinic $clinic_id ($clinic_name)")
    
  }
}



