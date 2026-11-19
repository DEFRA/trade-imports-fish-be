# API Reference

## Overview

The Trade Imports Fish Backend provides REST APIs for CRUD operations on examples and health monitoring.

## Base URL

```
http://localhost:8085
```

## Endpoints

### Health Check

**Endpoint**: `GET /health`

**Description**: Check application health status.

**Response**:

```json
{
  "status": "UP"
}
```

### Actuator Endpoints

**Endpoint**: `GET /actuator`

**Description**: List available actuator endpoints.

**Response**:

```json
{
  "_links": {
    "self": {
      "href": "http://localhost:8085/actuator"
    },
    "health": {
      "href": "http://localhost:8085/actuator/health"
    },
    "metrics": {
      "href": "http://localhost:8085/actuator/metrics"
    },
    "info": {
      "href": "http://localhost:8085/actuator/info"
    }
  }
}
```

**Health Endpoint**: `GET /actuator/health`

**Metrics Endpoint**: `GET /actuator/metrics`

**Info Endpoint**: `GET /actuator/info`

## Example Resource Endpoints

The following endpoints manage Example resources (CRUD operations).

### List Examples

**Endpoint**: `GET /example`

**Description**: Retrieve all examples.

**Response**:

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

**Status Code**: `200 OK`

### Get Example by ID

**Endpoint**: `GET /example/{id}`

**Description**: Retrieve a single example by ID.

**Parameters**:

- `id` (string, path): MongoDB document ID

**Response**:

```json
{
  "id": "507f1f77bcf86cd799439011",
  "name": "Example 1",
  "value": "Value 1",
  "counter": 1,
  "created": "2024-01-01T10:30:00"
}
```

**Status Codes**:

- `200 OK` - Example found
- `404 Not Found` - Example does not exist

### Create Example

**Endpoint**: `POST /example`

**Description**: Create a new example.

**Request Body**:

```json
{
  "name": "Example 1",
  "value": "Value 1"
}
```

**Validation Rules**:

- `name`: Required, cannot be blank
- `value`: Required, cannot be blank
- `name`: Must be unique (database constraint)

**Response**:

```json
{
  "id": "507f1f77bcf86cd799439011",
  "name": "Example 1",
  "value": "Value 1",
  "counter": null,
  "created": "2024-01-01T10:30:00"
}
```

**Status Codes**:

- `201 Created` - Example created successfully
- `400 Bad Request` - Validation failed
- `409 Conflict` - Example with same name already exists

### Update Example

**Endpoint**: `PUT /example/{id}`

**Description**: Update an existing example.

**Parameters**:

- `id` (string, path): MongoDB document ID

**Request Body**:

```json
{
  "name": "Updated Example",
  "value": "Updated Value"
}
```

**Response**:

```json
{
  "id": "507f1f77bcf86cd799439011",
  "name": "Updated Example",
  "value": "Updated Value",
  "counter": null,
  "created": "2024-01-01T10:30:00"
}
```

**Status Codes**:

- `200 OK` - Example updated successfully
- `400 Bad Request` - Validation failed
- `404 Not Found` - Example does not exist
- `409 Conflict` - Another example with same name exists

### Delete Example

**Endpoint**: `DELETE /example/{id}`

**Description**: Delete an example by ID.

**Parameters**:

- `id` (string, path): MongoDB document ID

**Status Codes**:

- `204 No Content` - Example deleted successfully
- `404 Not Found` - Example does not exist

## Request/Response Format

- **Content-Type**: `application/json`
- **Character Encoding**: UTF-8

## Common Headers

### Request Headers

```
Content-Type: application/json
Accept: application/json
X-CDP-Request-ID: <trace-id>  (optional, for request tracing)
```

### Response Headers

```
Content-Type: application/json
X-CDP-Request-ID: <trace-id>  (populated from request or generated)
```

## OpenAPI/Swagger

For interactive API documentation and testing, visit:

```
http://localhost:8085/swagger-ui.html
```

Or access the OpenAPI specification at:

```
http://localhost:8085/v3/api-docs
```

## See Also

- [Error Handling](error-handling.md) - Error responses and codes
- [Authentication](authentication.md) - API security
