# Configuration

## Overview

The Trade Imports Fish Backend uses Spring Boot configuration management for flexible deployment across different environments.

## Configuration Hierarchy

Spring Boot configuration follows this hierarchy (highest to lowest priority):

1. Command-line arguments
2. Environment variables
3. Properties files (application-{profile}.yml)
4. Default application.yml
5. Default values in code

## Environment Profiles

The application supports multiple profiles:

- **local**: Local development
- **dev**: Development environment
- **prod**: Production environment

Activate a profile:

```bash
export SPRING_PROFILES_ACTIVE=local
# or
java -jar app.jar --spring.profiles.active=local
```

## Configuration Files

### application.yml

Default configuration for all environments.

Location: `src/main/resources/application.yml`

### application-local.yml

Local development overrides.

Location: `src/main/resources/application-local.yml`

Example:

```yaml
logging:
  level:
    uk.gov.defra.trade: DEBUG
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/trade-imports-fish-dev
```

### application-prod.yml

Production configuration.

Location: `src/main/resources/application-prod.yml`

## Key Configuration Properties

### Server

```yaml
server:
  port: 8085
  servlet:
    context-path: /
```

### MongoDB

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://user:password@host:27017/database
```

### Logging

```yaml
logging:
  level:
    root: INFO
    uk.gov.defra.trade: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

### Actuator

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

## Environment Variables

Configure via environment variables:

```bash
export SERVER_PORT=8085
export MONGO_URI=mongodb://localhost:27017/
export MONGO_DATABASE=db
export LOGGING_LEVEL_UK_GOV_DEFRA_TRADE=DEBUG
```

## Secrets Management

Sensitive data (passwords, API keys, credentials):

1. **Never commit** to version control
2. **Use environment variables** or secret management systems
3. **Use Spring Cloud Config Server** for centralized configuration in production

## Next Steps

- [Getting Started](../getting-started/index.md)
- [Running the Application](../getting-started/running.md)
