# Multi-stage build for Flink
FROM maven:3.9-eclipse-temurin-11 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage - Slim Flink image
FROM flink:1.18.0-scala_2.12-java11

# Copy the built JAR
COPY --from=builder /app/target/flink-streaming-*.jar /opt/flink/usrlib/

# Install additional dependencies
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# Expose ports
EXPOSE 8081 9249

# Health check
HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
  CMD curl -f http://localhost:8081 || exit 1

# Flink will start with command from docker-compose
