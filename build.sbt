// Generated with scalagen

lazy val root = (project in file(".")).
  settings(
    name := "actyx-monitoring",
    version := "1.0",
    scalaVersion := "2.11.8"
  ).enablePlugins(play.sbt.PlayScala)

//mainClass in (Compile, run) := Some("...")

val akkaVersion = "2.4.14"
val akkaHttpVersion = "10.0.0"

libraryDependencies ++= Seq(
    cache,
    "com.typesafe.play" %% "play-ws" % "2.5.10",
    "com.typesafe.play" %% "play-json" % "2.5.10",
    "io.getquill" %% "quill-cassandra" % "1.0.1",
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "de.heikoseeberger" %% "akka-http-play-json" % "1.10.1",
    "org.scalatest" %% "scalatest" % "3.0.0" % "test"
  )

maintainer in Linux := "Mikolaj Wielocha <mwielocha@icloud.com>"

packageSummary in Linux := "Actyx Machine Park Monitoring App"

packageDescription := "Actyx Machine Park Monitoring App"
