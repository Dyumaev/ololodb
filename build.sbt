name := "ololodb"
version := "0.1"
scalaVersion := "2.12.11"

lazy val global = project
  .in(file("."))
  .settings(settings)
  .disablePlugins(AssemblyPlugin, ProtocPlugin)
  .aggregate(
    core,
    grpc,
    etcd
  )

lazy val core = project
  .settings(
    name := "core",
    settings,
    libraryDependencies ++= coreDependencies
  )
  .disablePlugins(AssemblyPlugin, ProtocPlugin)

lazy val grpc = project
  .settings(
    name := "grpc",
    settings,
    assemblySettings,
    protocSettings,
    libraryDependencies ++= coreDependencies ++ grpcDependencies
  )
  .dependsOn(core)

lazy val etcd = project
  .settings(
    name := "etcd",
    settings,
    assemblySettings,
    libraryDependencies ++= coreDependencies ++ etcdDependencies
  )
  .dependsOn(core)

lazy val dependencies =
  new {
    val monixV = "3.2.1"
    val fs2V = "2.2.1"
    val rocksdbV = "6.6.4"
    val jetcdV = "0.5.3"
    val scalapbRuntimeV = scalapb.compiler.Version.scalapbVersion
    val grpcNettyV = scalapb.compiler.Version.grpcJavaVersion
    val kindProjectorV = "0.11.0"
    val betterMonadicForV = "0.3.1"

    val monix = "io.monix" %% "monix" % monixV
    val fs2 = "co.fs2" %% "fs2-core" % fs2V
    val rocksdb = "org.rocksdb" % "rocksdbjni" % rocksdbV
    val jetcd = "io.etcd" % "jetcd-core" % jetcdV
    val scalapbRuntime = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbRuntimeV % "protobuf"
    val scalapbRuntimeGrpc = "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapbRuntimeV
    val grpcNetty = "io.grpc" % "grpc-netty" % grpcNettyV
    val kindProjector = compilerPlugin("org.typelevel" %% "kind-projector" % kindProjectorV).cross(CrossVersion.full)
    val betterMonadicFor = compilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicForV)
  }

lazy val coreDependencies = Seq(
  dependencies.monix,
  dependencies.fs2,
  dependencies.rocksdb,
  dependencies.kindProjector,
  dependencies.betterMonadicFor
)

lazy val grpcDependencies = Seq(
  dependencies.scalapbRuntime,
  dependencies.scalapbRuntimeGrpc,
  dependencies.grpcNetty
)

lazy val etcdDependencies = Seq(
  dependencies.jetcd
)

lazy val settings = Seq(
  scalacOptions ++= Seq(
    "-unchecked",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-deprecation",
    "-encoding",
    "utf8"
  ),
  resolvers ++= Seq(
    "Local Maven Repository".at("file://" + Path.userHome.absolutePath + "/.m2/repository"),
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  )
)

lazy val assemblySettings = Seq(
  assemblyJarName in assembly := name.value + ".jar",
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case "application.conf"            => MergeStrategy.concat
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

lazy val protocSettings = Seq(
  PB.targets in Compile := Seq(
    scalapb.gen(flatPackage = true) -> (sourceManaged in Compile).value
  )
)
