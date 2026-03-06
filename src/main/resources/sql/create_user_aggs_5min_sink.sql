-- Sink table for Kafka
CREATE TABLE user_aggs_5min_sink (
    tenant_id STRING,
    user_id STRING,
    window_start TIMESTAMP(3),
    window_end TIMESTAMP(3),
    total_usage DOUBLE,
    event_count BIGINT,
    max_timestamp TIMESTAMP(3),
    PRIMARY KEY (tenant_id, user_id, window_start) NOT ENFORCED
) WITH (
    'connector' = 'upsert-kafka',
    'topic' = 'user-aggs-5min',
    'properties.bootstrap.servers' = 'kafka:29092',
    'key.format' = 'json',
    'value.format' = 'json'
);
