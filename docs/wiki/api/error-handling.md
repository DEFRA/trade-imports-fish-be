# Error Handling

## Overview

The API returns standard HTTP status codes and structured error responses.

## HTTP Status Codes

| Code | Meaning | Description |
| ------ | --------- | ------------- |
| 200 | OK | Request succeeded |
| 201 | Created | Resource created successfully |
| 400 | Bad Request | Invalid request parameters |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Resource conflict (e.g., duplicate) |
| 500 | Internal Server Error | Server error occurred |
| 503 | Service Unavailable | Service temporarily unavailable |

## Error Response Format

```json
{
  "error": {
    "code": "INVALID_REQUEST",
    "message": "The request is invalid",
    "details": "Additional error details",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

## Common Errors

### 400 - Bad Request

**Cause**: Invalid request parameters or format.

**Example**:

```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Validation failed for request",
    "details": "Field 'email' is required"
  }
}
```

### 401 - Unauthorized

**Cause**: Missing or invalid authentication token.

**Solution**: Ensure you include a valid Bearer token in the Authorization header.

### 404 - Not Found

**Cause**: Requested resource does not exist.

**Example**:

```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "Resource not found",
    "details": "Import with ID '123' not found"
  }
}
```

### 500 - Internal Server Error

**Cause**: Server encountered an unexpected error.

**Solution**: Check application logs and contact support if the issue persists.

## Handling Errors in Client Code

### Example: Java

```java
try {
    response = client.get("/api/imports");
} catch (HttpClientErrorException.BadRequest e) {
    // Handle 400 error
} catch (HttpClientErrorException.Unauthorized e) {
    // Handle 401 error
} catch (HttpServerErrorException.InternalServerError e) {
    // Handle 500 error
}
```

### Example: JavaScript

```javascript
fetch('/api/imports', {
    headers: { 'Authorization': `Bearer ${token}` }
})
.then(response => {
    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.json();
})
.catch(error => console.error('Error:', error));
```

## Next Steps

- [API Reference](reference.md)
- [Authentication](authentication.md)
