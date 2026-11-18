# Database

## MongoDB Setup

The application uses MongoDB as its primary data store.

## Collections

The application manages several collections:

- **imports** - Fish import records
- **users** - User information
- **audit_logs** - Audit trail

## Querying

### Connect to MongoDB

```bash
mongosh
```

### Select Database

```bash
use trade-imports-fish
```

### Query Collections

```bash
# List all collections
db.getCollectionNames()

# Find documents
db.imports.find()

# Count documents
db.imports.countDocuments()
```

## Data Migration

Data migrations are handled automatically on application startup using Spring Data MongoDB.

## Backup and Recovery

### Backup MongoDB

```bash
mongodump --db trade-imports-fish --out ./backup
```

### Restore MongoDB

```bash
mongorestore ./backup
```

## Performance Considerations

- **Indexes**: Ensure proper indexes are created for frequently queried fields
- **Connection pooling**: The application uses connection pooling by default
- **Batch operations**: Use bulk operations for large data migrations

## Next Steps

- [Configuration](configuration.md)
- [Troubleshooting](troubleshooting.md)
