# Event Engine - Flink

Stream processing infrastructure for the Event Engine platform using Apache Flink.

## Overview

This repository contains Flink streaming jobs, configurations, and deployment files for real-time event processing and aggregation in the Event Engine platform.

## Features

- **Real-time Aggregations**: 1-minute and 5-minute windowed aggregations
- **Stateful Processing**: RocksDB state backend for fault tolerance
- **Exactly-Once Semantics**: Guaranteed event processing
- **Scalable**: 2 TaskManagers with 8 slots each (16 parallel tasks)
- **High Throughput**: Optimized for millions of events per second

## Architecture

```
Kafka (usage-events)
    ↓
Flink Job (EventAggregationJob)
    ├── 1-min aggregations → tenant-aggs-1min
    ├── 5-min aggregations → tenant-aggs-5min
    ├── User aggregations → user-aggs-1min
    └── Alerts → usage-alerts
    ↓
ClickHouse (persistence)
```

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Maven 3.9+ (for building)
- Java 11+
- Running Kafka cluster (see event-engine-kafka)
- Running ClickHouse (see event-engine-clickhouse)

### 1. Build the Project

```bash
mvn clean package
```

This creates: `target/flink-streaming-1.0-SNAPSHOT.jar`

### 2. Start Flink Cluster

```bash
cd deployments
docker-compose up -d
```

### 3. Verify Flink is Running

Open Flink Dashboard: http://localhost:8081

Or check health:
```bash
docker logs event-engine-flink-jobmanager
```

### 4. Submit Flink Job

**Using PowerShell**:
```powershell
cd scripts
.\submit_job.ps1
```

**Using Bash**:
```bash
cd scripts
./submit_job.sh
```

## Flink Jobs

### EventAggregationJob

**Purpose**: Real-time event aggregation with multiple time windows

**Input Topics**:
- `usage-events` (32 partitions)

**Output Topics**:
- `tenant-aggs-1min` - 1-minute tenant aggregations
- `tenant-aggs-5min` - 5-minute tenant aggregations
- `user-aggs-1min` - 1-minute user aggregations
- `user-aggs-5min` - 5-minute user aggregations
- `usage-alerts` - Threshold-based alerts

**Configuration**:
- Parallelism: 4
- Checkpointing: Every 60 seconds
- State Backend: RocksDB
- Checkpoint Mode: EXACTLY_ONCE

## Development

### Project Structure

```
event-engine-flink/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/eventengine/flink/
│       │       ├── EventAggregationJob.java
│       │       ├── functions/
│       │       └── models/
│       └── resources/
│           └── log4j.properties
├── deployments/
│   ├── Dockerfile
│   └── docker-compose.yml
├── scripts/
│   ├── submit_job.sh
│   └── submit_job.ps1
├── pom.xml
└── README.md
```

### Building Locally

```bash
# Clean build
mvn clean package

# Skip tests
mvn clean package -DskipTests

# Run tests only
mvn test
```

### Running Locally (without Docker)

```bash
# Start local Flink cluster
./bin/start-cluster.sh

# Submit job
./bin/flink run target/flink-streaming-1.0-SNAPSHOT.jar

# Stop cluster
./bin/stop-cluster.sh
```

## Configuration

### Flink Configuration (docker-compose.yml)

```yaml
jobmanager.memory.process.size: 2048m
taskmanager.memory.process.size: 4096m
taskmanager.numberOfTaskSlots: 8
parallelism.default: 4
state.backend: rocksdb
execution.checkpointing.interval: 60000
execution.checkpointing.mode: EXACTLY_ONCE
```

### Application Configuration

Edit `src/main/resources/application.properties`:

```properties
kafka.bootstrap.servers=kafka:29092
kafka.group.id=flink-aggregator
clickhouse.url=jdbc:clickhouse://clickhouse:8123/event_engine
```

## Monitoring

### Flink Dashboard

Access at: http://localhost:8081

Features:
- Job overview and status
- Task metrics
- Checkpoints and savepoints
- Backpressure monitoring
- Logs and exceptions

### Metrics

Flink exposes metrics on port 9249 (Prometheus format)

```bash
curl http://localhost:9249/metrics
```

### Job Status

```bash
# List all jobs
curl http://localhost:8081/jobs

# Get specific job
curl http://localhost:8081/jobs/<job-id>

# Get job metrics
curl http://localhost:8081/jobs/<job-id>/metrics
```

## Operations

### Restart Job

```bash
# Cancel job
curl -X PATCH http://localhost:8081/jobs/<job-id>?mode=cancel

# Resubmit
./scripts/submit_job.sh
```

### Create Savepoint

```bash
curl -X POST http://localhost:8081/jobs/<job-id>/savepoints \
  -H "Content-Type: application/json" \
  -d '{"target-directory": "/tmp/flink-savepoints"}'
```

### Restore from Savepoint

```bash
flink run -s /tmp/flink-savepoints/savepoint-xxx \
  target/flink-streaming-1.0-SNAPSHOT.jar
```

### Scale TaskManagers

Edit `docker-compose.yml` and add more taskmanagers:

```yaml
flink-taskmanager-3:
  # ... same config as taskmanager-1 ...
```

Then:
```bash
docker-compose up -d --scale flink-taskmanager-3=1
```

## Performance Tuning

### Memory

- **JobManager**: 2GB (metadata, coordination)
- **TaskManager**: 4GB each (data processing)
- Adjust based on state size and throughput

### Parallelism

```java
// Set in code
env.setParallelism(8);

// Or per operator
stream.map(...).setParallelism(4);
```

### State Backend

RocksDB is used for large state:
- Disk-based, can handle TB of state
- Incremental checkpoints
- Slower than heap, but more scalable

### Checkpointing

- **Interval**: 60 seconds (adjust based on recovery time vs overhead)
- **Mode**: EXACTLY_ONCE (or AT_LEAST_ONCE for higher performance)
- **Timeout**: 10 minutes

## Troubleshooting

### Job Won't Start

Check logs:
```bash
docker logs event-engine-flink-jobmanager
docker logs event-engine-flink-taskmanager-1
```

### Out of Memory

Increase TaskManager memory in docker-compose.yml:
```yaml
taskmanager.memory.process.size: 8192m
```

### Backpressure

Check Flink Dashboard → Backpressure tab

Solutions:
- Increase parallelism
- Add more TaskManagers
- Optimize operators
- Increase Kafka partitions

### Checkpoint Failures

Check checkpoint directory permissions:
```bash
docker exec event-engine-flink-jobmanager ls -la /tmp/flink-checkpoints
```

### Kafka Connection Issues

Verify Kafka is accessible:
```bash
docker exec event-engine-flink-jobmanager \
  curl kafka:29092
```

## Integration

This repository depends on:
- **event-engine-kafka**: Input and output topics
- **event-engine-clickhouse**: Sink for aggregated data

Used by:
- **event-engine-core**: Reads aggregation results
- Analytics dashboards

## Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

```bash
# Start test environment
docker-compose -f docker-compose.test.yml up -d

# Run tests
mvn verify

# Cleanup
docker-compose -f docker-compose.test.yml down
```

### Local Testing

Use Flink's MiniCluster for local testing:

```java
StreamExecutionEnvironment env = 
    StreamExecutionEnvironment.createLocalEnvironment();
```

## Production Considerations

1. **High Availability**: Deploy JobManager in HA mode with ZooKeeper
2. **Scaling**: Add more TaskManagers based on load
3. **Monitoring**: Integrate with Prometheus + Grafana
4. **Alerting**: Set up alerts for job failures and backpressure
5. **State Management**: Regular savepoints for version upgrades
6. **Resource Limits**: Set appropriate Kubernetes resource limits

## Dependencies

Key dependencies (from pom.xml):
- Flink 1.18.0
- Kafka Connector
- ClickHouse JDBC
- RocksDB State Backend

## License

Proprietary - Pente AI
