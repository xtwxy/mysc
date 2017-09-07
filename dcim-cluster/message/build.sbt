import com.trueaccord.scalapb.compiler.Version.scalapbVersion

name := "message"
organization := "com.wincom.dcim"
version := "1.0.0"

scalaVersion := "2.12.3"

lazy val akkaVersion = "2.5.3"

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

libraryDependencies ++= Seq(
  "com.trueaccord.scalapb" %% "compilerplugin" % scalapbVersion,
  "com.trueaccord.scalapb" %% "scalapb-runtime" % scalapbVersion % "protobuf"
)

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)


