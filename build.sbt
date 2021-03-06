import NativePackagerHelper._

organization := "com.maiden"


version := "0.7.0"

scalaVersion := "2.11.8"

val finagleVersion = "6.36.0"

val finchVersion = "0.10.0"

val quillVersion = "1.0.1"

val circeVersion = "0.5.4"

val catsVersion = "0.7.0"

val http4sVersion = "0.14.11"

val logbackVersion = "1.1.7"

val specsVersion = "3.8.3"


lazy val `maiden-framework` = (project in file("."))
  .settings(
    mappings in Universal ++= directory("config"),
    libraryDependencies ++= Seq(
      "org.spire-math" % "spire_2.11" % "0.11.0",
      "commons-io" % "commons-io" % "2.4",
      "com.github.finagle" %% "finch-core" % finchVersion,
      "com.github.finagle" %% "finch-circe" % finchVersion,
      "com.github.finagle" %% "finch-oauth2" % finchVersion,
      "com.twitter" %% "finagle-stats" % finagleVersion,
      "com.twitter" %% "finagle-zipkin" % finagleVersion,
      "com.twitter" % "twitter-server_2.11" % "1.21.0",
      "com.github.rlazoti" %% "finagle-metrics" % "0.0.3",
      "com.netaporter" %% "scala-uri" % "0.4.14",
      "org.typelevel" %% "cats" % catsVersion,

      //for easier reflection
      "org.scala-lang" % "scalap" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,

      //for arg parsing
      "com.github.scopt" %% "scopt" % "3.5.0",

      //for migrations
      "com.imageworks.scala-migrations" %% "scala-migrations" % "1.1.1",

      //quill
      "io.getquill" %% "quill-core" % quillVersion,
      "io.getquill" %% "quill-sql" % quillVersion,
      "io.getquill" %% "quill-jdbc" % quillVersion,
      "io.getquill" %% "quill-async" % quillVersion,

      //data drivers
      "mysql" % "mysql-connector-java" % "5.1.38",
      "org.postgresql" % "postgresql" % "9.4.1209",

      //for hashing functions
      "com.roundeights" %% "hasher" % "1.2.0",

      //for working with Facebook
      "com.restfb" % "restfb" % "1.14.0",
      //"joda-time" % "joda-time" % "2.8.1",

      //for HTTP client
      "org.http4s" %% "http4s-client" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.json4s" %% "json4s-native" % "3.4.0",

      //for validations
      "com.googlecode.libphonenumber" % "libphonenumber" % "7.4.4",

      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" % "circe-java8_2.11" % circeVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,

      // Utilities
      "joda-time" % "joda-time" % "2.9.3",
      "org.joda" % "joda-convert" % "1.8",
      "com.github.benhutchison" %% "mouse" % "0.2",
      "commons-codec" % "commons-codec" % "1.10",

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

    resolvers += "RoundEights" at "http://maven.spikemark.net/roundeights",


    scalacOptions ++= Seq("-Xlint", "-unchecked", "-deprecation"),

    scalacOptions in Test ++= Seq("-Yrangepos"),

    //fork in test := true

    //parallelExecution in Test := false

    testOptions in Test += Tests.Setup(() => System.setProperty("ENV", "test")),

    //javaOptions in Test := Seq("-DENV=test")
    seq(Revolver.settings: _*)



).enablePlugins(JavaAppPackaging)
