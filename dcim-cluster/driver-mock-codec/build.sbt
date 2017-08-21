import sbtassembly.MergeStrategy

name := "driver-mock-codec"
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
  Seq(
    //"com.typesafe.akka"         %%  "akka-actor"                          % akkaVersion,
    //"org.reflections"           %   "reflections"                         % "0.9.11"
  )
}
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.rename
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

mainClass in assembly := Some("com.wincom.dcim.sharded.Main")
assemblyJarName in assembly := "driver-mock-codec.jar"


