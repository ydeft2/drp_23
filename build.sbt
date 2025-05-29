import org.scalajs.linker.interface.ModuleInitializer

ThisBuild / scalaVersion := "3.3.1"

enablePlugins(ScalaJSPlugin)
lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "frontend",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.3.0",
      "io.github.cquiroz" %%% "scala-java-time" % "2.5.0"
    )
  )

lazy val backend = (project in file("backend"))
  .settings(
    scalaVersion := "3.3.1",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl"          % "0.23.23",
      "org.http4s" %% "http4s-ember-server" % "0.23.23",
      "org.http4s" %% "http4s-server" % "0.23.23",
      "org.typelevel" %% "cats-effect"      % "3.5.1",
    )
  )