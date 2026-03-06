-- Source Table: Usage Events from Kafka
CREATE TABLE usage_events (
    event_id STRING,
    org_id STRING,              -- tenant_id
    user_id STRING,
    event_type STRING,
    amount DOUBLE,              -- usage_amount
    unit STRING,
    service STRING,
    timestamp_ms BIGINT,
    
    -- Event time attribute from timestamp_ms field
    event_time AS TO_TIMESTAMP_LTZ(timestamp_ms, 3),
    WATERMARK FOR event_time AS event_time - INTERVAL '2' SECOND,
    proc_time AS PROCTIME()
    
) WITH (
    'connector' = 'kafka',
    'topic' = 'usage-events',
    'properties.bootstrap.servers' = 'kafka:29092',
    'properties.group.id' = 'flink-realtime-aggs',
    'scan.startup.mode' = 'latest-offset',
    'scan.watermark.idle-timeout' = '5 s',
    'properties.isolation.level' = 'read_committed',
    'format' = 'json',
    'json.fail-on-missing-field' = 'false',
    'json.ignore-parse-errors' = 'true'
);
