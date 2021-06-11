import NativePackagerHelper._
import sbt.Keys._

version := "0.1"

addCommandAlias("build", "prepare; test")

val Version = new {
  val http4s = "0.21.24"
  val zio = "1.0.8"
  val zioLogging = "0.5.10"
  val zioConfig = "1.0.6"
  val log4j = "2.13.3"
  val apacheCodec = "1.13"
  val korolev = "0.17.2"
}

val excludeSlf4jBinding = ExclusionRule(organization = "org.slf4j")
val excludeLogback = ExclusionRule(organization = "ch.qos.logback")

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, DockerSpotifyClientPlugin)
  .settings(
    organization := "a5000 Event Solutions",
    name := "sublimat-server",
    maintainer := "stranger82@bk.ru",
    scalaVersion := "2.13.4",
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    scalacOptions := Seq(
      "-feature",
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-unchecked",
    //  "-source:3.0-migration",
    //  "-Ykind-projector"
    ) ++ (if (isSnapshot.value) Seq.empty
    else
      Seq(
        "-opt:l:inline"
      )),
    scriptClasspath := Seq("*"),
    //resourceDirectory in Compile := (resourceDirectory in (server, Compile)).value,
    Universal / mappings += {
      ((Compile / resourceDirectory).value / "application.conf") -> "conf/application.conf"
    },
    Universal / mappings += {
      ((Compile / resourceDirectory).value / "log4j2.xml") -> "conf/log4j2.xml"
    },
    Universal / mappings ++= directory((Compile / sourceDirectory).value  / "main" / "webapp"),
    bashScriptConfigLocation := Some("${app_home}/../SERVER_config.txt"),

    libraryDependencies ++= Seq(
      ("org.http4s"                   %% "http4s-blaze-server" % Version.http4s).cross(CrossVersion.for3Use2_13),
      ("org.http4s"                   %% "http4s-dsl"          % Version.http4s).cross(CrossVersion.for3Use2_13),
      ("org.http4s"                   %% "http4s-blaze-client" % Version.http4s).cross(CrossVersion.for3Use2_13),

      "dev.zio"                      %% "zio"                 % Version.zio,
      "dev.zio"                      %% "zio-test"            % Version.zio  % "test",
      "dev.zio"                      %% "zio-test-sbt"        % Version.zio  % "test",
      ("dev.zio"                      %% "zio-interop-cats"    % "2.1.4.1").cross(CrossVersion.for3Use2_13),
      "dev.zio"                      %% "zio-logging"         % Version.zioLogging,
      "dev.zio"                      %% "zio-logging-slf4j"   % Version.zioLogging,
      "dev.zio"                      %% "zio-config" %   Version.zioConfig,
      "io.github.kitlangton" %% "zio-magic" % "0.3.2",
      "dev.zio" %% "zio-config-typesafe" %   Version.zioConfig,

      "org.apache.logging.log4j"      % "log4j-api"           % Version.log4j,
      "org.apache.logging.log4j"      % "log4j-core"          % Version.log4j,
      "org.apache.logging.log4j"      % "log4j-slf4j-impl"    % Version.log4j,
     // ("com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"  excludeAll(excludeSlf4jBinding)).cross(CrossVersion.for3Use2_13),

      "com.lmax"                       % "disruptor"          % "3.4.2",
      "commons-codec"                 % "commons-codec"       % Version.apacheCodec,

      "org.fomkin" %% "korolev" % Version.korolev,
      "org.fomkin" %% "korolev-zio" % Version.korolev,
      "org.fomkin" %% "korolev-http4s" % Version.korolev,

      "org.typelevel" %% "cats-effect" % "2.5.1",

      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
      compilerPlugin(("org.typelevel" % "kind-projector"      % "0.11.2").cross(CrossVersion.full))
    ).map(_.excludeAll(excludeLogback))
  )


