-- Insert aggregated data into Kafka sink (Window TVF syntax for Flink 1.18+)
INSERT INTO user_aggs_1min_sink
SELECT
    org_id AS tenant_id,
    user_id,
    window_start,
    window_end,
    SUM(amount) AS total_usage,
    COUNT(*) AS event_count,
    CURRENT_TIMESTAMP AS max_timestamp
FROM TABLE(
    TUMBLE(TABLE usage_events, DESCRIPTOR(proc_time), INTERVAL '10' SECOND)
)
GROUP BY
    org_id,
    user_id,
    window_start,
    window_end;
