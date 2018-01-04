organization := "privateblue"

name := "eleven"

scalaVersion := "2.12.4"

scalacOptions += "-Ypartial-unification"

lazy val root = project.in(file(".")).aggregate(appJS, appJVM)

lazy val app = crossProject.in(file("."))
  .settings(
    libraryDependencies += "org.typelevel" %%% "cats-core" % "1.0.0-RC1"
  ).jsSettings(
    webpackBundlingMode := BundlingMode.LibraryOnly(),
    emitSourceMaps := false
  ).jsConfigure(
    _ enablePlugins ScalaJSBundlerPlugin
  ).jvmSettings(
    mainClass in assembly := Some("games.OriginalConsole"),
    assemblyJarName in assembly := s"${name.value}.jar"
  )

lazy val appJS = app.js

lazy val appJVM = app.jvm
