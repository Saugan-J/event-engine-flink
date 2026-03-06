-- Per-User-Within-Tenant 5-Minute Aggregations

CREATE TABLE user_aggs_5min_sink (
    tenant_id STRING,
    user_id STRING,
    window_start TIMESTAMP(3),
    window_end TIMESTAMP(3),
    total_usage DOUBLE,
    event_count BIGINT,
    PRIMARY KEY (tenant_id, user_id, window_start) NOT ENFORCED
) WITH (
    'connector' = 'upsert-kafka',
    'topic' = 'user-aggs-5min',
    'properties.bootstrap.servers' = 'kafka:29092',
    'key.format' = 'json',
    'value.format' = 'json'
);

INSERT INTO user_aggs_5min_sink
SELECT 
    org_id AS tenant_id,
    user_id,
    TUMBLE_START(event_time, INTERVAL '5' MINUTE) AS window_start,
    TUMBLE_END(event_time, INTERVAL '5' MINUTE) AS window_end,
    SUM(amount) AS total_usage,
    COUNT(*) AS event_count
FROM usage_events
GROUP BY 
    org_id,
    user_id,
    TUMBLE(event_time, INTERVAL '5' MINUTE);
