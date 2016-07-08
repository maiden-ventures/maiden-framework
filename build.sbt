import NativePackagerHelper._

organization := "com.maiden"

//name := "maiden-framework"

version := "0.1.0"

scalaVersion := "2.11.8"

lazy val `maiden-framework` = (project in file("."))
  .settings(

    mappings in Universal ++= directory("config"),
    libraryDependencies ++= Seq(
      "commons-io" % "commons-io" % "2.4",
      "com.github.finagle" % "finch-core_2.11" % "0.10.0",
      "com.twitter" % "finagle-stats_2.11" % "6.35.0",
      "com.twitter" % "finagle-zipkin_2.11" % "6.35.0",
      "com.github.finagle" %% "finch-circe" % "0.10.0",
      "com.github.finagle" %% "finch-oauth2" % "0.10.0",
      //"com.twitter" %% "twitter-server" % "1.20.0",
      "io.github.benwhitehead.finch" %% "finch-server" % "0.9.1",
      "com.github.rlazoti" % "finagle-metrics_2.11" % "0.0.3",
      "com.netaporter" %% "scala-uri" % "0.4.14",

      //for easier reflection
      "org.scala-lang" % "scalap" % "2.11.8",
      "org.scala-lang" % "scala-reflect" % "2.11.8",

      //for arg parsing
      "com.github.scopt" %% "scopt" % "3.5.0",

      //for migrations
      "com.imageworks.scala-migrations" %% "scala-migrations" % "1.1.1",

      //quill
      //"io.getquill" % "quill_2.11" % "0.6.1-SNAPSHOT",
      //"io.getquill" % "quill-sql_2.11" % "0.6.1-SNAPSHOT",
      "io.getquill" % "quill_2.11" % "0.7.0",
      "io.getquill" % "quill-sql_2.11" % "0.7.0",
      //"io.getquill" % "quill-cassandra_2.11" % "0.6.0",

      //yaml stuff
      //"net.jcazevedo" %% "moultingyaml" % "0.2",
      //data drivers
      "mysql" % "mysql-connector-java" % "5.1.36",
      "org.postgresql" % "postgresql" % "9.4-1206-jdbc41",

      //for hashing functions
      "com.roundeights" %% "hasher" % "1.0.0",

      //for working with Facebook
      "com.restfb" % "restfb" % "1.14.0",
      "joda-time" % "joda-time" % "2.8.1",

      //for HTTP client
      "org.http4s" % "http4s-client_2.11" % "0.13.2a",
      "org.http4s" % "http4s-blaze-client_2.11" % "0.13.2a",
      "org.json4s" %% "json4s-native" % "3.3.0",

      // Note. We use the latest 0.5.0 version of Circe as it pulls in the Cats 0.6.0 which Mouse depends on.
      "io.circe" %% "circe-generic" % "0.5.0-M2",
      "io.circe" % "circe-java8_2.11" % "0.5.0-M2",
      "io.circe" %% "circe-core" % "0.5.0-M2",
      "io.circe" %% "circe-parser" % "0.5.0-M2",

      // Utilities
      "joda-time" % "joda-time" % "2.9.3",
      "org.joda" % "joda-convert" % "1.8",
      "com.github.benhutchison" %% "mouse" % "0.2",

      // Logging
      "ch.qos.logback" % "logback-core" % "1.1.7",
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      //"org.slf4j" % "slf4j-api" % "1.7.21",

      // Monitoring
      "com.rollbar" % "rollbar" % "0.5.2",

      // Testing
      "org.specs2" %% "specs2-core" % "3.8.3" % "test",
      "org.specs2" %% "specs2-mock" % "3.8.3" % "test",
      "org.specs2" %% "specs2-scalacheck" % "3.8.3" % "test",
      "org.specs2" %% "specs2-cats" % "3.8.3" % "test",
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
