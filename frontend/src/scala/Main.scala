import org.scalajs.dom.document

object Main {
  def main(args: Array[String]): Unit = {
    val el = document.createElement("h1")
    el.textContent = "Welcome to Dentana"
    document.body.appendChild(el)
  }
}
