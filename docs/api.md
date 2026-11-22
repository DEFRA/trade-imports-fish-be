# API & Routes

## REST API Overview

The service exposes a REST API for CRUD operations on Example entities. All endpoints follow REST conventions with proper HTTP methods and status codes.

**Base URL**: `http://localhost:8085`

**Authentication**: None (scope: example CRUD only)

**Content-Type**: Requests and successful responses with a body use `application/json`. Errors handled by the service's exception advice use `application/problem+json`; `DELETE` success responses have no body.

**Documentation**: OpenAPI 3.0 Swagger available at `/swagger-ui.html` or `/v3/api-docs`

## Endpoints

### Create Example

Create a new example with a unique name.

**Request**:

```
POST /example
Content-Type: application/json

{
  "name": "example-1",
  "value": "some value",
  "counter": 0
}
```

**Response** (201 Created):

```json
{
  "id": "507f1f77bcf86cd799439011",
  "name": "example-1",
  "value": "some value",
  "counter": 0,
  "created": "2024-11-21T13:16:51"
}
```

**Error Responses**:

| Status | Condition                                  |
| ------ | ------------------------------------------ |
| `400`  | Validation failed (missing/invalid fields) |
| `409`  | Example with same name already exists      |
| `500`  | Server error                               |

**Validation**:

- `name` required, non-blank
- `value` required, non-blank
- `counter` optional, integer

---

### List All Examples

Retrieve all examples.

**Request**:

```
GET /example
```

**Response** (200 OK):

```json
[
  {
    "id": "507f1f77bcf86cd799439011",
    "name": "example-1",
    "value": "some value",
    "counter": 0,
    "created": "2024-11-21T13:16:51"
  },
  {
    "id": "507f1f77bcf86cd799439012",
    "name": "example-2",
    "value": "another value",
    "counter": 5,
    "created": "2024-11-21T13:17:00"
  }
]
```

**Metrics**: Counter `controller.getAllExampleEntities.count` incremented on each call.

---

### Get Example by ID

Retrieve a single example by MongoDB ObjectId.

**Request**:

```
GET /example/507f1f77bcf86cd799439011
```

**Response** (200 OK):

```json
{
  "id": "507f1f77bcf86cd799439011",
  "name": "example-1",
  "value": "some value",
  "counter": 0,
  "created": "2024-11-21T13:16:51"
}
```

**Error Responses**:

| Status | Condition         |
| ------ | ----------------- |
| `404`  | Example not found |
| `500`  | Server error      |

---

### Update Example

Update an existing example. All fields are updated; partial updates not supported.

**Request**:

```
PUT /example/507f1f77bcf86cd799439011
Content-Type: application/json

{
  "name": "example-1-updated",
  "value": "updated value",
  "counter": 10
}
```

**Response** (200 OK):

```json
{
  "id": "507f1f77bcf86cd799439011",
  "name": "example-1-updated",
  "value": "updated value",
  "counter": 10,
  "created": "2024-11-21T13:16:51"
}
```

**Error Responses**:

| Status | Condition                               |
| ------ | --------------------------------------- |
| `400`  | Validation failed                       |
| `404`  | Example not found                       |
| `409`  | New name conflicts with another example |
| `500`  | Server error                            |

**Notes**:

- If renaming to an existing name and it's a different example, returns 409
- Renaming to the same name (idempotent) succeeds
- `created` timestamp is preserved

---

### Delete Example

Delete an example by ID.

**Request**:

```
DELETE /example/507f1f77bcf86cd799439011
```

**Response** (204 No Content):

```
(empty body)
```

**Error Responses**:

| Status | Condition         |
| ------ | ----------------- |
| `404`  | Example not found |
| `500`  | Server error      |

---

## Actuator Endpoints

The service exposes actuator endpoints for health, metrics, and service information.

| Endpoint                | Purpose                         | Access |
| ----------------------- | ------------------------------- | ------ |
| `GET /health`           | Service health (liveness probe) | Public |
| `GET /metrics`          | Available metrics list          | Public |
| `GET /metrics/{metric}` | Specific metric value           | Public |
| `GET /info`             | Service version & environment   | Public |

**Health Response** (200 OK; public response):

```json
{
  "status": "UP"
}
```

The default public response contains only `status`; the integration test verifies that component and detail fields are absent. The local profile changes the detail setting to `when-authorized`. MongoDB health checks are disabled by default (`management.health.mongo.enabled: false`), so the response does not confirm database connectivity.

---

## Example Clients

### cURL

**Create**:

```bash
curl -X POST http://localhost:8085/example \
  -H "Content-Type: application/json" \
  -d '{"name":"test-1","value":"hello"}'
```

**List**:

```bash
curl http://localhost:8085/example
```

**Get by ID**:

```bash
curl http://localhost:8085/example/507f1f77bcf86cd799439011
```

**Update**:

```bash
curl -X PUT http://localhost:8085/example/507f1f77bcf86cd799439011 \
  -H "Content-Type: application/json" \
  -d '{"name":"test-1","value":"world"}'
```

**Delete**:

```bash
curl -X DELETE http://localhost:8085/example/507f1f77bcf86cd799439011
```

### HTTPie

**Create**:

```bash
http POST localhost:8085/example name="test-1" value="hello"
```

**List**:

```bash
http localhost:8085/example
```

**Update**:

```bash
http PUT localhost:8085/example/507f1f77bcf86cd799439011 name="test-1" value="world"
```

---

## HTTP Status Codes

The API uses standard HTTP status codes:

| Code  | Meaning                                             |
| ----- | --------------------------------------------------- |
| `200` | OK — request succeeded                              |
| `201` | Created — resource created successfully             |
| `204` | No Content — delete succeeded                       |
| `400` | Bad Request — validation failed                     |
| `404` | Not Found — resource does not exist                 |
| `409` | Conflict — resource already exists or name conflict |
| `500` | Internal Server Error — unexpected server error     |

---

## Error Response Format

Exceptions handled by `GlobalExceptionHandler` (validation, domain errors, and caught runtime exceptions) use RFC 7807 ProblemDetail format. Framework-generated errors, such as a missing route, may use a different response format:

```json
{
  "type": "https://api.cdp.defra.cloud/problems/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed for one or more fields",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "errors": {
    "name": "Name is required",
    "value": "Value is required"
  }
}
```

When present, **trace ID** helps correlate logs across the system (see [Error Handling](error-handling.md)).

---

## Request Tracing

All requests include automatic trace ID population (see [Filters & Middleware](filters.md)):

1. If `x-cdp-request-id` header present, used as trace ID
2. Otherwise, trace ID left empty
3. MDC context includes: trace ID, HTTP method, request URL, response status

**Example with trace ID**:

```bash
curl http://localhost:8085/example \
  -H "x-cdp-request-id: 550e8400-e29b-41d4-a716-446655440000"
```

The service does not generate trace IDs. Logs and error responses include the ID only when the request supplies a nonblank value in the configured tracing header.
