package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.{Div, Button, Span, Table, TableCell, TableRow}

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.util.Random

// replace with backend
final case class Slot(
  slotId: UUID,
  bookingId: UUID,
  clinicId: UUID,
  isTaken: Boolean,
  slotTime: Instant,
  slotLength: Long,
  clinicInfo: String,
  createdAt: Instant
)

object BookingPage {


  private val timeStrings = Seq("09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00")
  private val fmt         = DateTimeFormatter.ofPattern("HH:mm")
  private val zoneId      = ZoneOffset.UTC


  private def randUuid(): UUID = {
    val hex = List.fill(32)(Random.nextInt(16).toHexString).mkString
    val dashed =
      s"${hex.substring(0, 8)}-" +
      s"${hex.substring(8,12)}-" +
      s"${hex.substring(12,16)}-" +
      s"${hex.substring(16,20)}-" +
      hex.substring(20)
    UUID.fromString(dashed)
  }

// remove when backend complete
  private def dummySlots(weekStart: LocalDate): Seq[Slot] = {
    for {
      d       <- 0 until 7
      day      = weekStart.plusDays(d) if !day.getDayOfWeek.isWeekend
      t       <- timeStrings
      localDT  = LocalDateTime.of(day, LocalTime.parse(t, fmt))
      instant  = localDT.atZone(zoneId).toInstant
    } yield Slot(
      slotId     = randUuid(),
      bookingId  = randUuid(),
      clinicId   = randUuid(),
      isTaken    = false,
      slotTime   = instant,
      slotLength = 30,
      clinicInfo = "London Dental Care",
      createdAt  = Instant.now()
    )
  }

  def render(): Unit = {
    document.body.innerHTML = ""

    document.body.appendChild(createSubpageHeader("Available Bookings"))

    val main = document.createElement("div").asInstanceOf[Div]
    main.style.marginTop   = "80px"
    main.style.width       = "90%"
    main.style.marginLeft  = "auto"
    main.style.marginRight = "auto"
    main.style.maxWidth    = "1200px"
    main.style.padding     = "20px"
    main.style.backgroundColor = "#f9f9f9"
    main.style.borderRadius = "8px"
    main.style.boxShadow    = "0 2px 8px rgba(0, 0, 0, 0.1)"

  
    val today = LocalDate.now(java.time.Clock.systemUTC())
    val daysFromSunday  = today.getDayOfWeek.getValue % 7 // Sunday -> 0, Monday -> 1 …
    val startToday      = today.minusDays(daysFromSunday.toLong)
    var currentWeekStart = startToday
    val lastWeekStart    = startToday.plusWeeks(52)   // 12‑month horizon

    
    val navBar = document.createElement("div").asInstanceOf[Div]
    navBar.style.display = "flex"
    navBar.style.setProperty("justify-content", "center")
    navBar.style.setProperty("align-items", "center")
    navBar.style.marginBottom = "15px"
    navBar.style.padding = "10px"
    navBar.style.backgroundColor = "#e0e0e0"
    navBar.style.borderRadius = "5px"
    navBar.style.boxShadow = "0 1px 3px rgba(23, 21, 21, 0.1)"

    val prevBtn = navButton("< Prev Week")
    val nextBtn = navButton("Next Week >")
    val weekLab = document.createElement("span").asInstanceOf[Span]
    weekLab.style.margin = "0 12px"

    navBar.appendChild(prevBtn)
    navBar.appendChild(weekLab)
    navBar.appendChild(nextBtn)
    main.appendChild(navBar)
    

    val tableHolder = document.createElement("div").asInstanceOf[Div]
    main.appendChild(tableHolder)

    def updateLabel(): Unit = {
      weekLab.textContent = s"${currentWeekStart} – ${currentWeekStart.plusDays(6)}"
    }

    def updateButtons(): Unit = {
      prevBtn.disabled = currentWeekStart == startToday
      nextBtn.disabled = currentWeekStart == lastWeekStart
    }

    def drawWeek(): Unit = {
      tableHolder.innerHTML = ""
      tableHolder.appendChild(buildTable(currentWeekStart))
      updateLabel()
      updateButtons()
    }

    prevBtn.onclick = (_: dom.MouseEvent) => {
      if (currentWeekStart.isAfter(startToday)) {
        currentWeekStart = currentWeekStart.minusWeeks(1)
        drawWeek()
      }
    }

    nextBtn.onclick = (_: dom.MouseEvent) => {
      if (currentWeekStart.isBefore(lastWeekStart)) {
        currentWeekStart = currentWeekStart.plusWeeks(1)
        drawWeek()
      }
    }

    document.body.appendChild(main)
    drawWeek()
  }


  private def buildTable(weekStart: LocalDate): Table = {

    // add actual slots from backend here
  val slotsByDay: Map[LocalDate, Seq[Slot]] =
    dummySlots(weekStart).groupBy(s =>
      s.slotTime.atZone(zoneId).toLocalDate
    )


  val tbl = document.createElement("table").asInstanceOf[Table]
  tbl.style.borderCollapse = "collapse"
  tbl.style.width          = "100%"
  tbl.style.border         = "1px solid #ddd"

  def th(txt: String): TableCell = {
    val c = document.createElement("th").asInstanceOf[TableCell]
    c.textContent = txt
    c.style.border = "1px solid #999"
    c.style.padding = "6px"
    c
  }

  def td(txt: String, clickable: Boolean): TableCell = {
    val c = document.createElement("td").asInstanceOf[TableCell]
    c.textContent = txt
    c.style.border = "1px solid #ccc"
    c.style.padding = "6px"
    if (clickable) {
      c.style.cursor = "pointer"
      c.style.backgroundColor = "#e0ffe0"
    }
    c
  }

  val headerRow = document.createElement("tr").asInstanceOf[TableRow]
  headerRow.appendChild(th(""))
  for (i <- 0 until 7)
    headerRow.appendChild(th(weekStart.plusDays(i).getDayOfWeek.toString.take(3)))
  tbl.appendChild(headerRow)


  for (tStr <- timeStrings) {
    val row = document.createElement("tr").asInstanceOf[TableRow]
    row.appendChild(th(tStr))

    val targetTime = LocalTime.parse(tStr)

    for (i <- 0 until 7) {
      val day     = weekStart.plusDays(i)
      val slotOpt = slotsByDay.getOrElse(day, Nil).find(s =>
        s.slotTime.atZone(zoneId).toLocalTime == targetTime
      )

      slotOpt match {
        case Some(slt) =>
          val cell = td("Book", clickable = true)
          cell.addEventListener("click", (_: dom.MouseEvent) =>
            dom.window.alert(
              s"Booking confirmed for $day at $tStr\nClinic: ${slt.clinicInfo}"
            )
            
            // Add booking backend logic here
          )
          row.appendChild(cell)
        case None =>
          row.appendChild(td("-", clickable = false))
      }
    }

    tbl.appendChild(row)
  }

  tbl
}


  private def navButton(text: String): Button = {
    val b = document.createElement("button").asInstanceOf[Button]
    b.textContent = text
    b.style.padding = "6px 12px"
    b
  }


  extension (d: DayOfWeek) private def isWeekend: Boolean =
    d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY
}
