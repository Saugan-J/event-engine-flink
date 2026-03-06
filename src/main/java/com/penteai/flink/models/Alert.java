package com.penteai.flink.models;

import java.io.Serializable;
import java.util.UUID;

public class Alert implements Serializable {
    private static final long serialVersionUID = 1L;

    private String alertId;
    private String alertType;      // TENANT_THRESHOLD or USER_THRESHOLD
    private String tenantId;
    private String userId;         // null for tenant-level alerts
    private long windowStart;
    private long windowEnd;
    private double currentValue;
    private double thresholdLimit;
    private String severity;       // WARNING or CRITICAL
    private long alertTimestamp;
    private long eventCount;

    public Alert() {
        this.alertId = UUID.randomUUID().toString();
        this.alertTimestamp = System.currentTimeMillis();
    }

    public Alert(String alertType, String tenantId, String userId, long windowStart, long windowEnd,
                double currentValue, double thresholdLimit, String severity, long eventCount) {
        this();
        this.alertType = alertType;
        this.tenantId = tenantId;
        this.userId = userId;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.currentValue = currentValue;
        this.thresholdLimit = thresholdLimit;
        this.severity = severity;
        this.eventCount = eventCount;
    }

    // Getters and setters
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }
    
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public long getWindowStart() { return windowStart; }
    public void setWindowStart(long windowStart) { this.windowStart = windowStart; }
    
    public long getWindowEnd() { return windowEnd; }
    public void setWindowEnd(long windowEnd) { this.windowEnd = windowEnd; }
    
    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }
    
    public double getThresholdLimit() { return thresholdLimit; }
    public void setThresholdLimit(double thresholdLimit) { this.thresholdLimit = thresholdLimit; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public long getAlertTimestamp() { return alertTimestamp; }
    public void setAlertTimestamp(long alertTimestamp) { this.alertTimestamp = alertTimestamp; }
    
    public long getEventCount() { return eventCount; }
    public void setEventCount(long eventCount) { this.eventCount = eventCount; }

    @Override
    public String toString() {
        return "Alert{" +
                "alertId='" + alertId + '\'' +
                ", alertType='" + alertType + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", userId='" + userId + '\'' +
                ", windowStart=" + windowStart +
                ", windowEnd=" + windowEnd +
                ", currentValue=" + currentValue +
                ", thresholdLimit=" + thresholdLimit +
                ", severity='" + severity + '\'' +
                ", alertTimestamp=" + alertTimestamp +
                ", eventCount=" + eventCount +
                '}';
    }
}
