name := "dbabspro"

version := "0.1.70"

organization := "io.github.lemonxah"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  val akkaVersion = "2.3.9"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "org.scalaz" %% "scalaz-core" % "7.0.6",
    "com.h2database" % "h2" % "1.4.187",
    "org.apache.kafka" %% "kafka" % "0.8.2.1",
    "org.scalikejdbc" %% "scalikejdbc" % "2.2.7",
    "org.mongodb" %% "casbah" % "2.8.1",
    "com.novus" %% "salat" % "1.9.9",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    "org.scala-lang" % "scala-reflect" % scalaVersion.value
  )
}

addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)