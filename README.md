
# Quick Start

1. Install [Java 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) from Oracle.
   You will need a JDK (not a JRE), as of the time of writing this is "Java SE Development Kit 8u92". There is also
   [documentation](http://www.oracle.com/technetwork/java/javase/documentation/jdk8-doc-downloads-2133158.html)
   available (handy for linking into your IDE).

2. Install SBT [sbt](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html)::

3. ```sbt publish-local```

4. ```./scripts/generate --model-file=examples/yml/models.yml --project-file=./examples/yml/project.yml --source-directory=[some_dir]```

5. Create a Postgres DB named test (owner: test, password: test)

6. ```cd [some_dir]```

7. ```sbt "migrate up"```

8. ```sbt run```

9. To add some data do:
  * ```curl -H "Content-Type: application/json" -X PUT -H "X-MAIDEN-AT: abcdefg" -d '{"firstName": "test_user"}' http://localhost:7070/api/test```

  * ```curl -H "Content-Type: application/json" -X PUT -H "X-MAIDEN-AT: abcdefg" -d '{"firstName": "test_user", "telephone": "206-300-2339", "zipCode": "98117"}' http://localhost:7070/api/test2```

  * To view in your browser visit ```http://localhost:7070/api/test/1?token=abcdefg`
  (*NOTE* -- token can be passed either in the headers or in the URL params)

  * To benchmark: install `wrk` and type ```wrk -t8 -c1000 -d10s http://localhost:7070/api/test/1/\?token=abcdefg```

  *NOTE* Run the above 4 or 5 times and you will  see the req/s increase. The JVM needs some time to warm-up. On moderate hardware you should see in excess of 5k req/sec


# More

There is a *ton* more here but this will have to do for now. This framework is already in very heavy production use. Stay tuned.
