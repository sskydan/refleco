name := "library"

organization := "refleco"

version := "0.5.0"

scalaVersion := "2.11.5"

scalacOptions ++= Seq(
    "-feature", 
    "-language:implicitConversions", 
    "-deprecation", 
    "-Xcheckinit"
)

resolvers ++= Seq(
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    "Concurrent Maven Repo" at "http://conjars.org/repo",
	  "clojars.org" at "http://clojars.org/repo",
	  "sonatype-oss" at "http://oss.sonatype.org/content/repositories/snapshots"
)

// jetty recent version = 9.1.2.v20140210
// WARNING- elasticsearch-hadoop-2.x requires scala2.10 atm
//   lib folder contains elasticsearch-hadoop-2.1.0-BUILD-SNAPSHOT compiled for 2.11
libraryDependencies ++= Seq(
    "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
    "commons-net" % "commons-net" % "2.0",
    "org.json" % "json" % "20090211",
    "net.lingala.zip4j" % "zip4j" % "1.3.2",
    "com.typesafe.akka" %% "akka-actor" % "2.3.4",
    "com.typesafe.akka" %% "akka-contrib" % "2.3.4",
    "com.typesafe.akka" %% "akka-testkit" % "2.3.4",
    "com.typesafe.akka" %% "akka-slf4j" % "2.3.4",
    "io.spray" %% "spray-servlet" % "1.3.2",
    "io.spray" %% "spray-routing-shapeless2" % "1.3.2",
    "io.spray" %% "spray-json" % "1.3.0",
    "org.eclipse.jetty" % "jetty-webapp" % "8.1.14.v20131031" % "container",
    "org.eclipse.jetty" % "jetty-plus"   % "8.1.14.v20131031" % "container",
    "org.scalatest" %% "scalatest" % "2.2.0" % "test",
    "com.github.nscala-time" %% "nscala-time" % "1.2.0",
    "org.elasticsearch" % "elasticsearch" % "1.4.0",
    //"org.apache.lucene" % "lucene-core" % "5.0.0",
    "org.jsoup" % "jsoup" % "1.8.1",
    "javax.ws.rs" % "jsr311-api" % "1.1.1",
    "com.sun.jersey" % "jersey-core" % "1.18.3"
//    "org.elasticsearch" % "elasticsearch-hadoop" % "2.1.0.Beta3" excludeAll(
//      ExclusionRule(organi.zation = "com.google"),
//      ExclusionRule(organization = "org.apache.spark")
//    )
).map(_.excludeAll(
    ExclusionRule(organization = "org.slf4j"),
    ExclusionRule(organization = "log4j")
))

libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % "1.2.0",
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.0.0"
)

seq(webSettings :_*)

port in container.Configuration := 7800

