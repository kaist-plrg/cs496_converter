ThisBuild / organization := "io.madcamp"
ThisBuild / scalaVersion := "2.13.2"
ThisBuild / version      := "1.0.0"
ThisBuild / name         := "utils"

ThisBuild / scalacOptions += "-deprecation"
ThisBuild / scalacOptions += "-Xlint:unused"

ThisBuild / libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0"

ThisBuild / libraryDependencies += "com.typesafe.play" %% "play-json" % "2.8.1"

ThisBuild / libraryDependencies += "commons-io" % "commons-io" % "2.6"
ThisBuild / libraryDependencies += "org.apache.poi" % "poi-ooxml" % "4.1.2"

ThisBuild / libraryDependencies += "com.google.api-client" % "google-api-client" % "1.30.9"
ThisBuild / libraryDependencies += "com.google.oauth-client" % "google-oauth-client-jetty" % "1.30.5"
ThisBuild / libraryDependencies += "com.google.apis" % "google-api-services-drive" % "v3-rev193-1.25.0"

val unfilteredVersion = "0.10.0-M6"
ThisBuild / libraryDependencies += "ws.unfiltered" %% "unfiltered-directives" % unfilteredVersion
ThisBuild / libraryDependencies += "ws.unfiltered" %% "unfiltered-filter" % unfilteredVersion
ThisBuild / libraryDependencies += "ws.unfiltered" %% "unfiltered-jetty" % unfilteredVersion

Global / onChangedBuildSource := ReloadOnSourceChanges
