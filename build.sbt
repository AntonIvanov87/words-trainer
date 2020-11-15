name := "words-trainer"

version := "0.1"

scalaVersion := "2.13.3"

mainClass in Compile := Some("wordstrainer.Main")

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.11.3",
)
