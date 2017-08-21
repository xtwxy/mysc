import sbtassembly.MergeStrategy

name := "rest"
organization := "com.wincom.dcim"
version := "1.0.0"
scalaVersion := "2.12.3"

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

enablePlugins(JavaAppPackaging)
enablePlugins(JavaServerAppPackaging)

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Sonatype snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots/")

parallelExecution in Test := false

fork := true

libraryDependencies ++= {
  val akkaVersion = "2.5.1"
  val akkaHttpVersion = "10.0.9"
  val sprayVersion = "1.3.3"
  val jodaTimeVersion = "2.9.9"
  Seq(
    "com.typesafe.akka"         %%  "akka-http"                           % akkaHttpVersion,
    "com.typesafe.akka"         %%  "akka-http-core"                      % akkaHttpVersion,
    "com.typesafe.akka"         %%  "akka-http-spray-json"                % akkaHttpVersion,
    //"io.spray"                  %%  "spray-json"                          % sprayVersion,

    "joda-time"                 %   "joda-time"                           % jodaTimeVersion
  )
}
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.rename
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

mainClass in assembly := Some("com.wincom.dcim.sharded.Main")
assemblyJarName in assembly := "domain.jar"


