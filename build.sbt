name := "netlogo-prims-json"
organization := "org.nlogo"
licenses += ("Creative Commons Zero v1.0 Universal Public Domain Dedication", url("https://creativecommons.org/publicdomain/zero/1.0/"))
version := "1.0.0"
isSnapshot := true
publishTo := { Some("Cloudsmith API" at "https://maven.cloudsmith.io/netlogo/netlogo-prims-json/") }

scalaVersion := "3.7.0"
scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-release", "17")

resolvers += "netlogo" at "https://dl.cloudsmith.io/public/netlogo/netlogo/maven/"

libraryDependencies ++= Seq(
  "org.nlogo" % "netlogo" % "7.0.3",
  "org.jogamp.jogl" % "jogl-all" % "2.4.0" from "https://jogamp.org/deployment/archive/rc/v2.4.0/jar/jogl-all.jar",
  "org.jogamp.gluegen" % "gluegen-rt" % "2.4.0" from "https://jogamp.org/deployment/archive/rc/v2.4.0/jar/gluegen-rt.jar",
  "com.lihaoyi" %% "ujson" % "4.4.1"
)
