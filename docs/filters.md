# Filters & Middleware

## Request and Logging Filters

Servlet filters intercept HTTP requests/responses before they reach controllers. The service uses a servlet filter for request tracing and a Logback event filter to suppress health-request logs.

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
   - If the header is missing or blank, the trace ID remains unset

2. **Populate Request Metadata**
   - `http.request.method` — HTTP method (GET, POST, PUT, DELETE, etc.)
   - `url.full` — Request URL without the query string

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
| `url.full`                  | Request URL without query string | `http://localhost:8085/example` |
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
//   "@timestamp": "2024-11-21T13:16:51Z",
//   "log.level": "INFO",
//   "log.logger": "uk.gov.defra.trade.imports.fish.be.service.ExampleService",
//   "trace.id": "550e8400-e29b-41d4-a716-446655440000",
//   "http.request.method": "POST",
//   "url.full": "http://localhost:8085/example",
//   "message": "Creating example"
// }
```

Exception handlers access trace ID:

```java
String traceId = MDC.get("trace.id");
if (traceId != null) {
    problemDetail.setProperty("traceId", traceId);
}
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

When the request includes a trace ID, the error response may include it and logs include it for correlation.

---

## HealthCheckFilter

**Purpose**: Suppress console log events for health requests to reduce log noise. This is a Logback event filter, not a servlet filter; health requests still pass through the normal HTTP filter chain and reach the controller.

**Location**: [`src/main/java/uk/gov/defra/trade/imports/fish/be/filter/HealthCheckFilter.java`](https://github.com/DEFRA/trade-imports-fish-be/blob/main/src/main/java/uk/gov/defra/trade/imports/fish/be/filter/HealthCheckFilter.java)

### Behavior

For each logging event, it checks the MDC `url.full` value. Events whose URL contains `/health` are denied; other events are allowed to continue through the logging pipeline.

### Benefits

- Reduces console log noise from routine health check probes

---

## Filter Chain Order

Servlet filters execute in `@Order` sequence (lower precedence first):

```
RequestTracingFilter (HIGHEST_PRECEDENCE = -2147483648)
  ↓
Other Spring Filters
  ↓
Controller
```

**Result**:

- Requests, including health checks, pass through the servlet filter chain
- `HealthCheckFilter` only suppresses matching log events; it does not bypass HTTP filters or controller processing

---

## Common Filter Patterns

### Adding a New Filter

1. Create a class implementing `Filter`:

   ```java
   @Component
   @Order(Ordered.HIGHEST_PRECEDENCE + 1)  // Run after RequestTracingFilter
   public class CustomFilter implements Filter {
       @Override
       public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
           throws IOException, ServletException {
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

- `HealthCheckFilter` only suppresses health-request log events; it does not bypass request processing
- Check for heavy operations in health endpoint processing or other advice

**MDC Leakage (Data in Wrong Request)**:

- RequestTracingFilter has `MDC.clear()` in `finally` block to prevent this
- If upgrading Spring/logging libraries, verify they also clear MDC properly

---

## Integration with Logging

The ECS logback configuration automatically outputs MDC fields:

```xml
<springProperty name="service.version" source="cdp.service-version"/>
<springProperty name="service.name" source="spring.application.name"/>

<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
  <filter class="uk.gov.defra.trade.imports.fish.be.filter.HealthCheckFilter"/>
  <encoder class="co.elastic.logging.logback.EcsEncoder">
    <serviceVersion>${service.version}</serviceVersion>
    <serviceName>${service.name}</serviceName>
  </encoder>
</appender>
```

The ECS encoder writes standard fields such as `@timestamp`, `log.level`, and `log.logger`, and automatically includes MDC fields.

---

## Security Considerations

**MDC Clearing**: Critical to clear MDC in `finally` to prevent:

- Trace ID leaking to unrelated requests in thread pool
- Private data (user IDs, trace IDs) appearing in wrong request's logs

**Sensitive Data**: Avoid storing passwords, tokens, or PII in MDC. If necessary, truncate or hash before storing.

**Header Validation**: `RequestTracingFilter` does not validate trace ID format. If using trace ID for authorization or critical decisions, validate in application logic.
