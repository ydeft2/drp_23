// package frontend
// import org.scalajs.dom
// import org.scalajs.dom.document
// import org.scalajs.dom.html._

// object RoleSelection {

//   def render(): Unit = {
//     clearPage()

//     document.body.appendChild(createSubpageHeader("Select Your Role"))

//     val container = document.createElement("div").asInstanceOf[Div]
//     container.style.setProperty("display", "flex")
//     container.style.setProperty("flex-direction", "column")
//     container.style.setProperty("align-items", "center")
//     container.style.setProperty("justify-content", "center")
//     container.style.setProperty("margin-top", "100px")

//     val title = document.createElement("h2")
//     title.textContent = "Are you a dentist or a patient?"
//     title.asInstanceOf[dom.html.Element].style.setProperty("margin-bottom", "30px")
//     container.appendChild(title)

//     // Dentist button
//     val dentistButton = document.createElement("button").asInstanceOf[Button]
//     dentistButton.textContent = "Dentist"
//     dentistButton.className = "role-button"
//     dentistButton.style.setProperty("margin", "10px")
//     dentistButton.addEventListener("click", (_: dom.MouseEvent) => {
//       dom.window.localStorage.setItem("role", "dentist")
//       dom.window.location.href = "/dentistDashboard"  // or trigger DentistPage.render()
//     })
//     container.appendChild(dentistButton)

//     // Patient button
//     val patientButton = document.createElement("button").asInstanceOf[Button]
//     patientButton.textContent = "Patient"
//     patientButton.className = "role-button"
//     patientButton.style.setProperty("margin", "10px")
//     patientButton.addEventListener("click", (_: dom.MouseEvent) => {
//       dom.window.localStorage.setItem("role", "patient")
//       dom.window.location.href = "/patientDashboard"  // or trigger PatientPage.render()
//     })
//     container.appendChild(patientButton)

//     document.body.appendChild(container)
//   }
// }
