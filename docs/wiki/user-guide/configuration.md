# Configuration

## Configuration Files

The application uses YAML configuration files:

- `application.yml` - Default configuration
- `application-local.yml` - Local development configuration
- `application-prod.yml` - Production configuration

## Active Profiles

Set the active profile using:

```bash
export SPRING_PROFILES_ACTIVE=local
# or
java -jar app.jar --spring.profiles.active=local
```

## Common Configuration Properties

### Server Configuration

```yaml
server:
  port: 8085
  servlet:
    context-path: /
```

### MongoDB Configuration

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/trade-imports-fish
```

### Logging Configuration

```yaml
logging:
  level:
    root: INFO
    uk.gov.defra.trade: DEBUG
```

## Environment Variables

Configuration can also be provided via environment variables:

```bash
export SERVER_PORT=8085
export SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/trade-imports-fish
```

## External Configuration

For sensitive data (passwords, API keys), use:

- Environment variables
- Secrets management systems
- Configuration servers

## Next Steps

- [API Reference](../api/reference.md)
- [Database](database.md)
