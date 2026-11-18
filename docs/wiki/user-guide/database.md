# Database

## MongoDB Setup

The application uses MongoDB as its primary data store.

## Collections

The application manages several collections:

- **examples** - Example records with `id`, `name`, `value`, `counter`, and `created` fields

## Querying

### Connect to MongoDB

```bash
mongosh
```

### Select Database

```bash
use trade-imports-fish-be
```

### Query Collections

```bash
# List all collections
db.getCollectionNames()

# Find documents
db.examples.find()

# Count documents
db.examples.countDocuments()
```

## Data Migration

Spring Data MongoDB automatically creates the declared indexes (`spring.data.mongodb.auto-index-creation: true`). No automatic data/schema migration mechanism is currently implemented; data migrations require a separate process.

## Backup and Recovery

### Backup MongoDB

```bash
mongodump --db trade-imports-fish-be --out ./backup
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
