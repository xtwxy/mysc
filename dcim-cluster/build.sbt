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
  .aggregate(domain, cluster, rest, driverCodec, driverMockCodec, fsuCodec, fsuMockCodec).dependsOn(cluster)

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
enablePlugins(JavaAppPackaging)
enablePlugins(JavaServerAppPackaging)
scriptClasspath +="../conf"

parallelExecution in Test := false

fork := true

lazy val domain = (project in file("domain")).dependsOn(driverCodec)
lazy val cluster = (project in file("cluster")).dependsOn(domain, rest, fsuMockCodec)
lazy val driverCodec = (project in file("driver-codec")).dependsOn(fsuCodec)
lazy val driverMockCodec = (project in file("driver-mock-codec")).dependsOn(driverCodec)
lazy val fsuCodec = (project in file("fsu-codec"))
lazy val fsuMockCodec = (project in file("fsu-mock-codec")).dependsOn(fsuCodec)
lazy val rest = (project in file("rest")).dependsOn(domain)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.rename
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

mainClass in assembly := Some("com.wincom.dcim.sharded.Main")
assemblyJarName in assembly := "dcim-cluster.jar"

