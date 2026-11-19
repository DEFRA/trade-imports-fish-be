# Architecture

## System Architecture

The Trade Imports Fish Backend follows a layered, three-tier architecture pattern with separation of concerns across controllers, services, and data access layers.

## High-Level Architecture Diagram

```
┌────────────────────────────────────────┐
│         Client/External Systems        │
└──────────────────┬─────────────────────┘
                   │ HTTP/REST
┌──────────────────▼─────────────────────┐
│    Request Filters & Interceptors      │
│  (Tracing, Health Check, Logging)      │
└──────────────────┬─────────────────────┘
                   │
┌──────────────────▼─────────────────────┐
│      REST API Controller Layer          │
│    (ExampleController)                  │
└──────────────────┬─────────────────────┘
                   │
┌──────────────────▼─────────────────────┐
│      Business Logic Layer               │
│    (ExampleService)                     │
│    - Validation & Exception Handling    │
│    - Metrics Publishing                 │
└──────────────────┬─────────────────────┘
                   │
┌──────────────────▼─────────────────────┐
│    Data Access Layer                    │
│  (ExampleRepository)                    │
│  Spring Data MongoDB                    │
└──────────────────┬─────────────────────┘
                   │
┌──────────────────▼─────────────────────┐
│      MongoDB Database                   │
│  - Collections                          │
│  - Indexes                              │
│  - Persistence                          │
└─────────────────────────────────────────┘
```

## Core Components

### 1. Controllers (`controller/`)

**Responsibility**: Handle HTTP requests and responses.

**Key Class**: `ExampleController`

**Features**:

- RESTful endpoint mapping with `@RestController`
- Request validation using `@Valid` and Bean Validation
- Response status codes (`@ResponseStatus`)
- OpenAPI/Swagger annotations (`@Operation`, `@Tag`)
- Request timing metrics (`@Timed`)
- Request counting metrics (`@Counted`)
- Structured logging via SLF4J

**Endpoints** (all in `/example` path):

- `POST` - Create example (HTTP 201)
- `GET` - List all examples (HTTP 200)
- `GET /{id}` - Get single example (HTTP 200)
- `PUT /{id}` - Update example (HTTP 200)
- `DELETE /{id}` - Delete example (HTTP 204)

### 2. Services (`service/`)

**Responsibility**: Implement business logic, validation, and coordination.

**Key Classes**:

#### `ExampleService`

- **CRUD operations** on Example entities
- **Validation**: Checks for duplicate names
- **Exception handling**: Converts database exceptions to domain exceptions
- **Metrics**: Publishes custom metrics via `MeterRegistry`
- **Logging**: Structured logging with SLF4J (ECS format)
- **Idempotency**: Handles race conditions (duplicate key errors)

#### `EmfMetricsPublisher`

- Publishes **custom metrics** to CloudWatch
- Supports **EMF (Embedded Metric Format)** for structured metrics
- Configured via environment variables

### 3. Repositories (`domain/repository/`)

**Responsibility**: Abstract data access, provide query methods.

**Key Interface**: `ExampleRepository`

- Extends `MongoRepository` for CRUD support
- **Custom queries**:
  - `findByName(String name): Optional<Example>`
- Automatic transaction management
- Automatic index management

### 4. Models/Entities (`domain/`)

**Responsibility**: Define data structures and persistence mappings.

**Key Class**: `Example`

**MongoDB Mapping**:

- Collection name: `examples`
- Document structure with `@Document`
- Auto-generated ID with `@Id`

**Fields**:

- `id`: MongoDB ObjectId (auto-generated)
- `name`: String, indexed with unique constraint
- `value`: String, required
- `counter`: Integer, optional
- `created`: LocalDateTime

**Validation**:

- `@NotBlank` on `name` and `value`
- Database-level unique index on `name`

### 5. Filters (`filter/`)

**Responsibility**: Intercept all HTTP requests for cross-cutting concerns.

#### `RequestTracingFilter`

- Populates MDC (Mapped Diagnostic Context) with request metadata
- Extracts trace ID from `x-cdp-request-id` header (configurable)
- Runs at highest precedence (`Ordered.HIGHEST_PRECEDENCE`)
- Provides:
  - `trace.id` - Request correlation ID
  - `http.request.method` - HTTP method
  - `http.response.status_code` - Response status
  - `url.full` - Request URL

#### `HealthCheckFilter`

- Provides fast-path for `/health` endpoint
- Prevents full request processing for health checks
- Improves performance for health monitoring

### 6. Interceptors (`interceptor/`)

**Responsibility**: Modify outgoing HTTP requests (Feign clients).

#### `TraceIdPropagationInterceptor`

- Propagates trace ID to external service calls
- Maintains request correlation across service boundaries
- Configured for Spring Cloud Feign clients

### 7. Configuration (`configuration/`)

**Responsibility**: Wire up beans and configure framework features.

**Key Configuration Classes**:

#### `LoggingConfig`

- Sets service version in System properties for Logback
- Enables ECS format logging

#### `MongoConfig`

- Configures MongoDB client settings
- Sets read/write concerns
- Configures connection pool
- Sets up auto-index creation

#### `AwsConfig`

- Provides AWS SDK configuration
- Sets up regional endpoints

#### `MetricsConfig`

- Configures Micrometer metrics
- Registers custom metrics
- Publishes to CloudWatch via EMF

#### `ProxyConfig`

- Sets up HTTP proxy for external calls
- Applies proxy to Feign clients

#### `RestClientConfig`

- Configures Spring Cloud Feign
- Sets up interceptors for trace propagation

#### `FeignLoggingConfig`

- Configures logging level for Feign HTTP client

#### `TrustStoreConfiguration` / `CertificateLoader`

- Loads custom CA certificates
- Configures HTTPS/TLS for secure connections
- Supports CDP certificate authority

### 8. Exceptions (`exceptions/`)

**Responsibility**: Domain-specific exception handling.

**Custom Exceptions**:

#### `NotFoundException`

- Thrown when a resource is not found
- Returns HTTP 404

#### `ConflictException`

- Thrown when a unique constraint is violated
- Returns HTTP 409

#### `GlobalExceptionHandler`

- Central exception handling with `@ControllerAdvice`
- Converts exceptions to standardized error responses
- Provides error codes and descriptions

## Configuration & Profiles

### Application Profiles

- **default** (production): Minimal logging, production-ready
- **local**: Enhanced debugging, local development overrides
- **prod** (optional): Additional production configurations

### Environment Variables

**Key Configuration**:

| Variable          | Default                      | Purpose                |
| ----------------- | ---------------------------- | ---------------------- |
| `PORT`            | 8085                         | Server port            |
| `MONGO_URI`       | `mongodb://localhost:27017/` | MongoDB connection     |
| `MONGO_DATABASE`  | `trade-imports-fish-be`      | Database name          |
| `LOG_LEVEL`       | `info`                       | Root logging level     |
| `TRACING_HEADER`  | `x-cdp-request-id`           | Trace ID header name   |
| `AWS_REGION`      | `eu-west-2`                  | AWS region             |
| `AWS_EMF_ENABLED` | `true`                       | CloudWatch EMF metrics |

## Cross-Cutting Concerns

### 1. Logging (ECS Format)

- **Framework**: SLF4J with Logback
- **Format**: Elastic Common Schema (ECS)
- **Encoder**: Logback ECS encoder
- **MDC Fields**: Automatically included in structured logs
  - `trace.id` - Request correlation
  - `http.request.method` - HTTP method
  - `http.response.status_code` - Response status
  - `service.version` - Application version
  - `service.name` - Application name

### 2. Metrics (Micrometer & CloudWatch)

- **Framework**: Micrometer (Spring Boot Actuator)
- **Backend**: CloudWatch via EMF (Embedded Metric Format)
- **Custom Metrics**:
  - `example_created` - Counter for created examples
  - `controller.*.time` - Timing metrics for controller methods
  - `controller.*.count` - Request counts
- **Built-in Metrics**:
  - JVM memory, threads, GC
  - HTTP server requests
  - Spring application events

### 3. Request Tracing & Correlation

- **Header**: `X-CDP-Request-ID` (configurable)
- **Propagation**: Via MDC in logs and Feign interceptor
- **Benefits**:
  - Correlate logs across services
  - Debug distributed request flows
  - Monitor request patterns

### 4. Health Checks

- **Endpoint**: `GET /health`
- **MongoDB checks**: Disabled by `management.health.mongo.enabled: false`; an UP result does not establish database connectivity
- **Logging**: Health-check log events are suppressed by the Logback `HealthCheckFilter`
- **HTTP status**: Returns 200 when the enabled health indicators report healthy

### 5. Actuator Endpoints

Configured endpoints (exposed via management.endpoints.web.exposure.include):

| Endpoint   | Purpose            |
| ---------- | ------------------ |
| `/health`  | Liveness/readiness |
| `/metrics` | Metrics registry   |
| `/info`    | Application info   |
| `/cache`   | Cache statistics   |

## Design Patterns

### 1. Dependency Injection

All dependencies injected via constructor (Spring-managed):

- Testable
- Immutable fields
- Clear dependencies

### 2. Repository Pattern

Data access abstraction via Spring Data:

- Loose coupling from persistence
- Easy testing with mocks
- Query derivation from method names

### 3. Service Layer Pattern

Business logic encapsulation:

- Separation of concerns
- Transaction management
- Cross-cutting concerns

### 4. Exception Translation

Database exceptions converted to domain exceptions:

- `DuplicateKeyException` → `ConflictException`
- `EmptyResultDataAccessException` → `NotFoundException`

### 5. Configuration Over Annotation

Properties-driven configuration:

- Externalized configuration
- Environment-specific settings
- Security best practices

## Deployment Architecture

### Local Development

```
Client
  ↓
Spring Boot Application (8085)
  ↓
MongoDB (via Docker)
Floci (AWS S3/SQS emulator)
Redis (optional)
```

### Production (CDP Platform)

```
Client
  ↓
API Gateway / Load Balancer
  ↓
Spring Boot Application (auto-scaled)
  ↓
MongoDB (managed database)
AWS S3 (file storage)
AWS SQS (async messaging)
CloudWatch (logging & metrics)
```

## Security Architecture

### Authentication & Authorization

- Supports OAuth2/OIDC (Spring Security)
- Role-based access control (RBAC) ready
- Bearer token validation

### Encryption

- TLS/HTTPS for in-transit encryption
- Custom CA certificate support
- MongoDB SSL/TLS connection support

### Validation

- Bean Validation on all inputs
- Database-level constraints
- Exception handling for violations

## Scalability Considerations

### Stateless Design

- No session state in application memory
- Distributable across multiple instances
- Load balancer friendly

### Database Scaling

- MongoDB replica sets for HA
- Read preferences (secondary) for read scaling
- Write concern (majority) for durability
- Automatic index creation on startup

### Metrics & Monitoring

- CloudWatch EMF for serverless/container metrics
- Custom business metrics
- Application health indicators

## Next Steps

- [Configuration Guide](../configuration/index.md)
- [API Documentation](../api/index.md)
- [Contributing Guide](../contributing/index.md)
