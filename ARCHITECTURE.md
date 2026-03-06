# Flink Streaming Architecture

## System Overview

The Flink streaming job processes usage events in real-time to provide immediate insights and alerting alongside the existing batch Analytics Workers.

## Architecture Diagram

```
                    ┌─────────────────────────────────────┐
                    │      Billing SDK (Python/Go)       │
                    └──────────────┬──────────────────────┘
                                   │ HTTP POST
                                   ▼
                    ┌─────────────────────────────────────┐
                    │         Ingest API (Go)             │
                    └──────────────┬──────────────────────┘
                                   │ Publish
                                   ▼
                    ┌─────────────────────────────────────┐
                    │  Kafka: usage-events (32 parts)    │
                    └───┬──────────────────────────────┬──┘
                        │                              │
          ┌─────────────┴──────────────┐               │
          │ Existing Analytics Workers │               │
          │ (4x batch processors)      │               │
          └─────────────┬──────────────┘               │
                        │ Bulk Insert                  │
                        ▼                              │
          ┌─────────────────────────────┐              │
          │       ClickHouse            │              │
          │  (Historical Analytics)     │              │
          └─────────────────────────────┘              │
                                                       │
                        ┌──────────────────────────────┘
                        │ Flink Consumer Group
                        │ (flink-realtime-aggs)
                        ▼
          ┌─────────────────────────────────────────────┐
          │                                             │
          │     Apache Flink Streaming Job              │
          │                                             │
          │  ┌──────────────────────────────────────┐   │
          │  │  Source: Kafka Consumer              │   │
          │  │  - Event-time processing             │   │
          │  │  - Watermark: 30s out-of-order       │   │
          │  │  - Parallelism: 32                   │   │
          │  └─────────────┬────────────────────────┘   │
          │                │                            │
          │  ┌─────────────┴────────────────────────┐   │
          │  │  Windowed Aggregations               │   │
          │  │  ┌─────────────────────────────────┐ │   │
          │  │  │ Tenant 1-min Tumbling Windows   │ │   │
          │  │  │ - SUM(amount), COUNT(*)         │ │   │
          │  │  │ - COUNT(DISTINCT user_id)       │ │   │
          │  │  └─────────────────────────────────┘ │   │
          │  │  ┌─────────────────────────────────┐ │   │
          │  │  │ Tenant 5-min Tumbling Windows   │ │   │
          │  │  └─────────────────────────────────┘ │   │
          │  │  ┌─────────────────────────────────┐ │   │
          │  │  │ User 1-min Tumbling Windows     │ │   │
          │  │  │ - Per tenant+user aggregation   │ │   │
          │  │  └─────────────────────────────────┘ │   │
          │  │  ┌─────────────────────────────────┐ │   │
          │  │  │ User 5-min Tumbling Windows     │ │   │
          │  │  └─────────────────────────────────┘ │   │
          │  │  - Parallelism: 16                  │   │
          │  └─────────────┬────────────────────────┘   │
          │                │                            │
          │  ┌─────────────┴────────────────────────┐   │
          │  │  Alert Detection (SQL)               │   │
          │  │  - Tenant threshold: 100K units/min  │   │
          │  │  - User threshold: 10K units/min     │   │
          │  │  - Severity: WARNING / CRITICAL      │   │
          │  └─────────────┬────────────────────────┘   │
          │                │                            │
          │  ┌─────────────┴────────────────────────┐   │
          │  │  Dual Sinks (Parallelism: 8)        │   │
          │  │  ┌──────────────┐  ┌──────────────┐ │   │
          │  │  │ Kafka Sink   │  │ Redis Sink   │ │   │
          │  │  │ (Durable)    │  │ (Fast Query) │ │   │
          │  │  └──────────────┘  └──────────────┘ │   │
          │  └──────────────────────────────────────┘   │
          │                                             │
          │  State: RocksDB (Incremental Checkpoints)  │
          │  Checkpointing: 60s, EXACTLY_ONCE          │
          │                                             │
          └──────────┬────────────────────┬─────────────┘
                     │                    │
        ┌────────────┴──────────┐  ┌─────┴──────────────┐
        │                       │  │                    │
        ▼                       ▼  ▼                    ▼
┌────────────────┐    ┌──────────────────────┐  ┌──────────────┐
│ Kafka Topics   │    │       Redis          │  │  WebSocket   │
│                │    │                      │  │   Server     │
│ • tenant-aggs- │    │ • tenant:*:agg:*     │  │              │
│   1min/5min    │    │ • user:*:agg:*       │  │ (Real-time   │
│ • user-aggs-   │    │ • alerts:recent      │  │  Dashboard)  │
│   1min/5min    │    │   (TTL: 10 min)      │  │              │
│ • usage-alerts │    │                      │  │              │
│                │    │                      │  │              │
│ (24h - 7d)     │    │                      │  │              │
└────────────────┘    └──────────────────────┘  └──────────────┘
```

## Components

### 1. Source: Kafka Consumer

- **Topic**: `usage-events` (32 partitions)
- **Consumer Group**: `flink-realtime-aggs` (separate from batch workers)
- **Format**: JSON
- **Parallelism**: 32 (matches partitions)
- **Event-Time**: Uses `timestamp_ms` field
- **Watermark**: 30 seconds out-of-order tolerance

### 2. Windowed Aggregations

#### Tenant-Level Aggregations

**1-Minute Windows:**
- Tumbling event-time windows
- Key: `org_id` (tenant)
- Aggregations:
  - `SUM(amount)` → total_usage
  - `COUNT(*)` → event_count
  - `COUNT(DISTINCT user_id)` → unique_users
  - `MAX(event_time)` → max_timestamp

**5-Minute Windows:**
- Same structure as 1-minute
- Longer aggregation period for trend analysis

#### User-Level Aggregations

**1-Minute Windows:**
- Key: `(org_id, user_id)`
- Aggregations:
  - `SUM(amount)` → total_usage
  - `COUNT(*)` → event_count

**5-Minute Windows:**
- Same structure as 1-minute

### 3. Alert Detection

Implemented using Flink SQL WHERE clauses on aggregated results:

**Tenant Alerts:**
```sql
WHERE total_usage > 100000  -- Configurable threshold
```

**User Alerts:**
```sql
WHERE total_usage > 10000   -- Per-user threshold
```

**Severity Levels:**
- `WARNING`: Threshold exceeded
- `CRITICAL`: 2x threshold exceeded

### 4. Dual Output Sinks

#### Kafka Sinks (Upsert)

- **Topics**: 
  - `tenant-aggs-1min` (16 partitions, compacted, 24h retention)
  - `tenant-aggs-5min` (16 partitions, compacted, 24h retention)
  - `user-aggs-1min` (32 partitions, compacted, 24h retention)
  - `user-aggs-5min` (32 partitions, compacted, 24h retention)
  - `usage-alerts` (8 partitions, 7d retention)
- **Delivery Guarantee**: Exactly-once
- **Format**: JSON

#### Redis Sinks (Async)

- **Purpose**: Low-latency dashboard queries
- **TTL**: 10 minutes
- **Key Patterns**:
  - `tenant:{tenant_id}:agg:1min:latest`
  - `tenant:{tenant_id}:agg:5min:latest`
  - `user:{tenant_id}:{user_id}:agg:1min:latest`
  - `user:{tenant_id}:{user_id}:agg:5min:latest`
  - `alerts:recent` (sorted set by timestamp)

### 5. State Management

- **Backend**: RocksDB (local disk-based)
- **Checkpointing**: 
  - Interval: 60 seconds
  - Mode: EXACTLY_ONCE
  - Incremental: Yes
  - Unaligned: Yes (for low latency)
- **State TTL**: 24 hours
- **Storage**: File system (configurable to S3 for production)

## Data Flow

1. **Event Ingestion**
   - Events arrive via Kafka in JSON format
   - Watermark strategy handles late data (30s tolerance)
   - Idle sources detected after 60s

2. **Keyed Streams**
   - Tenant stream: Keyed by `org_id`
   - User stream: Keyed by `(org_id, user_id)`

3. **Window Assignment**
   - Tumbling event-time windows
   - 1-minute and 5-minute durations
   - Window completion triggers aggregation

4. **Aggregation**
   - In-memory state accumulates events
   - Local aggregation before shuffle
   - Two-phase aggregation for efficiency

5. **Alert Check**
   - SQL WHERE clause filters exceeded thresholds
   - Alert objects generated with severity level

6. **Dual Write**
   - Kafka: Durable, replayable history
   - Redis: Fast access for live dashboards

## Performance Characteristics

### Throughput

- **Design Target**: 1M events/second burst
- **Steady State**: 100K-500K events/second
- **Per Partition**: ~30K events/second

### Latency

- **Window to Output**: 1-5 seconds (p99)
- **End-to-End**: < 10 seconds (ingestion to Redis)
- **Alert Latency**: < 5 seconds after threshold breach

### State Size

- **Per Tenant**: ~1KB/minute window
- **Per User**: ~500 bytes/minute window
- **Total State (10K tenants, 100K users)**: ~50GB

### Checkpoint Duration

- **Target**: < 30 seconds
- **Typical**: 10-20 seconds
- **Incremental**: Only changed state

## Scalability

### Horizontal Scaling

- **TaskManagers**: Add more Docker containers
- **Task Slots**: 8 per TaskManager
- **Total Parallelism**: slots × task_managers

### Vertical Scaling

- **Memory**: 4-8GB per TaskManager
- **CPU**: 2-4 cores per TaskManager
- **Disk**: 50GB+ for RocksDB state

### Kafka Partitions

- Source topic: 32 partitions → max 32 parallel consumers
- Sink topics: 8-32 partitions based on write load

## Fault Tolerance

### Failures Handled

1. **TaskManager Failure**
   - Job restarts from last checkpoint
   - Exactly-once guarantee maintained
   - Automatic recovery

2. **Network Partition**
   - Kafka consumer rebalance
   - State recovery from checkpoint
   - No data loss

3. **Kafka Downtime**
   - Job pauses, retries connection
   - Backpressure to source
   - Resume on Kafka recovery

4. **Redis Downtime**
   - Async sink buffers writes
   - Non-critical path, job continues
   - Kafka output still succeeds

### Guarantees

- **Exactly-Once**: Kafka sink using transactions
- **At-Least-Once**: Redis sink (idempotent writes)
- **No Data Loss**: Checkpointing + Kafka durability

## Monitoring

### Key Metrics

1. **Throughput**
   - Records in/out per operator
   - Bytes in/out per second

2. **Latency**
   - Event-time lag
   - Window completion time
   - Checkpoint duration

3. **Resource Usage**
   - CPU per TaskManager
   - Heap/off-heap memory
   - Network buffers

4. **Backpressure**
   - Busy/idle time ratio
   - Buffer utilization

5. **Checkpoints**
   - Success rate
   - Duration trend
   - State size growth

### Dashboards

- **Flink Web UI**: http://localhost:8081
- **Prometheus**: http://localhost:9249/metrics
- **Grafana**: Import dashboard #10369

## Comparison with Batch Workers

| Aspect | Batch Workers | Flink Streaming |
|--------|--------------|-----------------|
| **Latency** | 1-5 minutes | 1-5 seconds |
| **Purpose** | Bulk load to ClickHouse | Real-time aggregation |
| **Output** | ClickHouse tables | Kafka + Redis |
| **State** | Stateless | Stateful (RocksDB) |
| **Windowing** | Manual batch | Native event-time |
| **Guarantees** | At-least-once | Exactly-once |
| **Use Case** | Historical analytics | Live dashboards, alerts |

## Future Enhancements

1. **Sliding Windows**: Add for moving averages
2. **Session Windows**: Detect usage patterns per session
3. **Complex Event Processing**: Multi-step alert conditions
4. **ML Integration**: Anomaly detection with Flink ML
5. **Apache Pinot**: OLAP engine for complex dashboard queries
6. **Confluent Cloud**: Managed Flink for production
