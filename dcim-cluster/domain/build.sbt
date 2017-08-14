import sbtassembly.MergeStrategy

name := "domain"
organization := "com.wincom.dcim"
version := "1.0.0"
scalaVersion := "2.11.11"

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

enablePlugins(JavaAppPackaging)
enablePlugins(JavaServerAppPackaging)

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Sonatype snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots/")

parallelExecution in Test := false

fork := true

libraryDependencies ++= {
  val akkaVersion = "2.5.1"
  Seq(
    "com.typesafe.akka"         %%  "akka-actor"                          % akkaVersion,
    "com.typesafe.akka"         %%  "akka-stream"                         % akkaVersion,
    "com.typesafe.akka"         %%  "akka-persistence"                    % akkaVersion,
    "com.typesafe.akka"         %%  "akka-http-core"                      % "10.0.9",
    "joda-time"                 %   "joda-time"                           % "2.9.9"
  )
}
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.rename
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

mainClass in assembly := Some("com.wincom.dcim.driver.Main")
assemblyJarName in assembly := "domain.jar"

