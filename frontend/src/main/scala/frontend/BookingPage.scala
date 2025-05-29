package frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.{Div, Button, Element, Span}
import java.time.{LocalDate, YearMonth, DayOfWeek}
import scala.scalajs.js.timers._

object BookingPage {

  def render(): Unit = {
    // Clear the page
    document.body.innerHTML = ""

    // Create header
    val header = document.createElement("div").asInstanceOf[Div]
    header.style.backgroundColor = "purple"
    header.style.color = "white"
    header.style.padding = "10px"
    header.style.display = "flex"
    header.style.setProperty("justify-content", "space-between")
    header.style.setProperty("align-items", "center")
    header.style.position = "fixed"
    header.style.top = "0"
    header.style.left = "0"
    header.style.right = "0"
    header.style.height = "50px"
    header.style.zIndex = "1"

    // Home button
    val homeBtn = document.createElement("button").asInstanceOf[Button]
    homeBtn.textContent = "Home"
    homeBtn.style.background = "transparent"
    homeBtn.style.color = "white"
    homeBtn.style.border = "none"
    homeBtn.style.cursor = "pointer"
    homeBtn.style.fontSize = "16px"
    homeBtn.onclick = (_: dom.MouseEvent) => Main.render()

    val titleHeader = document.createElement("div")
    titleHeader.textContent = "Booking Calendar"
    titleHeader.asInstanceOf[Div].style.fontSize = "20px"
    titleHeader.asInstanceOf[Div].style.fontWeight = "bold"
    titleHeader.asInstanceOf[Div].style.margin = "0 auto"
    titleHeader.asInstanceOf[Div].style.position = "absolute"
    titleHeader.asInstanceOf[Div].style.left = "50%"
    titleHeader.asInstanceOf[Div].style.transform = "translateX(-50%)"

    header.appendChild(homeBtn)
    header.appendChild(titleHeader)
    document.body.appendChild(header)

    // Main container below header
    val container = document.createElement("div").asInstanceOf[Div]
    container.style.marginTop = "70px"
    container.style.marginLeft = "auto"
    container.style.marginRight = "auto"
    container.style.width = "80%"
    document.body.appendChild(container)

    // Setup month navigation state
    val today = LocalDate.now(java.time.Clock.systemUTC())
    val currentYearMonth = YearMonth.of(today.getYear, today.getMonthValue)
    var displayYearMonth: YearMonth = currentYearMonth
    val maxFuture: YearMonth = currentYearMonth.plusMonths(12)

    // Create month navigation container (will hold the navigation buttons)
    val monthNavContainer = document.createElement("div").asInstanceOf[Div]
    monthNavContainer.style.setProperty("justify-content", "center")
    monthNavContainer.style.setProperty("align-items", "center")
    monthNavContainer.style.marginBottom = "20px"
    container.appendChild(monthNavContainer)

    // Create calendar container (will hold the calendar grid)
    val calendarContainer = document.createElement("div").asInstanceOf[Div]
    calendarContainer.style.display = "grid"
    calendarContainer.style.setProperty("grid-template-columns", "repeat(7, 1fr)")
    calendarContainer.style.setProperty("gap", "5px")
    container.appendChild(calendarContainer)

    // Define calendar render logic before it's used in any event handler
    def renderCalendar(): Unit = {
      calendarContainer.innerHTML = ""
      // Add weekday header row
      val weekdays = List("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
      weekdays.foreach { day =>
        val dayHeader = document.createElement("div").asInstanceOf[Div]
        dayHeader.textContent = day
        dayHeader.style.fontWeight = "bold"
        dayHeader.style.textAlign = "center"
        calendarContainer.appendChild(dayHeader)
      }

      // Calculate details for the display month
      val firstDayOfMonth = displayYearMonth.atDay(1)
      val lastDay = displayYearMonth.lengthOfMonth()
      val firstDayOfWeek = firstDayOfMonth.getDayOfWeek.getValue % 7

      // Fill in leading blank cells
      for (_ <- 0 until firstDayOfWeek) {
        val emptyCell = document.createElement("div").asInstanceOf[Div]
        emptyCell.textContent = ""
        calendarContainer.appendChild(emptyCell)
      }

      // For each day of the month, create a cell in the calendar
      for(day <- 1 to lastDay) {
        val cell = document.createElement("div").asInstanceOf[Div]
        cell.textContent = day.toString
        cell.style.border = "1px solid #ccc"
        cell.style.padding = "8px"
        cell.style.textAlign = "center"
        cell.style.cursor = "pointer"
        // Highlight today's date only if display month is the current month
        if (displayYearMonth.equals(currentYearMonth) && day == today.getDayOfMonth) {
          cell.style.backgroundColor = "#d1e7dd"
        }
        // On click, show available times for the selected date
        cell.addEventListener("click", { (_: dom.MouseEvent) =>
          // Remove any previous times container before creating a new one
          val existingTimes = container.querySelector("#timesContainer")
          if (existingTimes != null) container.removeChild(existingTimes)
          val timesContainer = document.createElement("div").asInstanceOf[Div]
          timesContainer.id = "timesContainer"
          timesContainer.style.marginTop = "20px"
          container.appendChild(timesContainer)
          val selectedDate = displayYearMonth.atDay(day)
          showAvailableTimes(selectedDate, timesContainer)
        })
        calendarContainer.appendChild(cell)
      }
    }

    // Declare all buttons and label first:
    lazy val prevBtn: Button = {
      val btn = document.createElement("button").asInstanceOf[Button]
      btn.textContent = "<"
      btn.style.marginRight = "10px"
      btn.onclick = (_: dom.MouseEvent) => {
        if (displayYearMonth.isAfter(currentYearMonth)) {
          displayYearMonth = displayYearMonth.minusMonths(1)
          renderCalendar()
          updateNav()
        }
      }
      btn
    }

    lazy val monthLabel: Span = {
      val span = document.createElement("span").asInstanceOf[Span]
      span.style.setProperty("margin", "0 10px")
      span.style.right = "10px"
      span
    }

    lazy val nextBtn: Button = {
      val btn = document.createElement("button").asInstanceOf[Button]
      btn.textContent = ">"
      btn.style.marginLeft = "10px"
      btn.onclick = (_: dom.MouseEvent) => {
        if (displayYearMonth.isBefore(maxFuture)) {
          displayYearMonth = displayYearMonth.plusMonths(1)
          renderCalendar()
          updateNav()
        }
      }
      btn
    }

    // Now define updateNav AFTER those are declared
    def updateNav(): Unit = {
      monthLabel.textContent = s"${displayYearMonth.getMonth} ${displayYearMonth.getYear}"
      prevBtn.disabled = !displayYearMonth.isAfter(currentYearMonth)
      nextBtn.disabled = !displayYearMonth.isBefore(maxFuture)
    }

    // Append buttons to container after they are declared
    monthNavContainer.appendChild(prevBtn)
    monthNavContainer.appendChild(nextBtn)
    monthNavContainer.appendChild(monthLabel)

    // Then you can safely call updateNav and renderCalendar
    updateNav()
    renderCalendar()

  }

  private def showAvailableTimes(date: java.time.LocalDate, container: Div): Unit = {
    // Clear existing times
    container.innerHTML = ""
    // Title for selected day
    val title = document.createElement("h3")

    // Calculate tomorrow based on the current UTC date
    val tomorrow = LocalDate.now(java.time.Clock.systemUTC()).plusDays(1)

    // If the date is in the past or today, or if it's a weekend, show no available slots
    if (date.isBefore(tomorrow) || date.getDayOfWeek == java.time.DayOfWeek.SATURDAY || date.getDayOfWeek == java.time.DayOfWeek.SUNDAY) {
      title.textContent = s"No available slots for ${date.toString}"
      container.appendChild(title)
      return
    }

    title.textContent = s"Available slots for ${date.toString}"
    container.appendChild(title)

    // List available time slots from 9:00 AM to 4:00 PM
    val timesList = List(
      "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
      "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM"
    )

    timesList.foreach { time =>
      val timeSlot = document.createElement("div").asInstanceOf[Div]
      timeSlot.textContent = time
      timeSlot.style.border = "1px solid #ccc"
      timeSlot.style.padding = "6px"
      timeSlot.style.marginBottom = "4px"
      timeSlot.style.cursor = "pointer"
      timeSlot.addEventListener("click", { (_: dom.MouseEvent) =>
        dom.window.alert(s"Booking confirmed for ${date.toString} at $time")
      })
      container.appendChild(timeSlot)
    }
  }
}