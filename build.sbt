ThisBuild / organization := "io.madcamp"
ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version      := "1.0.0"
ThisBuild / name         := "apptopdf"

ThisBuild / scalacOptions += "-deprecation"
ThisBuild / scalacOptions += "-Xno-uescape"  // prevent backslash u escaping in strings
ThisBuild / scalacOptions += "-Xlint:unused"

ThisBuild / libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0"
ThisBuild / libraryDependencies += "commons-io" % "commons-io" % "2.6"
ThisBuild / libraryDependencies += "org.apache.poi" % "poi-ooxml" % "4.1.2"
ThisBuild / libraryDependencies += "com.google.api-client" % "google-api-client" % "1.30.9"
ThisBuild / libraryDependencies += "com.google.oauth-client" % "google-oauth-client-jetty" % "1.30.5"
ThisBuild / libraryDependencies += "com.google.apis" % "google-api-services-drive" % "v3-rev193-1.25.0"
ThisBuild / libraryDependencies += "com.typesafe.play" %% "play-json" % "2.8.1"

Global / onChangedBuildSource := ReloadOnSourceChanges
