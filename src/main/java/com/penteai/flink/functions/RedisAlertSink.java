package com.penteai.flink.functions;

import com.google.gson.Gson;
import com.penteai.flink.models.Alert;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * Async sink for writing alerts to Redis sorted set for quick access to recent alerts
 */
public class RedisAlertSink extends RichAsyncFunction<Alert, Void> {
    private static final Logger LOG = LoggerFactory.getLogger(RedisAlertSink.class);
    private static final long serialVersionUID = 1L;

    private final String redisHost;
    private final int redisPort;
    private static final String ALERTS_KEY = "alerts:recent";
    private static final int MAX_ALERTS = 1000; // Keep last 1000 alerts

    private transient RedisClient redisClient;
    private transient StatefulRedisConnection<String, String> connection;
    private transient RedisAsyncCommands<String, String> asyncCommands;
    private transient Gson gson;

    public RedisAlertSink(String redisHost, int redisPort) {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        
        LOG.info("Connecting to Redis at {}:{}", redisHost, redisPort);
        RedisURI redisUri = RedisURI.create(redisHost, redisPort);
        redisClient = RedisClient.create(redisUri);
        connection = redisClient.connect();
        asyncCommands = connection.async();
        gson = new Gson();
        
        LOG.info("Redis connection established successfully");
    }

    @Override
    public void asyncInvoke(Alert alert, ResultFuture<Void> resultFuture) {
        String value = gson.toJson(alert);
        double score = (double) alert.getAlertTimestamp();

        // Add to sorted set with timestamp as score
        RedisFuture<Long> zaddFuture = asyncCommands.zadd(ALERTS_KEY, score, value);

        zaddFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                LOG.error("Failed to write alert to Redis: {}", alert.getAlertId(), throwable);
                resultFuture.completeExceptionally(throwable);
            } else {
                LOG.info("Alert stored in Redis: {} - {} exceeded threshold", 
                        alert.getAlertType(), alert.getTenantId());
                
                // Trim the sorted set to keep only last N alerts
                asyncCommands.zremrangebyrank(ALERTS_KEY, 0, -MAX_ALERTS - 1);
                
                resultFuture.complete(Collections.emptyList());
            }
        });
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
        super.close();
    }
}
