
// Generated with scalagen

lazy val root = (project in file(".")).
  settings(
    name := "actyx-monitoring",
    version := "1.0",
    scalaVersion := "2.11.7"
  ).enablePlugins(play.sbt.PlayScala)

//mainClass in (Compile, run) := Some("...")

libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  )

