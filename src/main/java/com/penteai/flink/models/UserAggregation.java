package com.penteai.flink.models;

import java.io.Serializable;

public class UserAggregation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenantId;
    private String userId;
    private long windowStart;
    private long windowEnd;
    private double totalUsage;
    private long eventCount;

    public UserAggregation() {}

    public UserAggregation(String tenantId, String userId, long windowStart, long windowEnd, 
                          double totalUsage, long eventCount) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.totalUsage = totalUsage;
        this.eventCount = eventCount;
    }

    // Getters and setters
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public long getWindowStart() { return windowStart; }
    public void setWindowStart(long windowStart) { this.windowStart = windowStart; }
    
    public long getWindowEnd() { return windowEnd; }
    public void setWindowEnd(long windowEnd) { this.windowEnd = windowEnd; }
    
    public double getTotalUsage() { return totalUsage; }
    public void setTotalUsage(double totalUsage) { this.totalUsage = totalUsage; }
    
    public long getEventCount() { return eventCount; }
    public void setEventCount(long eventCount) { this.eventCount = eventCount; }

    @Override
    public String toString() {
        return "UserAggregation{" +
                "tenantId='" + tenantId + '\'' +
                ", userId='" + userId + '\'' +
                ", windowStart=" + windowStart +
                ", windowEnd=" + windowEnd +
                ", totalUsage=" + totalUsage +
                ", eventCount=" + eventCount +
                '}';
    }
}
