# Deployment Guide

This guide covers deploying the Trade Imports Fish Backend to different environments.

## Local Development

For local development, refer to [Running the Application](running.md).

## Docker Deployment

### Build Docker Image

```bash
docker build -t trade-imports-fish-be:0.1.0 .
```

## Docker Compose Deployment

The project includes a compose configuration for complete deployment:

```bash
docker compose up -d
```

This starts:

- Application (port 8085)
- MongoDB
- Floci (AWS S3/SQS emulator)
- Redis (optional)

## Environment Configuration

### Required Environment Variables

| Variable | Default | Description |
| ---------- | --------- | ------------- |
| `PORT` | 8085 | Server port |
| `MONGO_URI` | `mongodb://localhost:27017/` | MongoDB connection string |
| `MONGO_DATABASE` | `trade-imports-fish-be` | Database name |

### Optional Environment Variables

| Variable | Default | Description |
| ---------- | --------- | ------------- |
| `AWS_REGION` | `eu-west-2` | AWS region |
| `AWS_EMF_ENABLED` | `true` | CloudWatch EMF metrics |
| `LOG_LEVEL` | `info` | Root logging level |
| `TRACING_HEADER` | `x-cdp-request-id` | Trace ID header |

## Health Checks

Configure your orchestrator to use this health endpoint:

```bash
curl http://localhost:8085/health
```

Response:

```json
{
  "status": "UP"
}
```

## Production Deployment Best Practices

### Database

- Use managed MongoDB (AWS DocumentDB, MongoDB Atlas)
- Enable SSL/TLS connections
- Configure proper backups
- Use replica sets for high availability

### Application

- Set `SPRING_PROFILES_ACTIVE=prod`
- Configure appropriate logging level
- Enable CloudWatch EMF metrics
- Use multi-instance deployment with load balancer

### Security

- Use HTTPS/TLS
- Manage secrets with AWS Secrets Manager
- Use IAM roles for AWS service access
- Enable request tracing with correlation IDs

## Troubleshooting

### Application fails to start

Check logs:

```bash
docker logs <container_id>
```

Verify MongoDB connection:

```bash
mongodb-shell mongodb://host:27017
```

### High memory usage

Adjust JVM heap settings:

```bash
export JAVA_OPTS="-Xmx1g -Xms512m"
```

### Slow requests

Enable debug logging:

```bash
export LOG_LEVEL=debug
```

Check metrics:

```bash
curl http://localhost:8085/metrics
```

## See Also

- [Running the Application](running.md)
- [Configuration](../configuration/index.md)
- [Architecture](../architecture/index.md)
