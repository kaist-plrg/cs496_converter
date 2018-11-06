import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.cs496",
      scalaVersion := "2.12.5",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "tsv2html",
    scalacOptions ++= Seq("-encoding", "utf-8", "-Xno-uescape"),
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "commons-io" % "commons-io" % "2.6"
  )
