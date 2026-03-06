package com.penteai.flink;

import com.penteai.flink.config.JobConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.StatementSet;
import org.apache.flink.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class FlinkStreamingJob {
    private static final Logger LOG = LoggerFactory.getLogger(FlinkStreamingJob.class);

    public static void main(String[] args) throws Exception {
        // Load configuration
        JobConfig config = new JobConfig();
        LOG.info("Starting Flink Streaming Job with configuration:");
        LOG.info("  Kafka Brokers: {}", config.getKafkaBrokers());
        LOG.info("  Source Topic: {}", config.getSourceTopic());
        LOG.info("  Consumer Group: {}", config.getConsumerGroup());

        // Create execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // Set parallelism to 1 to avoid idle-partition watermark blocking.
        // With 32 partitions but events on only a few, higher parallelism causes
        // subtasks with empty partitions to block the overall watermark.
        env.setParallelism(1);
        
        // Configure checkpointing
        env.enableCheckpointing(config.getCheckpointIntervalMs());
        env.getCheckpointConfig().setCheckpointTimeout(config.getCheckpointTimeoutMs());
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(config.getCheckpointMinPauseMs());
        
        LOG.info("Checkpointing enabled with interval: {}ms", config.getCheckpointIntervalMs());

        // Create Table Environment
        EnvironmentSettings settings = EnvironmentSettings.newInstance()
                .inStreamingMode()
                .build();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);

        // Configure idle source timeout so that idle Kafka partitions don't block watermark
        // This is CRITICAL: without this, partitions with no data prevent windows from firing
        Configuration tableConfig = tableEnv.getConfig().getConfiguration();
        tableConfig.setString("table.exec.source.idle-timeout", "5000 ms");
        LOG.info("Idle source timeout set to 5000 ms, parallelism set to 1");

        // Execute SQL DDL statements - Create all tables first
        LOG.info("Creating source table...");
        String createSourceSql = readResourceFile("sql/create_source_table.sql");
        tableEnv.executeSql(createSourceSql).await();
        LOG.info("Source table CREATE completed");
        
        // Verify table was created
        String[] tables = tableEnv.listTables();
        LOG.info("Tables registered in catalog: {}", String.join(", ", tables));

        LOG.info("Creating sink tables...");
        tableEnv.executeSql(readResourceFile("sql/create_tenant_aggs_1min_sink.sql")).await();
        tableEnv.executeSql(readResourceFile("sql/create_tenant_aggs_5min_sink.sql")).await();
        tableEnv.executeSql(readResourceFile("sql/create_user_aggs_1min_sink.sql")).await();
        tableEnv.executeSql(readResourceFile("sql/create_user_aggs_5min_sink.sql")).await();
        tableEnv.executeSql(readResourceFile("sql/create_alerts_sink.sql")).await();
        LOG.info("All sink tables created");
        
        // Verify all tables
        tables = tableEnv.listTables();
        LOG.info("All tables in catalog: {}", String.join(", ", tables));

        LOG.info("Starting aggregation queries...");
        // Execute INSERT statements which run continuously as streaming jobs
        // Running only 3 jobs to fit in cluster resources (16 slots available)
        // 3 jobs × 2 parallelism × 2 operators = 12 slots needed
        String insertSql1 = readResourceFile("sql/insert_tenant_aggs_1min.sql");
        String insertSql3 = readResourceFile("sql/insert_user_aggs_1min.sql");
        String insertSql5 = readResourceFile("sql/insert_alerts.sql");
        
        tableEnv.executeSql(insertSql1);
        tableEnv.executeSql(insertSql3);
        
        // Execute the last INSERT and await (keeps the job running)
        LOG.info("Executing final INSERT query (will keep job running)...");
        tableEnv.executeSql(insertSql5).await();
        
        LOG.info("Flink real-time streaming analytics job completed");
    }

    private static void executeSqlFile(StreamTableEnvironment tableEnv, String resourcePath) {
        try {
            String sql = readResourceFile(resourcePath);
            
            // Split by semicolon and execute each statement
            String[] statements = sql.split(";");
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    LOG.debug("Executing SQL: {}", trimmed);
                    // For CREATE statements, wait for completion to ensure catalog registration
                    if (trimmed.toUpperCase().startsWith("CREATE")) {
                        tableEnv.executeSql(trimmed).await();
                        LOG.debug("CREATE statement completed: {}", resourcePath);
                    } else {
                        tableEnv.executeSql(trimmed);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error executing SQL file: {}", resourcePath, e);
            throw new RuntimeException("Failed to execute SQL file: " + resourcePath, e);
        }
    }

    private static String readResourceFile(String resourcePath) throws Exception {
        try (InputStream is = FlinkStreamingJob.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}
