import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.wincom.dcim",
      scalaVersion := "2.11.11",
      version      := "1.0.0"
    )),
    name := "dcim-cluster",
    libraryDependencies += scalaTest % Test
  )
  .aggregate(domain, cluster, driverCodec, driverMockCodec, fsuCodec, fsuMockCodec)


publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

lazy val domain = (project in file("domain")).dependsOn(driverCodec)
lazy val cluster = (project in file("cluster")).dependsOn(domain)
lazy val driverCodec = (project in file("driver-codec")).dependsOn(fsuCodec)
lazy val driverMockCodec = (project in file("driver-mock-codec")).dependsOn(driverCodec)
lazy val fsuCodec = (project in file("fsu-codec"))
lazy val fsuMockCodec = (project in file("fsu-mock-codec")).dependsOn(fsuCodec)
