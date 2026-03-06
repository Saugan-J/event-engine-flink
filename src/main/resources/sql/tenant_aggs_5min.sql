-- Per-Tenant 5-Minute Aggregations

CREATE TABLE tenant_aggs_5min_sink (
    tenant_id STRING,
    window_start TIMESTAMP(3),
    window_end TIMESTAMP(3),
    total_usage DOUBLE,
    event_count BIGINT,
    unique_users BIGINT,
    max_timestamp TIMESTAMP(3),
    PRIMARY KEY (tenant_id, window_start) NOT ENFORCED
) WITH (
    'connector' = 'upsert-kafka',
    'topic' = 'tenant-aggs-5min',
    'properties.bootstrap.servers' = 'kafka:29092',
    'key.format' = 'json',
    'value.format' = 'json'
);

INSERT INTO tenant_aggs_5min_sink
SELECT 
    org_id AS tenant_id,
    TUMBLE_START(event_time, INTERVAL '5' MINUTE) AS window_start,
    TUMBLE_END(event_time, INTERVAL '5' MINUTE) AS window_end,
    SUM(amount) AS total_usage,
    COUNT(*) AS event_count,
    COUNT(DISTINCT user_id) AS unique_users,
    MAX(event_time) AS max_timestamp
FROM usage_events
GROUP BY 
    org_id,
    TUMBLE(event_time, INTERVAL '5' MINUTE);
