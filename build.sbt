import NativePackagerHelper._

organization := "com.maiden"

//name := "maiden-framework"

version := "0.1.0"

scalaVersion := "2.11.8"

val finagleVersion = "6.35.0"

val finchVersion = "0.10.0"

val quillVersion = "0.7.0"

val circeVersion = "0.5.0-M2"

val http4sVersion = "0.13.2a"

val logbackVersion = "1.1.7"

val specsVersion = "3.8.3"

lazy val `maiden-framework` = (project in file("."))
  .settings(
    mappings in Universal ++= directory("config"),
    libraryDependencies ++= Seq(
      "commons-io" % "commons-io" % "2.4",
      "com.github.finagle" %% "finch-core" % finchVersion,
      "com.twitter" %% "finagle-stats" % finagleVersion,
      "com.twitter" %% "finagle-zipkin" % finagleVersion,
      "com.github.finagle" %% "finch-circe" % finchVersion,
      "com.github.finagle" %% "finch-oauth2" % finchVersion,
      "io.github.benwhitehead.finch" %% "finch-server" % "0.9.1",
      "com.github.rlazoti" %% "finagle-metrics" % "0.0.3",
      "com.netaporter" %% "scala-uri" % "0.4.14",

      //for easier reflection
      "org.scala-lang" % "scalap" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,

      //for arg parsing
      "com.github.scopt" %% "scopt" % "3.5.0",

      //for migrations
      "com.imageworks.scala-migrations" %% "scala-migrations" % "1.1.1",

      //quill
      "io.getquill" %% "quill" % quillVersion,
      "io.getquill" %% "quill-sql" % quillVersion,
      "io.getquill" %% "quill-cassandra" % quillVersion,

      //data drivers
      "mysql" % "mysql-connector-java" % "5.1.36",
      "org.postgresql" % "postgresql" % "9.4-1206-jdbc41",

      //for hashing functions
      "com.roundeights" %% "hasher" % "1.0.0",

      //for working with Facebook
      "com.restfb" % "restfb" % "1.14.0",
      "joda-time" % "joda-time" % "2.8.1",

      //for HTTP client
      "org.http4s" %% "http4s-client" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.json4s" %% "json4s-native" % "3.3.0",

      // Note. We use the latest 0.5.0 version of Circe as it pulls in the Cats 0.6.0 which Mouse depends on.
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" % "circe-java8_2.11" % circeVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,

      // Utilities
      "joda-time" % "joda-time" % "2.9.3",
      "org.joda" % "joda-convert" % "1.8",
      "com.github.benhutchison" %% "mouse" % "0.2",

      // Logging
      "ch.qos.logback" % "logback-core" % logbackVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,

      // Monitoring
      "com.rollbar" % "rollbar" % "0.5.2",

      // Testing
      "org.specs2" %% "specs2-core" % specsVersion % "test",
      "org.specs2" %% "specs2-mock" % specsVersion % "test",
      "org.specs2" %% "specs2-scalacheck" % specsVersion % "test",
      "org.specs2" %% "specs2-cats" % specsVersion % "test",
      "org.hamcrest" % "hamcrest-core" % "1.3" % "test",
      "org.mockito" % "mockito-all" % "1.10.19" % "test"
    //  "org.scalacheck" %% "scalacheck" % "1.13.1" % "test"
    ),

    resolvers += "Twitter" at "http://maven.twttr.com",

    resolvers += Resolver.sonatypeRepo("snapshots"),

    scalacOptions ++= Seq("-Xlint", "-unchecked", "-deprecation"),

    scalacOptions in Test ++= Seq("-Yrangepos"),

    //fork in test := true

    //parallelExecution in Test := false

    testOptions in Test += Tests.Setup(() => System.setProperty("ENV", "test")),

    //javaOptions in Test := Seq("-DENV=test")
    seq(Revolver.settings: _*)



).enablePlugins(JavaAppPackaging)
