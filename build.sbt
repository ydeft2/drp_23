import org.scalajs.linker.interface.ModuleInitializer
import sbtassembly.AssemblyPlugin.autoImport._

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
      "org.http4s" %% "http4s-dsl"           % "0.23.23",
      "org.http4s" %% "http4s-ember-server"  % "0.23.23",
      "org.http4s" %% "http4s-ember-client"  % "0.23.23",
      "org.http4s" %% "http4s-server"        % "0.23.23",
      "org.http4s" %% "http4s-circe"         % "0.23.23",
      "org.typelevel" %% "cats-effect"       % "3.5.1",
      "org.typelevel" %% "log4cats-slf4j"      % "2.5.0",
      "org.slf4j" % "slf4j-simple" % "2.0.12",
      "io.circe" %% "circe-generic"          % "0.14.5"
    ),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "versions", "9", "module-info.class") =>
        MergeStrategy.first // or MergeStrategy.discard
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    }
  )
