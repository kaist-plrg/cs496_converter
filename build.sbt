organization := "io.madcamp"
scalaVersion := "3.4.1"
version := "1.0.0"
name := "utils"

scalacOptions += "-deprecation"
scalacOptions += "-Xlint:unused"
scalacOptions += "-source:3.0-migration"

val unfilteredVersion = "0.13.0-M2"
libraryDependencies += "ws.unfiltered" %% "unfiltered-directives" % unfilteredVersion
libraryDependencies += "ws.unfiltered" %% "unfiltered-filter" % unfilteredVersion
libraryDependencies += "ws.unfiltered" %% "unfiltered-jetty" % unfilteredVersion
libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.10.1"
libraryDependencies += "commons-io" % "commons-io" % "2.6"
libraryDependencies += "org.apache.poi" % "poi-ooxml" % "4.1.2"
libraryDependencies += "com.google.api-client" % "google-api-client" % "1.30.9"
libraryDependencies += "com.google.oauth-client" % "google-oauth-client-jetty" % "1.35.0"
libraryDependencies += "com.google.apis" % "google-api-services-drive" % "v3-rev193-1.25.0"
