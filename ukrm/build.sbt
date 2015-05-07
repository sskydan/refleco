name := "ukrm"

organization := "refleco"

version := "0.1.1"

scalaVersion := "2.11.6"

scalacOptions ++= Seq(
    "-feature", 
    "-language:implicitConversions", 
    "-deprecation", 
    "-Xcheckinit",
    "-language:existentials",
    "-language:higherKinds")
//    "-Xprint:typer")

resolvers ++= Seq(
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

// shapeless conflict in spray vs. parboiled (?) - use spray-routing-shapeless2 ?
libraryDependencies ++= Seq(
    "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
    "com.typesafe.akka" %% "akka-actor" % "2.3.4",
    "com.typesafe.akka" %% "akka-contrib" % "2.3.4",
    "com.typesafe.akka" %% "akka-testkit" % "2.3.4",
    "com.typesafe.akka" %% "akka-slf4j" % "2.3.4",
    "io.spray" %% "spray-client" % "1.3.2",
    "io.spray" %% "spray-servlet" % "1.3.2",
    "io.spray" %% "spray-routing-shapeless2" % "1.3.2",
    "io.spray" %% "spray-json" % "1.3.0",
    "org.eclipse.jetty" % "jetty-webapp" % "8.1.14.v20131031" % "container",
    "org.eclipse.jetty" % "jetty-plus"   % "8.1.14.v20131031" % "container",
	  "org.scalatest" %% "scalatest" % "2.2.0" % "test",
	  "com.github.nscala-time" %% "nscala-time" % "1.2.0",
    "org.scalaz" %% "scalaz-core" % "7.1.0",
    "javax.ws.rs" % "jsr311-api" % "1.1.1",
    "com.sun.jersey" % "jersey-core" % "1.18.3"
).map(_.excludeAll(
    ExclusionRule(organization = "org.slf4j"),
    ExclusionRule(organization = "log4j")
))

libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % "1.3.0",
    "org.apache.spark" %% "spark-graphx" % "1.3.0",
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.0.0"
)

seq(webSettings :_*)

port in container.Configuration := 7802