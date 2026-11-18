# Authentication

## Overview

API endpoints require authentication using Bearer tokens.

## Obtaining a Token

Authentication tokens can be obtained through:

1. **OAuth2/OIDC Provider**
2. **API Key management system**
3. **Service account credentials**

## Using Bearer Tokens

Include the token in the Authorization header:

```bash
curl -H "Authorization: Bearer <your_token>" \
  http://localhost:8085/example
```

## Token Expiration

Tokens have an expiration time. When a token expires:

1. Re-authenticate to obtain a new token
2. Use refresh tokens if available

## Security Considerations

- **Never commit tokens** to version control
- **Use environment variables** to store tokens
- **Rotate tokens regularly** for service accounts
- **Use HTTPS** for all requests

## Example: Using Environment Variables

```bash
export API_TOKEN=your_token_here

curl -H "Authorization: Bearer $API_TOKEN" \
  http://localhost:8085/example
```

## Common Authentication Errors

| Status | Error | Cause |
| -------- | ------- | ------- |
| 401 | Unauthorized | Missing or invalid token |
| 403 | Forbidden | Insufficient permissions |
| 400 | Bad Request | Invalid token format |

## Next Steps

- [API Reference](reference.md)
- [Error Handling](error-handling.md)
