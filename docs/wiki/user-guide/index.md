# User Guide

This section provides guidance on using and working with the Trade Imports Fish Backend.

## Available Guides

- **[API Usage](api-usage.md)** - How to use the REST API
- **[Database](database.md)** - Working with MongoDB
- **[Authentication](authentication.md)** - Understanding security and access control
- **[Configuration](configuration.md)** - Configuring the application

## Common Tasks

### Check Application Health

```bash
curl http://localhost:8085/health
```

### Access Actuator Endpoints

```bash
curl http://localhost:8085/actuator
```

### Query MongoDB

```bash
mongosh
> use trade-imports-fish
> db.collections()
```

## Getting Help

For issues or questions, refer to the [Contributing Guide](../contributing/index.md) or check the [Troubleshooting Guide](troubleshooting.md).
