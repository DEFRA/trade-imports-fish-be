# Development Setup

This guide will help you set up your local development environment for the Trade Imports Fish Backend.

## Prerequisites

Ensure you have installed:

- **Java 25** ([download](https://www.oracle.com/java/technologies/downloads/))
- **Maven 3.8.0+** ([download](https://maven.apache.org/download.cgi))
- **Docker Desktop** ([download](https://www.docker.com/products/docker-desktop))
- **Git** ([download](https://git-scm.com/downloads))

## Clone the Repository

```bash
git clone https://github.com/DEFRA/trade-imports-fish-be.git
cd trade-imports-fish-be
```

## Set Up MongoDB

The application requires MongoDB. Choose one of the following options:

### Option 1: Docker Compose (Recommended)

Start the infrastructure services:

```bash
docker compose --profile infra up -d
```

This starts:

- MongoDB
- Floci (for AWS services: S3, SQS)
- Redis

### Option 2: Docker Only

```bash
docker compose up -d mongodb floci
```

### Option 3: Local MongoDB Installation

If you prefer to install MongoDB locally:

1. [Install MongoDB](https://www.mongodb.com/docs/manual/tutorial/#installation)
2. Start MongoDB:

```bash
sudo mongod --dbpath ~/mongodb-cdp
```

## Verify Installation

Verify your setup by running:

```bash
java -version
mvn --version
docker --version
```

## Next Steps

- [Configure Git Hooks](git-hooks.md) - Set up automatic documentation updates
- [Running the Application](running.md)
- [Testing](../contributing/quality/index.md)
- [Configuration](../configuration/index.md)
