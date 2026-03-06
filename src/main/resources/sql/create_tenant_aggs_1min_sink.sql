-- Sink table for Kafka
CREATE TABLE tenant_aggs_1min_sink (
    tenant_id STRING,
    window_start TIMESTAMP(3),
    window_end TIMESTAMP(3),
    total_usage DOUBLE,
    event_count BIGINT,
    unique_users BIGINT,
    max_timestamp TIMESTAMP(3),
    PRIMARY KEY (tenant_id, window_start, window_end) NOT ENFORCED
) WITH (
    'connector' = 'upsert-kafka',
    'topic' = 'tenant-aggs-1min',
    'properties.bootstrap.servers' = 'kafka:29092',
    'key.format' = 'json',
    'value.format' = 'json',
    'value.fields-include' = 'EXCEPT_KEY',
    'value.json.fail-on-missing-field' = 'false',
    'value.json.ignore-parse-errors' = 'true'
);
