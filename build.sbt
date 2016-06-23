organization := "com.maiden"

name := "maiden-framework"

version := "0.1.0"

scalaVersion := "2.11.8"

def fuckit(dir: java.io.File) = {
  val file = dir / "demo" / "Test.scala"
  IO.write(file, """object Test extends App { println("Hi") }""")
  Seq(file)
}

lazy val maiden = (project in file("."))
  .settings(
  sourceGenerators in Compile <+= sourceManaged in Compile map { dir =>
    fuckit(dir)
  },
    libraryDependencies ++= Seq(
      "com.github.finagle" % "finch-core_2.11" % "0.10.0",
      //"com.github.finagle" %% "finch-core" % "0.11.0-SNAPSHOT" changing(),
      //"com.github.finagle" %% "finch-circe" % "0.11.0-SNAPSHOT" changing(),
      "com.github.finagle" %% "finch-circe" % "0.10.0",
      "com.twitter" %% "twitter-server" % "1.20.0",
      "com.netaporter" %% "scala-uri" % "0.4.14",

      //for easier reflection
      "org.clapper" %% "classutil" % "1.0.11",
      "org.scala-lang" % "scalap" % "2.11.8",
      "org.scalastuff" % "scalabeans" % "0.3",
      "com.thoughtworks.paranamer" % "paranamer" % "2.4.1",
      "com.twitter" % "util-eval_2.11" % "6.34.0",

      "com.geirsson" %% "scalafmt" % "0.2.8",

      //for arg parsing
      "com.github.scopt" %% "scopt" % "3.5.0",

      //for migrations
      "com.imageworks.scala-migrations" %% "scala-migrations" % "1.1.1",

      //quill
      "io.getquill" % "quill_2.11" % "0.6.0",
      "io.getquill" % "quill-sql_2.11" % "0.6.0",
      "io.getquill" % "quill-jdbc_2.11" % "0.6.0",
      "io.getquill" % "quill-cassandra_2.11" % "0.6.0",

      //data drivers
      "mysql" % "mysql-connector-java" % "5.1.36",
      "org.postgresql" % "postgresql" % "9.4-1206-jdbc41",

      //for hashing functions
      "com.roundeights" %% "hasher" % "1.0.0",

      //for working with Facebook
      "com.restfb" % "restfb" % "1.14.0",
      "joda-time" % "joda-time" % "2.8.1",



      // Note. We use the latest 0.5.0 version of Circe as it pulls in the Cats 0.6.0 which Mouse depends on.
      "io.circe" %% "circe-generic" % "0.5.0-M1",
      //"io.circe" %% "circe-core" % "0.4.1",
      //"io.circe" %% "circe-generic" % "0.4.1",
      //"io.circe" %% "circe-parser" % "0.4.1",

      // Utilities
      "joda-time" % "joda-time" % "2.9.3",
      "org.joda" % "joda-convert" % "1.8",
      "com.github.benhutchison" %% "mouse" % "0.2",

      // Logging
      "ch.qos.logback" % "logback-core" % "1.1.7",
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "org.slf4j" % "slf4j-api" % "1.7.21",

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

    //resolvers += Resolver.sonatypeRepo("snapshots"),

    scalacOptions ++= Seq("-Xlint", "-unchecked", "-deprecation"),

    scalacOptions in Test ++= Seq("-Yrangepos"),

    //fork in test := true

    //parallelExecution in Test := false

    testOptions in Test += Tests.Setup(() => System.setProperty("ENV", "test")),

    //javaOptions in Test := Seq("-DENV=test")

    seq(Revolver.settings: _*)



).enablePlugins(JavaAppPackaging)
