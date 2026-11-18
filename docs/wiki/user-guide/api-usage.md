# API Usage

## Base URL

```
http://localhost:8085
```

## Available Endpoints

The application exposes several REST API endpoints. Refer to the [API Documentation](../api/index.md) for detailed information.

### Health Check

```bash
GET /health
```

Response:

```json
{
  "status": "UP"
}
```

### Actuator

```bash
GET /actuator
```

Returns available actuator endpoints.

## Authentication

All endpoints (except health checks) require proper authentication. See [Authentication](authentication.md) for details.

## Request/Response Format

- **Content-Type**: `application/json`
- **Character Encoding**: UTF-8

## Error Handling

The API returns appropriate HTTP status codes:

- **200**: Success
- **400**: Bad Request
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found
- **500**: Internal Server Error

See [Error Handling](../api/error-handling.md) for more details.

## Rate Limiting

Rate limits may be applied to certain endpoints. Check response headers for rate limit information.

## Next Steps

- [API Reference](../api/reference.md)
- [Authentication](authentication.md)
