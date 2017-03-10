name := """kotoed"""
organization := "org.jetbrains.research"

version := "0.1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

kotlinVersion := "1.1.0"
kotlinLib("stdlib")

kotlinSource := baseDirectory.value / "app"
kotlinSource := baseDirectory.value / "test"

kotlinClasspath(Compile, Def.setting(Seq(target.value / "scala-2.11" / "classes").classpath))

scalaVersion := "2.11.8"

libraryDependencies += filters
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "org.jetbrains.research.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "org.jetbrains.research.binders._"
