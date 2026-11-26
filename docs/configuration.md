# Configuration

## Overview

Configuration is managed via environment variables, YAML files, and Spring profiles. The application supports environment-specific settings for local development, testing, and production CDP environments.

## Application Properties

The main configuration is in [`application.yml`](https://github.com/DEFRA/trade-imports-fish-be/blob/main/src/main/resources/application.yml).

### Server Configuration

| Property                               | Environment Variable | Default    | Purpose                   |
| -------------------------------------- | -------------------- | ---------- | ------------------------- |
| `server.port`                          | `PORT`               | `8085`     | Listening port            |
| `server.shutdown`                      | —                    | `graceful` | Shutdown behavior         |
| `spring.lifecycle.timeout-per-shutdown-phase` | —                    | `30s`      | Graceful shutdown timeout |

### Spring Application

| Property                  | Environment Variable     | Default                 | Purpose                         |
| ------------------------- | ------------------------ | ----------------------- | ------------------------------- |
| `spring.application.name` | —                        | `trade-imports-fish-be` | Service name                    |
| `spring.profiles.active`  | `SPRING_PROFILES_ACTIVE` | (empty)                 | Active profiles (e.g., `local`) |

### MongoDB

| Property                                                          | Environment Variable          | Default                      | Purpose                                                                                      |
| ----------------------------------------------------------------- | ----------------------------- | ---------------------------- | -------------------------------------------------------------------------------------------- |
| `spring.data.mongodb.uri`                                         | `MONGO_URI`                   | `mongodb://localhost:27017/` | MongoDB connection URI                                                                       |
| `spring.data.mongodb.database`                                    | `MONGO_DATABASE`              | `trade-imports-fish-be`      | Database name                                                                                |
| `spring.data.mongodb.auto-index-creation`                         | —                             | `true`                       | Auto-create indexes from `@Indexed` annotations                                              |
| `spring.data.mongodb.read-preference`                             | `MONGO_READ_PREFERENCE`       | `secondary`                  | Read preference (secondary, primary, etc.)                                                   |
| `spring.data.mongodb.write-concern`                               | `MONGO_WRITE_CONCERN`         | `majority`                   | Write acknowledgment level                                                                   |
| `spring.data.mongodb.connection-pool.min-size`                    | `MONGO_POOL_MIN_SIZE`         | `10`                         | Minimum pool connections                                                                     |
| `spring.data.mongodb.connection-pool.max-size`                    | `MONGO_POOL_MAX_SIZE`         | `100`                        | Maximum pool connections                                                                     |
| `spring.data.mongodb.connection-pool.max-wait-time-ms`            | `MONGO_POOL_MAX_WAIT_TIME_MS` | `2000`                       | Max wait for available connection (ms)                                                       |
| `spring.data.mongodb.connection-pool.max-connection-idle-time-ms` | `MONGO_POOL_MAX_IDLE_TIME_MS` | `60000`                      | Max idle time before closing (ms)                                                            |
| `spring.data.mongodb.ssl.enabled`                                 | —                             | `true`                       | Configure the SSL context; TLS must also be enabled in `MONGO_URI` (for example, `tls=true`) |

**Connection URI Format**:

```
mongodb://[user[:password]@]host[:port][,...][/database][?options]
```

Example for local Docker:

```
mongodb://mongodb:27017
```

Example for production with authentication:

```
mongodb://user:password@mongo.example.com:27017/?ssl=true&authSource=admin
```

### CDP Platform Configuration

| Property                  | Environment Variable     | Default                 | Purpose                               |
| ------------------------- | ------------------------ | ----------------------- | ------------------------------------- |
| `cdp.certificate`         | `TRUSTSTORE_CDP_ROOT_CA` | (empty)                 | Root CA certificate (PEM encoded)     |
| `cdp.metrics.enabled`     | —                        | `true`                  | Unused; does not control metrics collection or EMF publishing |
| `cdp.service-version`     | `SERVICE_VERSION`        | `0.0.0-local`           | Service version (exposed via `/info`) |
| `cdp.tracing.header-name` | `TRACING_HEADER`         | `x-cdp-request-id`      | Request trace ID header name          |
| `cdp.cloudwatch.endpoint` | `AWS_EMF_AGENT_ENDPOINT` | `http://localhost:4566` | CloudWatch EMF agent endpoint         |
| `cdp.proxyUrl`            | `HTTP_PROXY`             | (empty)                 | HTTP/HTTPS proxy URL                  |

### AWS Configuration

| Property               | Environment Variable   | Default                 | Purpose                                       |
| ---------------------- | ---------------------- | ----------------------- | --------------------------------------------- |
| `aws.region`           | `AWS_REGION`           | `eu-west-2`             | AWS region for STS, CloudWatch                |
| `aws.emf.enabled`      | `AWS_EMF_ENABLED`      | `true`                  | Unused; does not control EMF publishing       |
| `aws.emf.environment`  | `AWS_EMF_ENVIRONMENT`  | `Local`                 | Environment for EMF (Local, Test, Production) |
| `aws.emf.namespace`    | `AWS_EMF_NAMESPACE`    | `trade-imports-fish-be` | CloudWatch namespace                          |
| `aws.emf.service.name` | `AWS_EMF_SERVICE_NAME` | `trade-imports-fish-be` | Service name for metrics                      |
| `aws.emf.service.type` | `AWS_EMF_SERVICE_TYPE` | `SpringBootApp`         | Service type for metrics                      |

**AWS Credentials**: Uses `DefaultCredentialsProvider` from AWS SDK. Checks in order:

1. Environment variables: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
2. Credentials file: `~/.aws/credentials`
3. IAM role (if running on EC2/Lambda)

### Logging Configuration

| Property                                           | Environment Variable                    | Default | Purpose                   |
| -------------------------------------------------- | --------------------------------------- | ------- | ------------------------- |
| `logging.level.root`                               | `LOG_LEVEL`                             | `info`  | Root logger level         |
| `logging.level.uk.gov.defra.trade.imports.fish.be` | `LOG_LEVEL`                             | `DEBUG` | Application package level |
| `logging.level.org.springframework`                | `LOGGING_LEVEL_ORG_SPRINGFRAMEWORK`     | `INFO`  | Spring Framework level    |
| `logging.level.org.springframework.web`            | `LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB` | `INFO`  | Spring Web level          |
| `logging.level.org.mongodb.driver`                 | `LOGGING_LEVEL_ORG_MONGODB_DRIVER`      | `WARN`  | MongoDB driver level      |

**Log Output Format**:
Configured in [`logback-spring.xml`](https://github.com/DEFRA/trade-imports-fish-be/blob/main/src/main/resources/logback-spring.xml). Uses ECS (Elastic Common Schema) structured format for JSON output.

### Actuator Endpoints

| Property                                    | Environment Variable | Default                     | Purpose                                               |
| ------------------------------------------- | -------------------- | --------------------------- | ----------------------------------------------------- |
| `management.endpoints.web.base-path`        | —                    | `/`                         | Base path for actuator endpoints                      |
| `management.endpoints.web.exposure.include` | —                    | `health,metrics,info,cache` | Exposed endpoints                                     |
| `management.endpoint.health.show-details`   | —                    | `never`                     | Health details visibility                             |
| `management.health.mongo.enabled`           | —                    | `false`                     | Disable MongoDB health check (non-blocking readiness) |
| `management.metrics.enabled`                | `METRICS_ENABLED`    | `true`                      | Enable the EMF publisher bean; collection uses `management.metrics.enable.*` |

**Exposed Endpoints**:

- `GET /health` — Liveness probe
- `GET /metrics` — Available metrics
- `GET /metrics/{metric}` — Specific metric value
- `GET /info` — Service info (name, version, environment)

### Application Info

Exposed via `GET /info`:

| Property               | Environment Variable        | Default                 |
| ---------------------- | --------------------------- | ----------------------- |
| `info.app.name`        | —                           | `trade-imports-fish-be` |
| `info.app.version`     | (via `cdp.service-version`) | `0.0.0-local`           |
| `info.app.environment` | `ENVIRONMENT`               | `local`                 |

## Spring Profiles

Profiles allow environment-specific configuration overrides.

### Local Profile

Activated via `SPRING_PROFILES_ACTIVE=local`. Loads [`application-local.yml`](https://github.com/DEFRA/trade-imports-fish-be/blob/main/src/main/resources/application-local.yml) with:

- Debug logging
- Local development endpoints
- Test data seeds (if configured)

### Environment Variables via Profiles

```bash
# Local development
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run

# Multiple profiles
SPRING_PROFILES_ACTIVE=local,test mvn spring-boot:run

# Production (no profile)
PORT=8085 MONGO_URI=... java -jar app.jar
```

## Configuration by Environment

### Local Development (Docker Compose)

```yaml
# compose.yml extracts
environment:
  PORT: 8085
  MONGO_URI: mongodb://mongodb:27017
  AWS_ENDPOINT_URL: http://floci:4566
  AWS_EMF_AGENT_ENDPOINT: http://floci:4566
  LOG_LEVEL: DEBUG
  SPRING_PROFILES_ACTIVE: local
```

### Local Development (IDE)

Create a run configuration with environment variables:

```
PORT=8085
MONGO_URI=mongodb://localhost:27017
AWS_REGION=eu-west-2
LOG_LEVEL=DEBUG
SPRING_PROFILES_ACTIVE=local
```

### Production (CDP Platform)

Injected via platform secrets/configmap:

```bash
PORT=8085
MONGO_URI=mongodb://user:pass@mongo.prod.internal:27017/trade-imports-fish-be
AWS_REGION=eu-west-2
AWS_EMF_ENVIRONMENT=Production
SERVICE_VERSION=1.2.3
LOG_LEVEL=info
TRUSTSTORE_CDP_ROOT_CA=-----BEGIN CERTIFICATE-----...
```

No `SPRING_PROFILES_ACTIVE` (uses production defaults).

## Configuration Validation

Spring validates configuration on startup:

```bash
# Compile Java sources (does not validate runtime configuration)
mvn clean compile

# Start the application to load YAML and bind configuration values
mvn spring-boot:run
```

If environment variables are missing, Spring will use defaults. If a configuration value is required and missing, the application will fail to start with a descriptive error.

## Externalizing Configuration

For production deployments, use:

1. **Environment Variables** (recommended for CDP)

   ```bash
   export MONGO_URI=mongodb://...
   export AWS_REGION=eu-west-2
   java -jar trade-imports-fish-be.jar
   ```

2. **Spring Cloud Config Server** (if available)
   - Centralized config management
   - Hot reload support

3. **Kubernetes ConfigMap/Secrets**
   ```yaml
   apiVersion: v1
   kind: ConfigMap
   metadata:
     name: trade-imports-fish-be-config
   data:
     application.yml: |
       server:
         port: 8085
       spring:
         data:
           mongodb:
             uri: ${MONGO_URI}
   ```

## Troubleshooting Configuration

**MongoDB Connection Refused**:

- Check `MONGO_URI` is accessible
- Verify MongoDB is running: `mongosh mongodb://localhost:27017`
- For Docker: ensure service name is correct (e.g., `mongodb` not `mongo-db`)

**Metrics Not Appearing**:

- Verify `METRICS_ENABLED=true` to enable the EMF publisher bean. `AWS_EMF_ENABLED` is unused and does not disable publishing.
- Check CloudWatch endpoint accessibility: `AWS_EMF_AGENT_ENDPOINT`
- For local: Floci should be running at `http://localhost:4566`

**Logging Not Captured**:

- Verify `LOG_LEVEL` is set
- Check logback configuration in `logback-spring.xml`
- For ECS format: ensure Docker logs driver captures stdout

**Health Check Failing**:

- Verify service is fully started (check logs)
- For Kubernetes: adjust `initialDelaySeconds` in liveness probe
- MongoDB health disabled by default; enable with `management.health.mongo.enabled=true` only if critical
