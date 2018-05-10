import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.cs496",
      scalaVersion := "2.12.5",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "tsv2html",
    libraryDependencies += scalaTest % Test
  )
