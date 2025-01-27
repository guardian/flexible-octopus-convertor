name := "octopus-converter"

organization := "com.gu"

description := "AWS Lambda providing conversion between Octopus JSON and Thrift"

version := "1.0"

scalaVersion := "2.13.16"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-Ywarn-dead-code"
)
val awsSdkVersion = "1.11.804"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.3",
  "com.amazonaws" % "aws-lambda-java-log4j2" % "1.6.0",
  "com.amazonaws" % "aws-java-sdk-kinesis" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-sqs" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsSdkVersion,
  "com.amazonaws" % "aws-lambda-java-events" % "3.14.0",
  "org.slf4j" % "slf4j-simple" % "2.0.16",
  "org.scalactic" %% "scalactic" % "3.2.19",
  "org.scalatest" %% "scalatest" % "3.2.19" % "test",
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % "2.11.0",
  "com.gu" %% "flexible-octopus-model" % "0.5.0",
  "org.apache.thrift" % "libthrift" % "0.13.0",
  "com.twitter" %% "scrooge-core" % "20.5.0"
)

assemblyJarName := s"${name.value}.jar"


assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _                             => MergeStrategy.first
}
