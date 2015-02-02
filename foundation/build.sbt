name := "foundation"

organization := "reflecho"

version := "0.6.0"

scalaVersion := "2.11.5"

scalacOptions ++= Seq(
      "-feature", 
      "-language:implicitConversions", 
      "-deprecation", 
      "-Xcheckinit")

resolvers ++= Seq(
      "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
      "spray repo" at "http://repo.spray.io/"
)

libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
      "commons-net" % "commons-net" % "2.0",
      "org.json" % "json" % "20090211",
      "net.lingala.zip4j" % "zip4j" % "1.3.2",
      "com.typesafe.akka" %% "akka-actor" % "2.3.4",
      "com.typesafe.akka" %% "akka-contrib" % "2.3.4",
      "com.typesafe.akka" %% "akka-testkit" % "2.3.4",
      "io.spray" %% "spray-json" % "1.3.0",
      "org.scala-lang" % "scala-reflect" % "2.11.5",
      "org.scalatest" %% "scalatest" % "2.2.0" % "test",
      "com.github.nscala-time" %% "nscala-time" % "1.2.0"
).map(_.excludeAll(
    ExclusionRule(organization = "org.slf4j"),
    ExclusionRule(organization = "log4j")
))
