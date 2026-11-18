# Performance Tuning

This guide covers optimizing the Trade Imports Fish Backend for performance.

## Overview

Performance optimization involves:

- Database query optimization
- Connection pooling
- Caching strategies
- JVM tuning
- Monitoring and profiling

## MongoDB Performance

### Connection Pool Tuning

Configure in `application.yml`:

```yaml
spring:
  data:
    mongodb:
      connection-pool:
        min-size: 10              # Minimum connections
        max-size: 100             # Maximum connections
        max-wait-time-ms: 2000    # Max wait for connection
        max-connection-idle-time-ms: 60000  # Idle timeout
```

### Recommended Settings

| Setting | Development | Production |
| --------- | ------------- | ----------- |
| `min-size` | 5 | 20 |
| `max-size` | 50 | 200 |
| `max-wait-time-ms` | 2000 | 5000 |
| `max-connection-idle-time-ms` | 60000 | 300000 |

Adjust based on:

- Expected concurrent requests
- Database response time
- Available server memory

### Indexing

Indexes dramatically improve query performance. The application auto-creates indexes via:

```java
@Indexed(unique = true)
private String name;
```

Verify indexes in MongoDB:

```bash
mongosh
use trade-imports-fish-be
db.examples.getIndexes()
```

### Query Optimization

#### Use Specific Queries

```java
// Good: Retrieve only needed fields
repository.findByName(name)
  .projection("name", "value")
  .getFirst()

// Avoid: Retrieve entire document
repository.findAll()
  .stream()
  .filter(e -> e.getName().equals(name))
  .findFirst()
```

#### Use Read Preferences

```yaml
spring:
  data:
    mongodb:
      read-preference: secondary  # Read from replicas
      write-concern: majority     # Write to majority
```

This balances consistency and performance.

## JVM Tuning

### Memory Configuration

Set JVM heap size:

```bash
export JAVA_OPTS="-Xmx1g -Xms512m"
```

Recommended values:

| Environment | Min Heap | Max Heap |
| ------------- | ---------- | ---------- |
| Development | 256m | 512m |
| Testing | 512m | 1g |
| Production | 1g | 4g |

Adjust based on:

- Concurrent users
- Dataset size
- Available system memory

### Garbage Collection

Tune GC for latency:

```bash
export JAVA_OPTS="-Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

Options:

| Option | Benefit |
| -------- | --------- |
| `UseG1GC` | Low latency GC |
| `MaxGCPauseMillis=200` | Control max pause time |
| `ParallelGCThreads=4` | GC concurrency |

## Caching Strategies

### Spring Cache

Enable caching:

```java
@Service
@CacheConfig(cacheNames = "examples")
public class ExampleService {
    
    @Cacheable
    public Example findById(String id) {
        return repository.findById(id).orElse(null);
    }
    
    @CacheEvict(allEntries = true)
    public void clearCache() {
    }
}
```

### Cache Configuration

Configure cache backend in `application.yml`:

```yaml
spring:
  cache:
    type: simple  # or redis, caffeine, etc.
```

## Request Processing

### Connection Pool for HTTP Clients

For external service calls:

```properties
# Increase connection pool
http.client.max-connections=100
http.client.max-connections-per-route=20
```

### Request Timeouts

Configure appropriate timeouts:

```yaml
# HTTP client timeout
http.client.connect-timeout: 5000  # 5 seconds
http.client.socket-timeout: 10000  # 10 seconds
```

## Database Connection Pooling

### HikariCP Configuration

Spring Boot uses HikariCP by default. Configure in `application.yml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
```

## Monitoring Performance

### Metrics Endpoints

Monitor performance via actuator:

```bash
# Overall metrics
curl http://localhost:8085/actuator/metrics

# HTTP requests
curl http://localhost:8085/actuator/metrics/http.server.requests

# JVM metrics
curl http://localhost:8085/actuator/metrics/jvm.memory.usage
curl http://localhost:8085/actuator/metrics/jvm.threads.live
```

### Key Performance Indicators

Monitor these metrics:

| Metric | Target | Action If Exceeded |
| -------- | -------- | ------------------- |
| Response time | <200ms | Optimize queries, increase pool |
| Error rate | <1% | Check logs, review error handling |
| JVM memory | <80% | Increase heap, check for leaks |
| DB connections | <80% | Increase pool, optimize queries |

## Load Testing

### Local Load Testing

Use Apache JMeter or similar tools:

```bash
# Test health endpoint
apache-jmeter -n -t test.jmx -l results.jtl

# Or use curl in a loop
for i in {1..1000}; do
  curl -s http://localhost:8085/health
done
```

### Profiling

Use Java Flight Recorder:

```bash
java -XX:StartFlightRecording=filename=recording.jfr ...
jfr dump --output=analysis.html recording.jfr
```

## Common Performance Issues

### High Database Query Time

**Symptoms**: Slow API responses, high database load

**Solutions**:

1. Add indexes on frequently queried fields
2. Use read preferences to distribute queries
3. Implement caching
4. Optimize query filters

### Memory Leaks

**Symptoms**: Increasing memory usage over time, frequent GC

**Solutions**:

1. Enable heap dump: `-XX:+HeapDumpOnOutOfMemoryError`
2. Analyze with Eclipse MAT
3. Check for circular references
4. Clear caches appropriately

### Connection Pool Exhaustion

**Symptoms**: "Cannot get a connection" errors, timeouts

**Solutions**:

1. Increase pool size
2. Reduce query time
3. Close connections properly
4. Monitor connection usage

## Best Practices

1. **Monitor continuously**: Track metrics in production
2. **Load test regularly**: Identify bottlenecks before issues
3. **Index strategically**: Index frequently queried fields
4. **Cache appropriately**: Cache expensive operations
5. **Tune JVM**: Match heap size to workload
6. **Use connection pooling**: Reuse connections
7. **Optimize queries**: Retrieve only needed data
8. **Handle errors gracefully**: Avoid resource exhaustion

## See Also

- [Architecture](index.md)
- [Configuration](../configuration/index.md)
- [Logging and Observability](logging.md)
