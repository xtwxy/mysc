import sbtassembly.MergeStrategy

name := "cluster"
organization := "com.wincom.dcim"
version := "1.0.0"
scalaVersion := "2.12.3"

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

enablePlugins(JavaAppPackaging)
enablePlugins(JavaServerAppPackaging)
scriptClasspath +="../conf"

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
    "org.iq80.leveldb"          %  "leveldb"                              % "0.7",
    "org.fusesource.leveldbjni" %  "leveldbjni-all"                       % "1.8",

    "com.typesafe.akka"         %%  "akka-cluster"                        % akkaVersion,
    "com.typesafe.akka"         %%  "akka-cluster-tools"                  % akkaVersion,
    "com.typesafe.akka"         %%  "akka-cluster-sharding"               % akkaVersion,

    "com.typesafe.akka"         %%  "akka-http"                           % "10.0.9",
    "com.typesafe.akka"         %%  "akka-http-core"                      % "10.0.9",
    "com.typesafe.akka"         %%  "akka-http-spray-json"                % "10.0.9",

    "com.typesafe.akka"         %%  "akka-persistence-cassandra"          % "0.54",
    "com.typesafe.akka"         %%  "akka-persistence-cassandra-launcher" % "0.54" % Test,

    "org.reflections"           %   "reflections"                         % "0.9.11",
    "joda-time"                 %   "joda-time"                           % "2.9.9",
    "org.scalatest"             %%  "scalatest"                           % "3.0.1"       % "test",

    "com.typesafe.akka"         %%  "akka-testkit"                        % akkaVersion   % "test",
    "com.typesafe.akka"         %%  "akka-multi-node-testkit"             % akkaVersion   % "test",

    "com.typesafe.akka"         %%  "akka-slf4j"                          % akkaVersion,

    "commons-io"                %  "commons-io"                           % "2.4",
    "ch.qos.logback"            %  "logback-classic"                      % "1.1.2"
  )
}
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.rename
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

mainClass in assembly := Some("com.wincom.dcim.sharded.Main")
assemblyJarName in assembly := "cluster.jar"


