# Filters & Middleware

## Servlet Filters Overview

Servlet filters intercept HTTP requests/responses before they reach controllers. The service implements two filters for cross-cutting concerns: request tracing and health check bypass.

## RequestTracingFilter

**Purpose**: Populate MDC (Mapped Diagnostic Context) with request metadata for structured logging and trace correlation.

**Location**: [`src/main/java/uk/gov/defra/trade/imports/fish/be/filter/RequestTracingFilter.java`](https://github.com/DEFRA/trade-imports-fish-be/blob/main/src/main/java/uk/gov/defra/trade/imports/fish/be/filter/RequestTracingFilter.java)

### Execution Order

Runs at `HIGHEST_PRECEDENCE` to ensure MDC is populated before any other filters, interceptors, or handlers.

```java
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTracingFilter implements Filter
```

### Behavior

For each HTTP request:

1. **Extract Trace ID**
   - Read `x-cdp-request-id` header (configurable via `cdp.tracing.header-name`)
   - If header present and non-empty, store in MDC as `trace.id`
   - If header missing or empty, trace ID remains empty (exception handlers can still use it)

2. **Populate Request Metadata**
   - `http.request.method` — HTTP method (GET, POST, PUT, DELETE, etc.)
   - `url.full` — Full request URL including query string

3. **Execute Filter Chain**
   - Continue to next filter or controller

4. **Capture Response Status**
   - After chain completes, store `http.response.status_code`

5. **Clear MDC**
   - **Critical**: MDC cleared in `finally` block to prevent data leakage to subsequent requests (servlet thread pools reuse threads)

### MDC Fields

| Field                       | Value                        | Example                                  |
| --------------------------- | ---------------------------- | ---------------------------------------- |
| `trace.id`                  | Request trace ID from header | `550e8400-e29b-41d4-a716-446655440000`   |
| `http.request.method`       | HTTP verb                    | `POST`                                   |
| `url.full`                  | Full request URL             | `http://localhost:8085/example?limit=10` |
| `http.response.status_code` | Response HTTP status         | `201`                                    |

### Configuration

**Header Name**:

```yaml
cdp:
  tracing:
    header-name: ${TRACING_HEADER:x-cdp-request-id}
```

Change via `TRACING_HEADER` environment variable.

### Usage in Application

MDC values are automatically included in log output (ECS format):

```java
log.info("Creating example");
// Output (ECS format):
// {
//   "timestamp": "2024-11-21T13:16:51Z",
//   "level": "INFO",
//   "logger": "ExampleService",
//   "trace.id": "550e8400-e29b-41d4-a716-446655440000",
//   "http.request.method": "POST",
//   "url.full": "http://localhost:8085/example",
//   "message": "Creating example"
// }
```

Exception handlers access trace ID:

```java
String traceId = MDC.get("trace.id");
problemDetail.setProperty("traceId", traceId);
```

### Example Request/Response

**Request with Trace ID**:

```bash
curl http://localhost:8085/example \
  -H "x-cdp-request-id: 550e8400-e29b-41d4-a716-446655440000"
```

**Server-side MDC**:

```
trace.id=550e8400-e29b-41d4-a716-446655440000
http.request.method=GET
url.full=http://localhost:8085/example
http.response.status_code=200
```

**Response includes trace ID** (via GlobalExceptionHandler for errors, or directly in logs for all requests).

---

## HealthCheckFilter

**Purpose**: Bypass subsequent filters for health check endpoint to reduce overhead and prevent circular dependencies.

**Location**: [`src/main/java/uk/gov/defra/trade/imports/fish/be/filter/HealthCheckFilter.java`](https://github.com/DEFRA/trade-imports-fish-be/blob/main/src/main/java/uk/gov/defra/trade/imports/fish/be/filter/HealthCheckFilter.java)

### Behavior

1. Check if request path is health check endpoint (e.g., `/health`)
2. If yes, skip remaining filters and pass directly to controller
3. If no, continue normal filter chain

### Configuration

```java
private static final String HEALTH_CHECK_PATH = "/health";

if (httpRequest.getRequestURI().equals(HEALTH_CHECK_PATH)) {
    chain.doFilter(request, response);
    return;
}
```

### Benefits

- Reduces latency for health check probes (Kubernetes readiness/liveness probes)
- Avoids unnecessary MDC operations for routine health checks
- Prevents potential circular dependencies if health check logic queries MDC

---

## Filter Chain Order

Filters execute in `@Order` sequence (lower precedence first):

```
RequestTracingFilter (HIGHEST_PRECEDENCE = -2147483648)
  ↓
HealthCheckFilter (default order)
  ↓
Other Spring Filters
  ↓
Controller
```

**Result**:

- MDC populated for all requests (including health checks)
- Health checks bypass remaining filters for performance

---

## Common Filter Patterns

### Adding a New Filter

1. Create a class implementing `Filter`:

   ```java
   @Component
   @Order(Ordered.HIGHEST_PRECEDENCE + 1)  // Run after RequestTracingFilter
   public class CustomFilter implements Filter {
       @Override
       public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
           // Pre-processing
           chain.doFilter(req, res);
           // Post-processing
       }
   }
   ```

2. Add `@Component` to auto-register with Spring
3. Set `@Order` relative to other filters
4. Use `try-finally` to clean up resources (like RequestTracingFilter does for MDC)

### Conditional Filter Logic

```java
HttpServletRequest http = (HttpServletRequest) request;
String path = http.getRequestURI();
String method = http.getMethod();

if (path.startsWith("/internal/") || "OPTIONS".equals(method)) {
    // Skip filter for internal endpoints or preflight requests
    chain.doFilter(request, response);
    return;
}

// Apply filter logic
```

---

## Troubleshooting

**Trace ID Missing from Logs**:

- Verify `RequestTracingFilter` is registered as a bean (check startup logs)
- Confirm header name matches client: `x-cdp-request-id` by default
- Check logback configuration includes MDC fields in output format

**Health Checks Slow**:

- Verify `HealthCheckFilter` is bypassing other filters
- Check for heavy operations in `GlobalExceptionHandler` or other advice

**MDC Leakage (Data in Wrong Request)**:

- RequestTracingFilter has `MDC.clear()` in `finally` block to prevent this
- If upgrading Spring/logging libraries, verify they also clear MDC properly

---

## Integration with Logging

The ECS logback configuration automatically outputs MDC fields:

```xml
<!-- logback-spring.xml -->
<pattern>
  {
    "timestamp": "%d{ISO_8601}",
    "level": "%p",
    "logger": "%c",
    "trace.id": "%X{trace.id}",
    "http.request.method": "%X{http.request.method}",
    "url.full": "%X{url.full}",
    "http.response.status_code": "%X{http.response.status_code}",
    "message": "%m"
  }
</pattern>
```

The `%X{key}` syntax outputs MDC values. Missing keys render as empty.

---

## Security Considerations

**MDC Clearing**: Critical to clear MDC in `finally` to prevent:

- Trace ID leaking to unrelated requests in thread pool
- Private data (user IDs, trace IDs) appearing in wrong request's logs

**Sensitive Data**: Avoid storing passwords, tokens, or PII in MDC. If necessary, truncate or hash before storing.

**Header Validation**: `RequestTracingFilter` does not validate trace ID format. If using trace ID for authorization or critical decisions, validate in application logic.
