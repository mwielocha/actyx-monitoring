// Generated with scalagen

lazy val root = (project in file(".")).
  settings(
    name := "actyx-monitoring",
    version := "1.0",
    scalaVersion := "2.11.8"
  ).enablePlugins(SbtTwirl)

mainClass in (Compile, run) := Some("io.mwielocha.actyxapp.EntryPoint")

val akkaVersion = "2.4.14"
val akkaHttpVersion = "10.0.0"

libraryDependencies ++= Seq(
    "com.github.cb372" %% "scalacache-guava" % "0.9.3",
    "com.google.inject" % "guice" % "4.1.0",
    "net.codingwell" %% "scala-guice" % "4.1.0",
    "com.typesafe.play" %% "play-json" % "2.5.10",
    "io.getquill" %% "quill-cassandra" % "1.0.1",
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "de.heikoseeberger" %% "akka-http-play-json" % "1.10.1",
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "org.scalatest" %% "scalatest" % "3.0.0" % "test"
  )

