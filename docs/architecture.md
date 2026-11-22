# Architecture & Setup

## Technology Stack

- **Framework**: Spring Boot 3.5.5
- **Language**: Java 25
- **Data Store**: MongoDB 7.0.28
- **AWS Services**: STS (via AWS SDK), CloudWatch EMF (Embedded Metric Format)
- **Local AWS Emulation**: Floci (replacement for LocalStack)
- **Build Tool**: Maven 3.x
- **Logging**: SLF4J with Logback, structured ECS format
- **Metrics**: Micrometer with CloudWatch backend
- **API Documentation**: OpenAPI 3.0 (Swagger)
- **Containerization**: Docker & Docker Compose

## Project Structure

```
trade-imports-fish-be/
├── src/main/java/uk/gov/defra/trade/imports/fish/be/
│   ├── Application.java                 # Spring Boot entry point
│   ├── configuration/                   # Spring configuration beans
│   │   ├── AwsConfig.java               # AWS SDK setup (STS client)
│   │   ├── MongoConfig.java             # MongoDB configuration
│   │   ├── LoggingConfig.java           # Logging setup
│   │   ├── MetricsConfig.java           # Micrometer metrics
│   │   ├── ProxyConfig.java             # HTTP proxy configuration
│   │   ├── RestClientConfig.java        # Feign client configuration
│   │   ├── FeignLoggingConfig.java      # Feign HTTP client logging
│   │   └── tls/
│   │       ├── TrustStoreConfiguration.java  # Certificate handling
│   │       └── CertificateLoader.java
│   ├── controller/                      # REST endpoints
│   │   └── ExampleController.java       # CRUD example API
│   ├── service/                         # Business logic
│   │   ├── ExampleService.java          # CRUD service
│   │   └── EmfMetricsPublisher.java     # Custom metrics publisher
│   ├── domain/                          # Data models
│   │   ├── Example.java                 # Entity with MongoDB mapping
│   │   └── repository/
│   │       └── ExampleRepository.java   # Spring Data MongoDB repository
│   ├── exceptions/                      # Exception handling
│   │   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   │   ├── ConflictException.java       # 409 Conflict
│   │   └── NotFoundException.java       # 404 Not Found
│   ├── filter/                          # Request and logging filters
│   │   ├── RequestTracingFilter.java    # MDC & trace ID population
│   │   └── HealthCheckFilter.java       # Suppresses health-request log events
│   └── interceptor/
│       └── TraceIdPropagationInterceptor.java  # Outbound trace ID propagation
├── src/main/resources/
│   ├── application.yml                  # Default configuration
│   ├── application-local.yml            # Local development overrides
│   ├── logback-spring.xml               # Logging configuration
│   └── schemas/                         # JSON schemas for validation
├── src/test/java/                       # JUnit 5 & Testcontainers tests
├── compose.yml                          # Docker Compose for local development
├── Dockerfile                           # Production container image
├── pom.xml                              # Maven dependencies
├── package.json                         # Node package manifest
└── mkdocs.yml                           # MkDocs configuration
```

## Local Development Environment

### Prerequisites

- Java 25
- Maven 3.8+
- Docker & Docker Compose
- Git

### Setup

1. **Clone the repository**:

   ```bash
   git clone https://github.com/DEFRA/trade-imports-fish-be.git
   cd trade-imports-fish-be
   ```

2. **Configure Git hooks** (for signed commits):
   ```bash
   git config core.hooksPath .githooks
   chmod +x .githooks/pre-commit
   ```

### Running Locally

#### Option 1: Full Docker Compose Setup (Recommended)

Start infrastructure (MongoDB, Floci AWS emulator, Redis) and the application:

```bash
docker compose --profile services up --build -d
```

The service will be available at `http://localhost:8085`.

Access MongoDB: `mongodb://localhost:27017/trade-imports-fish-be`

Access Floci (AWS): `http://localhost:4566`

#### Option 2: Infrastructure Only (with local Java execution)

Start only the infrastructure services so the application can run locally without a port conflict:

```bash
docker compose up -d mongodb floci
```

Then run the application from your IDE or command line:

```bash
mvn spring-boot:run
```

#### Option 3: Local MongoDB Setup

If you prefer to install MongoDB locally:

```bash
# macOS
brew install mongodb-community

# Start MongoDB
brew services start mongodb-community
```

MongoDB will listen on `mongodb://localhost:27017` by default.

### Publishing Feature Branches

The [Publish workflow](https://github.com/DEFRA/trade-imports-fish-be/blob/main/.github/workflows/publish.yml) publishes stable releases on pushes to `main`. To publish a feature branch for testing, run **Publish** manually from GitHub Actions and select the branch.

- Branch names must match `feature|bugfix|ci|chore/FISB-<digits>-<description>`, with a lowercase, hyphen-separated description (for example `feature/FISB-1234-upload-documents`).
- The workflow runs `mvn -B -e clean verify` and then publishes a unique prerelease image to ECR (not Docker Hub).
- Validation in [`scripts/configure-publish-settings.js`](https://github.com/DEFRA/trade-imports-fish-be/blob/main/scripts/configure-publish-settings.js) rejects a prerelease that would reuse a stable image tag, for example when the commit is already stable-tagged.
- After it succeeds, select the resulting version in the CDP Portal.

### Environment Variables

Key configuration via environment variables (see [Configuration](configuration.md) for complete list):

| Variable                 | Default                     | Purpose                             |
| ------------------------ | --------------------------- | ----------------------------------- |
| `PORT`                   | `8085`                      | Server listening port               |
| `MONGO_URI`              | `mongodb://localhost:27017` | MongoDB connection URI              |
| `MONGO_DATABASE`         | `trade-imports-fish-be`     | MongoDB database name               |
| `LOG_LEVEL`              | `info`                      | Root logging level                  |
| `AWS_REGION`             | `eu-west-2`                 | AWS region                          |
| `AWS_ENDPOINT_URL`       | (empty)                     | AWS endpoint override (for Floci)   |
| `METRICS_ENABLED`        | `true`                      | Enable the CloudWatch EMF publisher |
| `SPRING_PROFILES_ACTIVE` | (empty)                     | Active profiles (e.g., `local`)     |

## Application Lifecycle

### Startup Sequence

1. Spring Boot initializes `Application.class` with `@SpringBootApplication`
2. Configuration beans are created (@Configuration classes in `configuration/`)
3. MongoDB connection established (via `spring.data.mongodb.uri`)
4. Filters registered: `RequestTracingFilter` runs at `HIGHEST_PRECEDENCE`
5. Controllers registered and routes ready
6. Actuator endpoints available at `/health`, `/metrics`, `/info`, etc.
7. Server listens on configured `PORT`

### Graceful Shutdown

- Configured via `server.shutdown: graceful`
- Timeout: `lifecycle.timeout-per-shutdown-phase: 30s`
- In-flight requests allowed to complete before termination

## Domain Model

### Example Entity

The `Example` entity demonstrates MongoDB persistence with CDP compliance:

```java
@Document(collection = "examples")
public class Example {
    @Id
    String id;                    // Auto-generated MongoDB ObjectId

    @Indexed(unique = true)
    @NotBlank
    String name;                  // Unique constraint

    @NotBlank
    String value;

    Integer counter;
    LocalDateTime created;
}
```

**MongoDB Index**: Automatic index creation enabled (`auto-index-creation: true`). Unique index on `name` field prevents duplicates.

## Security Considerations

### TLS/SSL

- Enable TLS in the MongoDB connection URI (for example, with `tls=true`); the default URI does not enable TLS.
- `spring.data.mongodb.ssl.enabled` configures the SSL context but does not enable TLS on its own.
- Custom certificate loading via `TrustStoreConfiguration` (for CDP environments)
- Proxy configuration supported for outbound HTTPS requests

### Authentication

- AWS authentication uses `DefaultCredentialsProvider` (checks environment and credentials file)
- No API key authentication implemented (scope: example CRUD only)

### CORS

- Not explicitly configured (inherits Spring Boot defaults)
- Configure via `@CrossOrigin` on controllers if needed

## Deployment

### Docker Image

Build the Docker image:

```bash
docker build -t trade-imports-fish-be:latest .
```

The `Dockerfile` uses a multi-stage build for optimized image size.

### Environment-Specific Configuration

- **Local**: `application-local.yml` with debug logging and test endpoints
- **CDP Environments**: Production defaults from `application.yml` with environment variable overrides

Activate profiles via `SPRING_PROFILES_ACTIVE` (comma-separated: `local,test`).

### Documentation Site

The `Deploy documentation` workflow (`.github/workflows/build-docs.yml`) builds this site with `mkdocs-material` 9.8.1 on Python 3.12 using `mkdocs build --strict`, writing to `_site` (`site_dir` in `mkdocs.yml`).

- Pull requests to `main` that change `docs/**` or `mkdocs.yml` run the build only (on `opened`, `reopened` and `edited`).
- Pushes to `main` touching those paths, and manual `workflow_dispatch` runs, also deploy the site to GitHub Pages.
