-- Usage Alerts

CREATE TABLE usage_alerts_sink (
    alert_id STRING,
    alert_type STRING,          -- 'TENANT_THRESHOLD' | 'USER_THRESHOLD'
    tenant_id STRING,
    user_id STRING,             -- NULL for tenant-level alerts
    window_start TIMESTAMP(3),
    window_end TIMESTAMP(3),
    current_value DOUBLE,
    threshold_limit DOUBLE,
    severity STRING,            -- 'WARNING' | 'CRITICAL'
    alert_timestamp TIMESTAMP(3),
    event_count BIGINT
) WITH (
    'connector' = 'kafka',
    'topic' = 'usage-alerts',
    'properties.bootstrap.servers' = 'kafka:29092',
    'format' = 'json'
);
