# Troubleshooting

## Common Issues

### Application Won't Start

**Symptom**: Application fails to start with error messages.

**Solutions**:

1. **Check Java version**:

   ```bash
   java -version
   ```

   Ensure Java 25 is installed.

2. **Check MongoDB connection**:

   ```bash
   mongosh --eval "db.version()"
   ```

3. **Check for port conflicts**:

   ```bash
   lsof -i :8085
   ```

   If port 8085 is in use, change it in `application.yml`:

   ```yaml
   server:
     port: 8086
   ```

### MongoDB Connection Failed

**Symptom**: "MongoDB connection refused" or similar error.

**Solutions**:

1. Verify MongoDB is running:

   ```bash
   docker ps | grep mongodb
   ```

2. Check MongoDB logs:

   ```bash
   docker logs <container_id>
   ```

3. Verify connection string in `application.yml`

### Memory Issues

**Symptom**: OutOfMemoryError or application crashes.

**Solutions**:

1. Increase JVM heap size:

   ```bash
   mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx2048m"
   ```

2. Check memory usage:

   ```bash
   docker stats <container_id>
   ```

### Port Already in Use

**Symptom**: "Address already in use" error.

**Solutions**:

1. Find the process using the port:

   ```bash
   lsof -i :8085
   ```

2. Kill the process:

   ```bash
   kill -9 <PID>
   ```

3. Or change the port in configuration:

   ```yaml
   server:
     port: 8086
   ```

## Debugging

### Enable Debug Logging

```bash
export LOG_LEVEL=DEBUG
mvn spring-boot:run
```

### Run Tests

```bash
mvn test
mvn clean verify
```

### Check Application Health

```bash
curl http://localhost:8085/health
```

## Getting Help

- Check the [Getting Started Guide](../getting-started/index.md)
- Review the [Configuration Guide](configuration.md)
- Check the application console output, or use `docker compose logs trade-imports-fish-be` for the containerized service
- Create an issue on [GitHub](https://github.com/DEFRA/trade-imports-fish-be/issues)
