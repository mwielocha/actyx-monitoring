// Generated with scalagen

lazy val root = (project in file(".")).
  settings(
    name := "actyx-monitoring",
    version := "1.0",
    scalaVersion := "2.11.7"
  ).enablePlugins(play.sbt.PlayScala)

//mainClass in (Compile, run) := Some("...")

libraryDependencies ++= Seq(
    cache,
    "com.typesafe.play" %% "play-ws" % "2.5.3",
    "com.typesafe.play" %% "play-json" % "2.5.3",
    "io.getquill" %% "quill-cassandra" % "0.5.0",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  )

maintainer in Linux := "Mikolaj Wielocha <mwielocha@icloud.com>"

packageSummary in Linux := "Actyx Machine Park Monitoring App"

packageDescription := "Actyx Machine Park Monitoring App"
