name := "akka-cluster-sharding"

version := "1.0"

scalaVersion := "2.12.2"

resolvers += Resolver.typesafeRepo("releases")

resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

libraryDependencies ++= Seq(
  "org.scalactic"               %% "scalactic"              % "3.0.1",
  "org.scalatest"               %% "scalatest"              % "3.0.1"   % Test,
  "com.typesafe.akka"           %% "akka-actor"             % "2.5.2",
  "com.typesafe.akka"           %% "akka-persistence"       % "2.5.2",
  "com.typesafe.akka"           %% "akka-distributed-data"  % "2.5.2",
  "com.typesafe.akka"           %% "akka-testkit"           % "2.5.2"   % Test,
  "com.typesafe.akka"           %% "akka-cluster"           % "2.5.2",
  "com.typesafe.akka"           %% "akka-cluster-metrics"   % "2.5.2",
  "com.typesafe.akka"           %% "akka-cluster-tools"     % "2.5.2",
  "com.typesafe.akka"           %% "akka-cluster-sharding"  % "2.5.2",
  "com.typesafe.akka"           %% "akka-slf4j"             % "2.5.2",
  "ch.qos.logback"              % "logback-classic"         % "1.1.7",
  "org.apache.commons"          %"commons-lang3"            % "3.5",
  "org.iq80.leveldb"            % "leveldb"                 % "0.7",
  "org.fusesource.leveldbjni"   % "leveldbjni-all"          % "1.8"
)

// logLevel := Level.Debug

scalacOptions in Test ++= Seq("-Yrangepos")