ThisBuild / scalaVersion     := "2.13.12"
ThisBuild / version          := "0.1.0-SNAPSHOT"

lazy val frontend = project
  .in(file("frontend"))
  .enablePlugins(org.scalajs.sbtplugin.ScalaJSPlugin)
  .settings(
    name := "dentana-frontend",
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.6.0",
    // tell sbt where to emit the JS so backend can pick it up:
    Compile / fastOptJS / artifactPath :=
      baseDirectory.value / ".." / "backend" / "src" / "main" / "resources" / "web" / "frontend-fastopt.js",
  )

lazy val backend = project
  .in(file("backend"))
  .settings(
    name := "dentana-backend",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"   % "10.2.9",
      "com.typesafe.akka" %% "akka-stream" % "2.6.20"
    ),
    Compile / unmanagedResourceDirectories +=
      baseDirectory.value / "src" / "main" / "resources" / "web"
  )

lazy val root = (project in file("."))
  .aggregate(frontend, backend)
  .settings(publish := {}, publishLocal := {}, publishM2 := {})
