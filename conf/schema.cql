CREATE KEYSPACE "actyx" WITH replication = {
  'class': 'NetworkTopologyStrategy',
  'datacenter1': '1'
};

USE "actyx";

CREATE TABLE "samples" (
    id UUID,
    name text,
    current double,
    timestamp timestamp,
    PRIMARY KEY (id, timestamp)
) with CLUSTERING ORDER BY (timestamp DESC);