# Logging and Observability

This guide covers logging configuration, monitoring, and observability in the Trade Imports Fish Backend.

## Logging Overview

The application uses industry-standard logging and observability tools:

- **Framework**: SLF4J with Logback
- **Format**: Elastic Common Schema (ECS)
- **Correlation**: Request tracing with MDC (Mapped Diagnostic Context)
- **Metrics**: Micrometer with CloudWatch EMF integration

## ECS (Elastic Common Schema) Format

Logs are automatically formatted in ECS format for structured logging and analysis.

### Example ECS Log Entry

```json
{
  "@timestamp": "2024-01-01T10:30:00.000Z",
  "log.level": "INFO",
  "message": "Creating example with name: Example1",
  "logger.name": "uk.gov.defra.trade.imports.fish.be.service.ExampleService",
  "trace.id": "abc123def456",
  "http.request.method": "POST",
  "http.response.status_code": 201,
  "url.full": "http://localhost:8085/example",
  "service.name": "trade-imports-fish-be",
  "service.version": "0.1.0"
}
```

## Logging Configuration

### Setting Log Levels

Via `application.yml`:

```yaml
logging:
  level:
    root: INFO
    uk.gov.defra.trade: DEBUG
    org.springframework.web: INFO
```

Via environment variable:

```bash
export LOG_LEVEL=DEBUG
```

### Available Log Levels

| Level | Purpose | Use When |
| ------- | --------- | ---------- |
| `TRACE` | Very detailed | Extreme debugging |
| `DEBUG` | Detailed diagnostic | Development, troubleshooting |
| `INFO` | General information | Normal operation, key events |
| `WARN` | Warning conditions | Potential issues |
| `ERROR` | Error events | Failures and exceptions |
| `OFF` | Logging disabled | Performance tuning |

## Request Tracing

All requests are automatically traced for debugging and monitoring.

### Trace ID Header

Requests include a trace ID header:

```bash
X-CDP-Request-ID: abc123def456
```

This ID is:

- Extracted from request headers
- Added to MDC for automatic inclusion in logs
- Propagated to downstream services
- Included in metrics

### Using Trace IDs

Send a custom trace ID:

```bash
curl -H "X-CDP-Request-ID: my-custom-id" \
  http://localhost:8085/health
```

Retrieve logs by trace ID:

```bash
# If using Elasticsearch
curl -X GET "localhost:9200/logs-*/_search" \
  -H "Content-Type: application/json" \
  -d '{"query": {"match": {"trace.id": "my-custom-id"}}}'
```

## Metrics

The application publishes metrics via Micrometer to CloudWatch.

### Built-in Metrics

| Metric | Description |
| -------- | ------------- |
| `http.server.requests` | HTTP request metrics (count, timing, errors) |
| `jvm.memory.used` | JVM memory currently used |
| `jvm.threads.peak` | Peak thread count |
| `gc.*` | Garbage collection metrics |

### Custom Metrics

The application publishes custom business metrics:

| Metric | Description |
| -------- | ------------- |
| `example_created` | Counter for created examples |
| `controller.*.time` | Timing metrics for controller methods |
| `controller.*.count` | Request count metrics |

### Accessing Metrics

Via Spring Boot Actuator:

```bash
# List all metrics
curl http://localhost:8085/metrics

# Get specific metric
curl http://localhost:8085/metrics/example_created
```

Response example:

```json
{
  "name": "example_created",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 42
    }
  ]
}
```

## CloudWatch Integration

Metrics are automatically published to AWS CloudWatch via EMF (Embedded Metric Format).

### Enabling CloudWatch Metrics

```bash
export AWS_EMF_ENABLED=true
export AWS_EMF_NAMESPACE=trade-imports-fish-be
export AWS_EMF_ENVIRONMENT=production
export AWS_REGION=eu-west-2
```

### CloudWatch EMF Configuration

| Variable | Default | Purpose |
| ---------- | --------- | --------- |
| `AWS_EMF_ENABLED` | `true` | Enable EMF metrics |
| `AWS_EMF_NAMESPACE` | `trade-imports-fish-be` | CloudWatch namespace |
| `AWS_EMF_ENVIRONMENT` | `Local` | Environment name |
| `AWS_EMF_SERVICE_NAME` | `trade-imports-fish-be` | Service name in metrics |
| `AWS_EMF_SERVICE_TYPE` | `SpringBootApp` | Service type |

## Health Checks

The application exposes health check endpoints for monitoring:

### Liveness Probe

```bash
curl http://localhost:8085/health
```

Returns 200 OK when application is running.

### Readiness Probe

```bash
curl http://localhost:8085/actuator/health/readiness
```

Returns 200 OK when application is ready to serve requests.

## Structured Logging Best Practices

### Log Important Events

```java
log.info("User created with id: {}", userId);
log.warn("High memory usage detected: {}%", memoryUsage);
log.error("Failed to connect to database", exception);
```

### Avoid Logging Sensitive Data

```java
// Bad: Logs password
log.info("User created: {}", user);

// Good: Only logs necessary info
log.info("User created with email: {}", user.getEmail());
```

### Use Appropriate Log Levels

```java
// TRACE: Very detailed, only in development
log.trace("Processing request with method: {}", method);

// DEBUG: Detailed diagnostic information
log.debug("Retrieved {} items from cache", items.size());

// INFO: Informational message
log.info("Service started successfully");

// WARN: Potentially harmful situations
log.warn("Connection retry #3 to database");

// ERROR: Error events
log.error("Failed to process payment", exception);
```

## Troubleshooting Logging

### Logs not appearing

1. Check log level configuration
2. Verify logger name matches configured packages
3. Ensure logging framework is included in dependencies

### Too much logging

Increase log level to `WARN` or `ERROR`:

```bash
export LOG_LEVEL=WARN
```

Or configure specific packages:

```yaml
logging:
  level:
    uk.gov.defra: INFO
    org.springframework: WARN
```

### Missing trace IDs

Ensure `RequestTracingFilter` is active:

- Check `RequestTracingFilter` bean is loaded
- Verify filter order is `HIGHEST_PRECEDENCE`
- Confirm MDC is properly configured in Logback

## See Also

- [Architecture](../architecture/index.md)
- [Configuration](../configuration/index.md)
- [Monitoring Best Practices](https://www.elastic.co/guide/en/ecs/current/index.html)
