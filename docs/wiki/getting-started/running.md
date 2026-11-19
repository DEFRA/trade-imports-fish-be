# Running the Application

## Starting the Services

### With Docker Compose (Full Setup)

To start all services including MongoDB and Floci:

```bash
docker compose --profile services up --build -d
```

Or with the infra profile only:

```bash
docker compose --profile infra up -d
```

### Running the Spring Boot Application

#### From Command Line

```bash
mvn spring-boot:run
```

#### From Your IDE

Import the project into your IDE (IntelliJ IDEA, Eclipse, VS Code) and run the `main` method in `uk.gov.defra.trade.imports.fish.be.Application`.

#### As a JAR File

Build and run:

```bash
mvn clean package
java -jar target/trade-imports-fish-be-*.jar
```

## Application Startup

Once started, the application will:

1. Connect to MongoDB
2. Run database migrations
3. Expose the REST API

By default:

- **Base URL**: `http://localhost:8085`
- **Health Check**: `http://localhost:8085/health`
- **Actuator**: `http://localhost:8085/actuator`

## Verifying the Application

Check if the application is running:

```bash
curl http://localhost:8085/health
```

You should see a response like:

```json
{
  "status": "UP"
}
```

## Accessing MongoDB

### Via MongoDB Shell

```bash
mongosh
```

### Via Docker

If using Docker:

```bash
docker exec -it <container_id> mongosh
```

## Stopping Services

### Stop Spring Boot

If running from the command line, press `Ctrl+C`.

### Stop Docker Services

```bash
docker compose down
```

## Troubleshooting

### Application Won't Start

- **MongoDB not running**: Ensure MongoDB is started before running the application
- **Port 8085 in use**: Change the port in `application.yml` or kill the process using port 8085
- **Java version mismatch**: Verify Java 25 is installed and set as default

### MongoDB Connection Issues

- Check MongoDB logs: `docker logs <container_id>`
- Verify connection string in `application.yml`
- Ensure write concern and read preference settings match your deployment (default: majority/secondary)

## Next Steps

- [API Documentation](../api/index.md)
- [Configuration](../configuration/index.md)
- [Testing](../contributing/quality/index.md)
