-- Sink table for Kafka
CREATE TABLE tenant_aggs_1min_sink (
    tenant_id STRING,
    window_start TIMESTAMP(3),
    window_end TIMESTAMP(3),
    total_usage DOUBLE,
    event_count BIGINT,
    unique_users BIGINT,
    max_timestamp TIMESTAMP(3)
) WITH (
    'connector' = 'kafka',
    'topic' = 'tenant-aggs-1min',
    'properties.bootstrap.servers' = 'kafka:29092',
    'format' = 'json',
    'json.fail-on-missing-field' = 'false',
    'json.ignore-parse-errors' = 'true',
    'key.format' = 'json',
    'key.fields' = 'tenant_id;window_start'
);
