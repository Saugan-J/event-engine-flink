package com.penteai.flink.models;

import java.io.Serializable;

public class TenantAggregation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenantId;
    private long windowStart;
    private long windowEnd;
    private double totalUsage;
    private long eventCount;
    private long uniqueUsers;
    private long maxTimestamp;

    public TenantAggregation() {}

    public TenantAggregation(String tenantId, long windowStart, long windowEnd, 
                            double totalUsage, long eventCount, long uniqueUsers, long maxTimestamp) {
        this.tenantId = tenantId;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.totalUsage = totalUsage;
        this.eventCount = eventCount;
        this.uniqueUsers = uniqueUsers;
        this.maxTimestamp = maxTimestamp;
    }

    // Getters and setters
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public long getWindowStart() { return windowStart; }
    public void setWindowStart(long windowStart) { this.windowStart = windowStart; }
    
    public long getWindowEnd() { return windowEnd; }
    public void setWindowEnd(long windowEnd) { this.windowEnd = windowEnd; }
    
    public double getTotalUsage() { return totalUsage; }
    public void setTotalUsage(double totalUsage) { this.totalUsage = totalUsage; }
    
    public long getEventCount() { return eventCount; }
    public void setEventCount(long eventCount) { this.eventCount = eventCount; }
    
    public long getUniqueUsers() { return uniqueUsers; }
    public void setUniqueUsers(long uniqueUsers) { this.uniqueUsers = uniqueUsers; }
    
    public long getMaxTimestamp() { return maxTimestamp; }
    public void setMaxTimestamp(long maxTimestamp) { this.maxTimestamp = maxTimestamp; }

    @Override
    public String toString() {
        return "TenantAggregation{" +
                "tenantId='" + tenantId + '\'' +
                ", windowStart=" + windowStart +
                ", windowEnd=" + windowEnd +
                ", totalUsage=" + totalUsage +
                ", eventCount=" + eventCount +
                ", uniqueUsers=" + uniqueUsers +
                ", maxTimestamp=" + maxTimestamp +
                '}';
    }
}
