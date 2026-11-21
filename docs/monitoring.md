# Monitoring & Observability

## Overview

The service provides comprehensive observability through structured logging, metrics collection, health checks, and distributed tracing support.

## Structured Logging

### Log Format: ECS (Elastic Common Schema)

Logs are output in ECS JSON format for integration with centralized logging systems (ELK, CloudWatch Logs).

**Example log output**:

```json
{
  "timestamp": "2024-11-21T13:16:51.123Z",
  "level": "INFO",
  "logger": "uk.gov.defra.trade.imports.fish.be.service.ExampleService",
  "trace.id": "550e8400-e29b-41d4-a716-446655440000",
  "http.request.method": "POST",
  "url.full": "http://localhost:8085/example",
  "http.response.status_code": "201",
  "message": "Created example with id: 507f1f77bcf86cd799439011"
}
```

### Log Levels

| Level   | Use When                                | Examples                                              |
| ------- | --------------------------------------- | ----------------------------------------------------- |
| `DEBUG` | Diagnostic info for developers          | Method entry/exit, variable values, query details     |
| `INFO`  | Business events                         | Resource created/updated/deleted, service started     |
| `WARN`  | Recoverable errors, expected edge cases | Resource not found, duplicate key, validation failure |
| `ERROR` | Unexpected failures                     | Uncaught exceptions, database connection lost         |

**Configuration**:

```yaml
logging:
  level:
    root: ${LOG_LEVEL:info}
    uk.gov.defra.trade.imports.fish.be: ${LOG_LEVEL:DEBUG}
    org.springframework: ${LOGGING_LEVEL_ORG_SPRINGFRAMEWORK:INFO}
    org.mongodb.driver: ${LOGGING_LEVEL_ORG_MONGODB_DRIVER:WARN}
```

Override via environment variables:

```bash
LOG_LEVEL=DEBUG mvn spring-boot:run
LOGGING_LEVEL_ORG_MONGODB_DRIVER=DEBUG docker compose up
```

### MDC (Mapped Diagnostic Context)

MDC automatically includes trace ID and request metadata in all logs (see [Filters & Middleware](filters.md)):

| Field                       | Value                            |
| --------------------------- | -------------------------------- |
| `trace.id`                  | Request trace ID for correlation |
| `http.request.method`       | HTTP method                      |
| `url.full`                  | Full request URL                 |
| `http.response.status_code` | Response status                  |

**Usage in queries**:

```bash
# CloudWatch Logs Insights
fields @timestamp, @message, trace.id
| filter trace.id = "550e8400-e29b-41d4-a716-446655440000"
| sort @timestamp desc

# ELK / Kibana
trace.id: "550e8400-e29b-41d4-a716-446655440000"
```

---

## Metrics

### Collection & Export

Metrics are collected via Micrometer and exported to CloudWatch using EMF (Embedded Metric Format).

**Configuration**:

```yaml
aws:
  emf:
    enabled: ${AWS_EMF_ENABLED:true}
    environment: ${AWS_EMF_ENVIRONMENT:Local}
    namespace: ${AWS_EMF_NAMESPACE:trade-imports-fish-be}
    service:
      name: ${AWS_EMF_SERVICE_NAME:trade-imports-fish-be}
      type: ${AWS_EMF_SERVICE_TYPE:SpringBootApp}
```

### Standard Metrics

#### JVM Metrics

| Metric                 | Description              |
| ---------------------- | ------------------------ |
| `jvm.memory.committed` | Committed memory (bytes) |
| `jvm.memory.used`      | Used memory (bytes)      |
| `jvm.memory.max`       | Maximum memory (bytes)   |
| `jvm.threads.live`     | Active threads           |
| `jvm.threads.peak`     | Peak thread count        |

#### HTTP Metrics

| Metric                                   | Description                                  |
| ---------------------------------------- | -------------------------------------------- |
| `http.server.requests`                   | Active HTTP requests (gauge)                 |
| `http.server.requests`                   | Request latency (histogram)                  |
| Response status code breakdown (counter) | Requests by status (200, 201, 404, 409, 500) |

#### Controller Metrics

Auto-instrumented via `@Timed` and `@Counted` annotations:

| Metric                                   | Method               | Type    |
| ---------------------------------------- | -------------------- | ------- |
| `controller.postExampleEntity.time`      | POST /example        | Timer   |
| `controller.getExampleEntity.time`       | GET /example/{id}    | Timer   |
| `controller.getAllExampleEntities.time`  | GET /example         | Timer   |
| `controller.getAllExampleEntities.count` | GET /example         | Counter |
| `controller.putExampleEntity.time`       | PUT /example/{id}    | Timer   |
| `controller.deleteExampleEntity.time`    | DELETE /example/{id} | Timer   |

### Business Metrics

Custom counters published by services:

| Metric            | Description                  |
| ----------------- | ---------------------------- |
| `example_created` | Successful example creations |
| `example_updated` | Successful example updates   |
| `example_deleted` | Successful example deletions |

**Publish in code**:

```java
meterRegistry.counter("example_created").increment();
```

### Accessing Metrics

**Via actuator endpoint**:

```bash
# List all metrics
curl http://localhost:8085/metrics

# Get specific metric
curl http://localhost:8085/metrics/jvm.memory.used

# Response
{
  "name": "jvm.memory.used",
  "description": "The amount of used memory",
  "baseUnit": "bytes",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 268435456
    }
  ]
}
```

**Via CloudWatch** (production):

- Navigate to CloudWatch console
- Select namespace: `trade-imports-fish-be` (configurable)
- View metrics dashboard

---

## Health Checks

### Liveness Probe

Indicates whether the application is running and responsive.

**Endpoint**: `GET /health`

**Success Response** (200 OK):

```json
{
  "status": "UP",
  "components": {
    "diskSpace": { "status": "UP" },
    "livenessState": { "status": "UP" },
    "readinessState": { "status": "UP" }
  }
}
```

**Failure Response** (503 Service Unavailable):

```json
{
  "status": "DOWN",
  "components": {
    "diskSpace": {
      "status": "DOWN",
      "details": { "status": 1, "threshold": 1048576 }
    }
  }
}
```

**Kubernetes usage**:

```yaml
livenessProbe:
  httpGet:
    path: /health
    port: 8085
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

### Readiness Probe

Indicates whether the application is ready to accept traffic.

**Endpoint**: Same as liveness (`GET /health`), `readinessState` component

**Configuration**:

```yaml
management:
  health:
    mongo:
      enabled: false # Don't block readiness on MongoDB
```

MongoDB health check disabled by default to allow graceful degradation. If MongoDB is critical, enable and ensure it's always healthy during deployment.

### Custom Health Checks

Extend via `@Component` implementing `HealthIndicator`:

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        boolean isHealthy = checkSomething();
        if (isHealthy) {
            return Health.up().build();
        }
        return Health.down().withDetail("reason", "...").build();
    }
}
```

---

## Service Info Endpoint

**Endpoint**: `GET /info`

**Response**:

```json
{
  "app": {
    "name": "trade-imports-fish-be",
    "version": "1.2.3",
    "environment": "production"
  }
}
```

**Configuration**:

```yaml
info:
  app:
    name: ${spring.application.name}
    version: ${cdp.service-version}
    environment: ${ENVIRONMENT:local}
```

---

## Distributed Tracing

### Trace ID Propagation

All requests automatically include a trace ID for correlation across logs and metrics.

**Flow**:

1. Client sends `x-cdp-request-id` header
2. `RequestTracingFilter` extracts and stores in MDC
3. All logs include `trace.id` field
4. Error responses include `traceId` property
5. Metrics tagged with `trace_id` dimension

**Manual trace ID generation** (if not provided by upstream):

```bash
TRACE_ID=$(uuidgen)
curl http://localhost:8085/example \
  -H "x-cdp-request-id: $TRACE_ID"
```

### Log Correlation Example

Request with trace ID:

```bash
curl -H "x-cdp-request-id: abc123" http://localhost:8085/example
```

Logs include:

```json
{"timestamp": "2024-11-21T13:16:51Z", "trace.id": "abc123", "message": "GET /example"}
{"timestamp": "2024-11-21T13:16:51Z", "trace.id": "abc123", "message": "Found 5 examples"}
{"timestamp": "2024-11-21T13:16:51Z", "trace.id": "abc123", "message": "Response 200"}
```

---

## Monitoring Dashboard (CloudWatch)

### Setup

1. Enable EMF in production:

   ```bash
   AWS_EMF_ENABLED=true
   AWS_EMF_AGENT_ENDPOINT=http://cloudwatch-agent:3000
   ```

2. Create CloudWatch dashboard:
   ```bash
   aws cloudwatch put-dashboard \
     --dashboard-name trade-imports-fish-be \
     --dashboard-body '{...}'  # JSON dashboard config
   ```

### Recommended Metrics to Monitor

| Metric                               | Threshold   | Action                                          |
| ------------------------------------ | ----------- | ----------------------------------------------- |
| `example_created`                    | Sudden drop | Check error logs, database connectivity         |
| `jvm.memory.used`                    | > 80% max   | Increase heap size, check for memory leaks      |
| `jvm.threads.live`                   | > 100       | Check for thread leaks, increase pool size      |
| `http.server.requests` (latency p99) | > 1s        | Check database performance, review slow queries |
| `http.server.requests` (5xx errors)  | > 0         | Investigate error logs with trace ID            |

### Alerting

Setup CloudWatch alarms:

```bash
aws cloudwatch put-metric-alarm \
  --alarm-name "trade-imports-fish-be-errors" \
  --metric-name "http.server.requests" \
  --statistic Sum \
  --period 300 \
  --threshold 10 \
  --comparison-operator GreaterThanThreshold \
  --alarm-actions arn:aws:sns:eu-west-2:123456789:alerts
```

---

## Performance Considerations

### Logging Performance

- Structured JSON logging has minimal overhead
- MDC operations are thread-safe (InheritableThreadLocal)
- Avoid logging large objects in tight loops

### Metrics Performance

- EMF publishing is asynchronous
- No blocking on metric collection
- Pre-computed metrics (not calculated on-demand)

### Health Check Performance

- Simple status checks run in < 10ms
- MongoDB health check disabled to avoid probe timeouts
- Customize threshold via `management.endpoint.health.probes.enabled=true`

---

## Troubleshooting

**Missing metrics in CloudWatch**:

- Verify `AWS_EMF_ENABLED=true`
- Check AWS credentials and permissions
- Confirm `AWS_REGION` matches CloudWatch region
- For local dev: Floci running at `AWS_EMF_AGENT_ENDPOINT`

**Health check timeout**:

- Reduce custom health indicators' execution time
- Disable slow checks (`management.health.mongo.enabled=false`)
- Increase probe timeout in Kubernetes

**Trace ID missing from logs**:

- Verify `RequestTracingFilter` is registered
- Check logback format includes `%X{trace.id}`
- Confirm client sends `x-cdp-request-id` header

**High memory usage**:

- Check `jvm.memory.used` metric
- Review for memory leaks (long-lived collections)
- Increase heap: `JAVA_OPTS=-Xmx1024m`
