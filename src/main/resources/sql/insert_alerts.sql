-- Insert alerts for tenant-level thresholds (Window TVF syntax for Flink 1.18+)
INSERT INTO usage_alerts_sink
SELECT
    CONCAT('ALERT-', CAST(CURRENT_TIMESTAMP AS STRING), '-', org_id) AS alert_id,
    'TENANT_THRESHOLD' AS alert_type,
    org_id AS tenant_id,
    CAST(NULL AS STRING) AS user_id,
    window_start,
    window_end,
    SUM(amount) AS current_value,
    10000.0 AS threshold_limit,
    CASE
        WHEN SUM(amount) > 15000 THEN 'CRITICAL'
        ELSE 'WARNING'
    END AS severity,
    CURRENT_TIMESTAMP AS alert_timestamp,
    COUNT(*) AS event_count
FROM TABLE(
    TUMBLE(TABLE usage_events, DESCRIPTOR(event_time), INTERVAL '5' MINUTE)
)
GROUP BY
    org_id,
    window_start,
    window_end
HAVING SUM(amount) > 10000;
