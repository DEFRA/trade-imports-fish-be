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
  "type": "https://api.cdp.defra.cloud/problems/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed for one or more fields",
  "errors": { "name": "Name is required" }
}
```

## Common Errors

### 400 - Bad Request

**Cause**: Invalid request parameters or format.

**Example**:

```json
{
  "type": "https://api.cdp.defra.cloud/problems/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed for one or more fields",
  "errors": { "name": "Name is required" }
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
  "type": "https://api.cdp.defra.cloud/problems/not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Example not found with id: 123"
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
