# Actyx Monitoring

Akka Http/Streams solution to Actyx Tech Challenge number 1, "Power Usage Alert": https://www.actyx.io/en/tech-challenges/

Testing:

```bash
sbt test
```
Running locally:

```bash
export HTTP_HOST=localhost:9000; sbt run
```
(requires cassandra 3.x running on localhost)

Live demo can be found here: http://actyx-monitoring.wmdev.org
