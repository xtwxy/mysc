import Dependencies._
import sbtassembly.MergeStrategy

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.wincom.dcim",
      scalaVersion := "2.12.3",
      version      := "1.0.0"
    )),
    name := "dcim-cluster",
    libraryDependencies += scalaTest % Test
  )
  .aggregate(domain, cluster, rest, driverMockCodec, fsuMockCodec).dependsOn(cluster)

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
enablePlugins(JavaAppPackaging)
enablePlugins(JavaServerAppPackaging)
scriptClasspath +="../conf"

parallelExecution in Test := false

fork := true

lazy val domain = (project in file("domain"))
lazy val cluster = (project in file("cluster")).dependsOn(domain, rest, fsuMockCodec, driverMockCodec)
lazy val driverMockCodec = (project in file("driver-mock-codec")).dependsOn(domain)
lazy val fsuMockCodec = (project in file("fsu-mock-codec")).dependsOn(domain)
lazy val rest = (project in file("rest")).dependsOn(domain)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.rename
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

mainClass in assembly := Some("com.wincom.dcim.sharded.Main")
assemblyJarName in assembly := "dcim-cluster.jar"

