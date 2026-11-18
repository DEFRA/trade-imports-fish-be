# API Documentation

## Overview

The Trade Imports Fish Backend provides REST APIs for managing example records, health monitoring, and application metrics.

## API Structure

- **Base URL**: `http://localhost:8085`
- **Content-Type**: `application/json`
- **Response Format**: JSON

## Health & Monitoring

- **Health Check**: `GET /health`
- **Actuator Endpoints**: `GET /actuator/*` (health, metrics, info, cache)
- **Swagger UI**: `http://localhost:8085/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8085/v3/api-docs`

## Example Resource API

CRUD operations for managing example records:

- **[API Reference](reference.md)** - Complete endpoint documentation
- **List Examples**: `GET /example`
- **Get Example**: `GET /example/{id}`
- **Create Example**: `POST /example`
- **Update Example**: `PUT /example/{id}`
- **Delete Example**: `DELETE /example/{id}`

## Documentation

- **[API Reference](reference.md)** - Complete endpoint documentation with examples
- **[Authentication](authentication.md)** - API authentication and security
- **[Error Handling](error-handling.md)** - Error responses and HTTP status codes

## Using the API

### Example Request: Create Example

```bash
curl -X POST http://localhost:8085/example \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Example",
    "value": "Example Value"
  }'
```

### Example Response

```json
{
  "id": "507f1f77bcf86cd799439011",
  "name": "My Example",
  "value": "Example Value",
  "counter": null,
  "created": "2024-01-01T10:30:00"
}
```

### Example Request: List Examples

```bash
curl -X GET http://localhost:8085/example \
  -H "Content-Type: application/json"
```

### Example Response

```json
[
  {
    "id": "507f1f77bcf86cd799439011",
    "name": "Example 1",
    "value": "Value 1",
    "counter": 1,
    "created": "2024-01-01T10:30:00"
  }
]
```

## Request Tracing

All requests are automatically traced for debugging:

```bash
curl -X GET http://localhost:8085/health \
  -H "X-CDP-Request-ID: my-trace-123"
```

The trace ID is propagated through all logs and can be found in:

- Request/Response headers
- Application logs (ECS format)
- CloudWatch metrics (if AWS EMF enabled)

## Interactive API Testing

Use Swagger UI for interactive testing:

1. Navigate to: `http://localhost:8085/swagger-ui.html`
2. Browse available endpoints
3. Test requests directly from the UI
4. View sample requests and responses

## Rate Limiting

Currently, no rate limiting is configured. Contact operations for production deployment rate limit policies.

## Pagination

Currently, list endpoints return all results. For large datasets, consider:

- Using filtering in future versions
- Implementing pagination in the service layer
- Caching frequently accessed data

## Next Steps

- [API Reference](reference.md) - Full endpoint details
- [Error Handling](error-handling.md) - Error response formats
- [Authentication](authentication.md) - API security setup
