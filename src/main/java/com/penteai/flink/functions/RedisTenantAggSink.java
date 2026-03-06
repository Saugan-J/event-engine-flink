package com.penteai.flink.functions;

import com.google.gson.Gson;
import com.penteai.flink.models.TenantAggregation;
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
 * Async sink for writing tenant aggregations to Redis for low-latency dashboard queries
 */
public class RedisTenantAggSink extends RichAsyncFunction<TenantAggregation, Void> {
    private static final Logger LOG = LoggerFactory.getLogger(RedisTenantAggSink.class);
    private static final long serialVersionUID = 1L;

    private final String redisHost;
    private final int redisPort;
    private final int ttlSeconds;
    private final String windowType; // "1min" or "5min"

    private transient RedisClient redisClient;
    private transient StatefulRedisConnection<String, String> connection;
    private transient RedisAsyncCommands<String, String> asyncCommands;
    private transient Gson gson;

    public RedisTenantAggSink(String redisHost, int redisPort, int ttlSeconds, String windowType) {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.ttlSeconds = ttlSeconds;
        this.windowType = windowType;
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
    public void asyncInvoke(TenantAggregation agg, ResultFuture<Void> resultFuture) {
        String key = String.format("tenant:%s:agg:%s:latest", agg.getTenantId(), windowType);
        String value = gson.toJson(agg);

        RedisFuture<String> setFuture = asyncCommands.setex(key, ttlSeconds, value);

        setFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                LOG.error("Failed to write to Redis for tenant: {}", agg.getTenantId(), throwable);
                resultFuture.completeExceptionally(throwable);
            } else {
                LOG.debug("Successfully wrote to Redis: {}", key);
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
