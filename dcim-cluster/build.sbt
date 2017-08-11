import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.wincom.dcim",
      scalaVersion := "2.11.1",
      version      := "1.0.0"
    )),
    name := "dcim-cluster",
    libraryDependencies += scalaTest % Test
  )
  .aggregate(domain, cluster, driver, driverMock)


publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

lazy val domain = (project in file("domain"))
lazy val cluster = (project in file("cluster")).dependsOn(driver)
lazy val driver = (project in file("driver")).dependsOn(domain)
lazy val driverMock = (project in file("driver-mock")).dependsOn(driver)
