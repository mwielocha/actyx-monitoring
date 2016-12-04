CREATE KEYSPACE "actyx" WITH replication = {
  'class': 'NetworkTopologyStrategy',
  'datacenter1': '1'
};

USE "actyx";

CREATE TABLE "sample" (
    id UUID,
    name text,
    current double,
    pressure double,
    humidity double,
    temperature double,
    timestamp timestamp,
    PRIMARY KEY (id, timestamp)
) with CLUSTERING ORDER BY (timestamp DESC);