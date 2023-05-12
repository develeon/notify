version := "1.0"

val versions = new {
  val specs2            = "4.19.2"
  val catsVersion       = "2.9.0"
  val catsEffectVersion = "3.4.6"
  val fs2               = "3.5.0"
  val scalaLogging      = "3.9.5"
}

val testDependencies = List(
  "org.specs2" %% "specs2-core" % versions.specs2 % "it,test"
)

val cats = List(
  "org.typelevel" %% "cats-core"   % versions.catsVersion,
  "org.typelevel" %% "cats-effect" % versions.catsEffectVersion,
)

val logging = List(
  "com.typesafe.scala-logging" %% "scala-logging"   % versions.scalaLogging,
  "ch.qos.logback"              % "logback-classic" % "1.4.5"
)

val fs2 = List(
  "co.fs2" %% "fs2-core" % versions.fs2,
)

lazy val root = (project in file("."))
  .settings(
    name := "notify",
    organization := "ch.epfl.scala",
    scalaVersion := "2.13.8",
    coverageEnabled := false,
  )
  .settings(
    libraryDependencies ++= Seq(
      "com.twilio.sdk" % "twilio"        % "7.55.3",
      "com.sendgrid"   % "sendgrid-java" % "4.6.6",
      "com.typesafe"   % "config"        % "1.4.2"
    ) ++ testDependencies ++ logging ++ cats
  )

coverageEnabled := false
