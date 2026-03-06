-- Insert aggregated data into Kafka sink
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
