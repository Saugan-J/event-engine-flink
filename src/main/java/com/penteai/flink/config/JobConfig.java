package com.penteai.flink.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.Serializable;

public class JobConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String kafkaBrokers;
    private final String sourceTopic;
    private final String consumerGroup;
    
    private final String tenantAggs1minTopic;
    private final String tenantAggs5minTopic;
    private final String userAggs1minTopic;
    private final String userAggs5minTopic;
    private final String alertsTopic;
    
    private final String redisHost;
    private final int redisPort;
    private final int redisTtlSeconds;
    
    private final int watermarkMaxOutOfOrdernessSeconds;
    private final int watermarkIdleSourceTimeoutSeconds;
    
    private final double tenantThresholdPerMinute;
    private final double userThresholdPerMinute;
    
    private final long checkpointIntervalMs;
    private final long checkpointTimeoutMs;
    private final long checkpointMinPauseMs;
    
    private final int sourceParallelism;
    private final int aggregationParallelism;
    private final int sinkParallelism;

    public JobConfig() {
        Config config = ConfigFactory.load();
        
        // Kafka configuration
        this.kafkaBrokers = config.getString("flink.kafka.brokers");
        this.sourceTopic = config.getString("flink.kafka.source-topic");
        this.consumerGroup = config.getString("flink.kafka.consumer-group");
        
        this.tenantAggs1minTopic = config.getString("flink.kafka.sink-topics.tenant-aggs-1min");
        this.tenantAggs5minTopic = config.getString("flink.kafka.sink-topics.tenant-aggs-5min");
        this.userAggs1minTopic = config.getString("flink.kafka.sink-topics.user-aggs-1min");
        this.userAggs5minTopic = config.getString("flink.kafka.sink-topics.user-aggs-5min");
        this.alertsTopic = config.getString("flink.kafka.sink-topics.alerts");
        
        // Redis configuration
        this.redisHost = config.getString("flink.redis.host");
        this.redisPort = config.getInt("flink.redis.port");
        this.redisTtlSeconds = config.getInt("flink.redis.ttl-seconds");
        
        // Watermark configuration
        this.watermarkMaxOutOfOrdernessSeconds = config.getInt("flink.watermark.max-out-of-orderness-seconds");
        this.watermarkIdleSourceTimeoutSeconds = config.getInt("flink.watermark.idle-source-timeout-seconds");
        
        // Alert thresholds
        this.tenantThresholdPerMinute = config.getDouble("flink.alerts.tenant-threshold-per-minute");
        this.userThresholdPerMinute = config.getDouble("flink.alerts.user-threshold-per-minute");
        
        // Checkpoint configuration
        this.checkpointIntervalMs = config.getLong("flink.checkpoint.interval-ms");
        this.checkpointTimeoutMs = config.getLong("flink.checkpoint.timeout-ms");
        this.checkpointMinPauseMs = config.getLong("flink.checkpoint.min-pause-ms");
        
        // Parallelism
        this.sourceParallelism = config.getInt("flink.parallelism.source");
        this.aggregationParallelism = config.getInt("flink.parallelism.aggregation");
        this.sinkParallelism = config.getInt("flink.parallelism.sink");
    }

    // Getters
    public String getKafkaBrokers() { return kafkaBrokers; }
    public String getSourceTopic() { return sourceTopic; }
    public String getConsumerGroup() { return consumerGroup; }
    public String getTenantAggs1minTopic() { return tenantAggs1minTopic; }
    public String getTenantAggs5minTopic() { return tenantAggs5minTopic; }
    public String getUserAggs1minTopic() { return userAggs1minTopic; }
    public String getUserAggs5minTopic() { return userAggs5minTopic; }
    public String getAlertsTopic() { return alertsTopic; }
    public String getRedisHost() { return redisHost; }
    public int getRedisPort() { return redisPort; }
    public int getRedisTtlSeconds() { return redisTtlSeconds; }
    public int getWatermarkMaxOutOfOrdernessSeconds() { return watermarkMaxOutOfOrdernessSeconds; }
    public int getWatermarkIdleSourceTimeoutSeconds() { return watermarkIdleSourceTimeoutSeconds; }
    public double getTenantThresholdPerMinute() { return tenantThresholdPerMinute; }
    public double getUserThresholdPerMinute() { return userThresholdPerMinute; }
    public long getCheckpointIntervalMs() { return checkpointIntervalMs; }
    public long getCheckpointTimeoutMs() { return checkpointTimeoutMs; }
    public long getCheckpointMinPauseMs() { return checkpointMinPauseMs; }
    public int getSourceParallelism() { return sourceParallelism; }
    public int getAggregationParallelism() { return aggregationParallelism; }
    public int getSinkParallelism() { return sinkParallelism; }
}
