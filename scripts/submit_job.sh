#!/bin/bash

# Submit Flink Job
# Usage: ./submit_job.sh [jar-file] [main-class]

set -e

JAR_FILE="${1:-target/flink-streaming-1.0-SNAPSHOT.jar}"
MAIN_CLASS="${2:-com.eventengine.flink.EventAggregationJob}"
FLINK_HOST="${FLINK_HOST:-localhost}"
FLINK_PORT="${FLINK_PORT:-8081}"

echo "======================================"
echo "Submitting Flink Job"
echo "======================================"
echo "JAR: $JAR_FILE"
echo "Main Class: $MAIN_CLASS"
echo "Flink: $FLINK_HOST:$FLINK_PORT"
echo "======================================"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found: $JAR_FILE"
    echo "Please build the project first: mvn clean package"
    exit 1
fi

# Wait for Flink to be ready
echo "Waiting for Flink to be ready..."
for i in {1..30}; do
    if curl -s "http://$FLINK_HOST:$FLINK_PORT" > /dev/null; then
        echo "✓ Flink is ready"
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 2
done

# Submit the job
echo "Submitting job..."
curl -X POST "http://$FLINK_HOST:$FLINK_PORT/jars/upload" \
    -F "jarfile=@$JAR_FILE" \
    > /tmp/upload_response.json

JAR_ID=$(cat /tmp/upload_response.json | grep -oP '(?<="filename":")[^"]+')

if [ -z "$JAR_ID" ]; then
    echo "✗ Failed to upload JAR"
    cat /tmp/upload_response.json
    exit 1
fi

echo "✓ JAR uploaded: $JAR_ID"

# Run the job
echo "Running job..."
curl -X POST "http://$FLINK_HOST:$FLINK_PORT/jars/$JAR_ID/run" \
    -H "Content-Type: application/json" \
    -d "{\"entryClass\":\"$MAIN_CLASS\"}" \
    > /tmp/run_response.json

JOB_ID=$(cat /tmp/run_response.json | grep -oP '(?<="jobid":")[^"]+')

if [ -z "$JOB_ID" ]; then
    echo "✗ Failed to run job"
    cat /tmp/run_response.json
    exit 1
fi

echo "✓ Job submitted successfully!"
echo "Job ID: $JOB_ID"
echo "Dashboard: http://$FLINK_HOST:$FLINK_PORT/#/job/$JOB_ID/overview"
echo "======================================"
