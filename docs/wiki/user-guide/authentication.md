# Authentication

## Overview

The Trade Imports Fish Backend uses authentication to secure access to API endpoints.

## Authentication Methods

### 1. OAuth2 / OIDC

The application supports OAuth2 and OIDC authentication for secure user verification.

### 2. API Keys

API keys can be used for service-to-service communication.

## Bearer Token

API requests should include a Bearer token in the Authorization header:

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8085/api/example
```

## Session Management

Sessions are managed through secure cookies or JWT tokens depending on configuration.

## User Roles and Permissions

Users can have different roles with corresponding permissions:

- **ADMIN** - Full system access
- **USER** - Standard user permissions
- **VIEWER** - Read-only access

## Security Best Practices

1. **Never share credentials** - Keep your tokens and API keys confidential
2. **Rotate keys regularly** - Change API keys periodically
3. **Use HTTPS** - Always use encrypted connections
4. **Validate tokens** - Ensure tokens are valid before making requests

## Next Steps

- [API Reference](../api/reference.md)
- [Configuration](configuration.md)
