# Wallet Hub - Setup Guide

This guide walks you through setting up the Wallet Hub development environment from scratch.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Database Setup](#database-setup)
- [Kafka Setup](#kafka-setup)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Docker Compose Usage](#docker-compose-usage)
- [IDE Setup](#ide-setup)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required

- **Java Development Kit (JDK) 24+**
  - GraalVM CE 25 recommended (required for native compilation)
  - Download: [Oracle JDK 24](https://www.oracle.com/java/technologies/downloads/) or [GraalVM](https://www.graalvm.org/downloads/)
  - Verify installation:
    ```bash
    java -version
    # Expected output: java version "24" or higher
    ```

- **Maven 3.x**
  - The project includes Maven Wrapper (`./mvnw`), so a system-wide Maven installation is optional
  - If installing manually: [Maven Download](https://maven.apache.org/download.cgi)
  - Verify installation:
    ```bash
    mvn -version
    # Expected output: Apache Maven 3.x
    ```

### Optional (but recommended)

- **Docker Desktop** or **Docker Engine**
  - Required for running MongoDB, PostgreSQL, Redis, and Kafka locally
  - Download: [Docker Desktop](https://www.docker.com/products/docker-desktop/)
  - Verify installation:
    ```bash
    docker --version
    docker compose version
    ```

- **Apache Kafka** (if not using Docker)
  - Download: [Apache Kafka](https://kafka.apache.org/downloads)
  - Required for event-driven messaging
  - Default broker: `localhost:9092`

- **PostgreSQL** (if not using Docker or H2)
  - Download: [PostgreSQL](https://www.postgresql.org/download/)
  - Default port: `5432`

---

## Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd bloco-wallet-java
```

### 2. Verify Project Structure

```bash
ls -la
# You should see: pom.xml, mvnw, src/, compose.yaml, etc.
```

### 3. Build the Project

Skip tests on first build to verify dependencies download correctly:

```bash
./mvnw clean package -DskipTests
```

**Expected output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: 2-5 minutes (first build downloads dependencies)
```

### 4. Verify Build Artifacts

```bash
ls -la target/
# You should see: wallet-hub-0.0.1-SNAPSHOT.jar
```

---

## Database Setup

Wallet Hub supports multiple database configurations. Choose one based on your needs.

### Option 1: H2 File Database (Default - No Setup Required)

The application uses H2 by default with file-based persistence.

**Configuration (application.yml):**
```yaml
spring:
  datasource:
    url: jdbc:h2:file:./db/wallet;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.dialect.H2Dialect
```

**Access H2 Console:**
```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./db/wallet
Username: sa
Password: (leave empty)
```

**Database Location:**
- Files are stored in `./db/wallet.mv.db` and `./db/wallet.trace.db`
- Automatically created on first run

### Option 2: PostgreSQL (Production-Ready)

#### Using Docker Compose

Start PostgreSQL via the included `compose.yaml`:

```bash
docker compose up -d postgres
```

**Connection Details:**
```
Host: localhost
Port: 5432 (dynamically assigned, check with `docker compose ps`)
Database: mydatabase
Username: myuser
Password: secret
```

#### Manual PostgreSQL Setup

1. Install PostgreSQL locally
2. Create database and user:
   ```sql
   CREATE DATABASE wallet;
   CREATE USER walletuser WITH ENCRYPTED PASSWORD 'walletpass';
   GRANT ALL PRIVILEGES ON DATABASE wallet TO walletuser;
   ```

#### Configure Application

Create `src/main/resources/application-postgres.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/wallet
    username: walletuser
    password: walletpass
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    properties:
      hibernate:
        jdbc:
          lob:
            non_contextual_creation: true
```

**Run with PostgreSQL profile:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

**Or use environment variables:**
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/wallet
export SPRING_DATASOURCE_USERNAME=walletuser
export SPRING_DATASOURCE_PASSWORD=walletpass
./mvnw spring-boot:run
```

### Option 3: In-Memory H2 (Testing Only)

Useful for quick tests without persistence:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
```

---

## Kafka Setup

Wallet Hub uses Apache Kafka for event-driven messaging with Spring Cloud Stream.

### Option 1: Use Test Binder (No Kafka Required)

For local development and testing without real Kafka:

**Environment Variable:**
```bash
export SPRING_CLOUD_STREAM_DEFAULTBINDER=test
./mvnw spring-boot:run
```

**Or in application.yml:**
```yaml
spring:
  cloud:
    stream:
      defaultBinder: test
```

This routes messages in-memory using `spring-cloud-stream-test-binder` (already included in dependencies).

### Option 2: Local Kafka with Docker

#### Using Docker Compose

Add Kafka to `compose.yaml` or use a separate Kafka Docker setup:

**Quick Kafka Setup (via Confluent):**
```bash
docker run -d \
  --name kafka \
  -p 9092:9092 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  confluentinc/cp-kafka:latest
```

**Verify Kafka is running:**
```bash
docker logs kafka
# Should show "Kafka Server started"
```

#### Manual Kafka Installation

1. Download Kafka from [Apache Kafka Downloads](https://kafka.apache.org/downloads)
2. Extract archive:
   ```bash
   tar -xzf kafka_2.13-3.x.x.tgz
   cd kafka_2.13-3.x.x
   ```

3. Start Zookeeper:
   ```bash
   bin/zookeeper-server-start.sh config/zookeeper.properties
   ```

4. Start Kafka broker (in separate terminal):
   ```bash
   bin/kafka-server-start.sh config/server.properties
   ```

5. Verify broker:
   ```bash
   bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092
   ```

### Kafka Topic Configuration

Topics are created automatically by Spring Cloud Stream on first message. No manual topic creation needed.

**Default Topics (from application.yml):**
- `wallet-created-topic`
- `funds-added-topic`
- `funds-withdrawn-topic`
- `funds-transferred-topic`

**To manually create topics (optional):**
```bash
# Using Docker
docker exec -it kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic wallet-created-topic \
  --partitions 3 \
  --replication-factor 1

# Using local Kafka
bin/kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic wallet-created-topic \
  --partitions 3 \
  --replication-factor 1
```

**List topics:**
```bash
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

---

## Configuration

### Default Configuration (application.yml)

Located at `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: wallet-hub
  datasource:
    url: jdbc:h2:file:./db/wallet
    username: sa
    password:
  cloud:
    stream:
      kafka:
        binder:
          brokers: localhost:9092
```

### Environment Variable Overrides

All Spring properties can be overridden via environment variables:

```bash
# Database
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/wallet
export SPRING_DATASOURCE_USERNAME=myuser
export SPRING_DATASOURCE_PASSWORD=secret

# Kafka
export SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS=kafka.example.com:9092

# Application
export SPRING_APPLICATION_NAME=wallet-hub-prod
export SERVER_PORT=8081
```

### Profile-Based Configuration

Create profile-specific configuration files:

**application-dev.yml** (development):
```yaml
spring:
  jpa:
    show-sql: true
  h2:
    console:
      enabled: true
logging:
  level:
    dev.bloco.wallet.hub: DEBUG
```

**application-prod.yml** (production):
```yaml
spring:
  jpa:
    show-sql: false
  h2:
    console:
      enabled: false
logging:
  level:
    dev.bloco.wallet.hub: INFO
```

**Activate profile:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# Or via environment
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

### Important Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `spring.datasource.url` | `jdbc:h2:file:./db/wallet` | Database JDBC URL |
| `spring.cloud.stream.kafka.binder.brokers` | `localhost:9092` | Kafka broker addresses |
| `spring.cloud.stream.defaultBinder` | `kafka` | Binder type (kafka/test) |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema management (create/update/validate) |
| `wallet.networks.chainlist-url` | `https://chainlist.org/rpcs.json` | Blockchain network source |
| `wallet.networks.cache-ttl` | `PT5M` | Network cache duration (5 minutes) |

---

## Running the Application

### Method 1: Maven Wrapper (Recommended)

```bash
./mvnw spring-boot:run
```

**With profile:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**With arguments:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### Method 2: Packaged JAR

```bash
# Build JAR
./mvnw clean package -DskipTests

# Run JAR
java -jar target/wallet-hub-0.0.1-SNAPSHOT.jar
```

**With profile:**
```bash
java -jar target/wallet-hub-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Method 3: Docker Image (JVM)

Build Docker image using Spring Boot buildpacks:

```bash
./mvnw spring-boot:build-image
```

**Run container:**
```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/wallet \
  wallet-hub:0.0.1-SNAPSHOT
```

### Method 4: Native Executable (GraalVM Required)

Compile to native executable:

```bash
./mvnw native:compile -Pnative
```

**Run native image:**
```bash
./target/wallet-hub
```

Benefits:
- Fast startup (< 100ms)
- Low memory footprint (~50MB)
- No JVM required

### Verify Application Started

**Look for:**
```
Started WalletHubApplication in X.XXX seconds
```

**Health check:**
```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

**H2 Console (if enabled):**
```
http://localhost:8080/h2-console
```

---

## Docker Compose Usage

The project includes `compose.yaml` with MongoDB, PostgreSQL, and Redis services.

### Start All Services

```bash
docker compose up -d
```

**Verify services:**
```bash
docker compose ps
```

Expected output:
```
NAME                        IMAGE               STATUS
bloco-wallet-java-mongodb   mongo:latest        Up
bloco-wallet-java-postgres  postgres:latest     Up
bloco-wallet-java-redis     redis:latest        Up
```

### Start Individual Services

```bash
# PostgreSQL only
docker compose up -d postgres

# MongoDB only
docker compose up -d mongodb

# Redis only
docker compose up -d redis
```

### View Logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f postgres
```

### Stop Services

```bash
# Stop all (keeps data)
docker compose down

# Stop and remove volumes (deletes data)
docker compose down -v
```

### Connection Details

**PostgreSQL:**
```
Host: localhost
Port: Check with `docker compose ps` (dynamic)
Database: mydatabase
Username: myuser
Password: secret
```

**MongoDB:**
```
URI: mongodb://root:secret@localhost:27017/mydatabase?authSource=admin
```

**Redis:**
```
Host: localhost
Port: Check with `docker compose ps` (dynamic)
```

### Add Kafka to Docker Compose

Extend `compose.yaml` to include Kafka:

```yaml
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
```

Then:
```bash
docker compose up -d kafka
```

---

## IDE Setup

### IntelliJ IDEA (Recommended)

#### 1. Import Project

1. Open IntelliJ IDEA
2. File > Open > Select `bloco-wallet-java` directory
3. Wait for Maven import to complete

#### 2. Configure JDK

1. File > Project Structure > Project
2. SDK: Select Java 24 or GraalVM CE 25
3. Language Level: 24

#### 3. Enable Annotation Processing

Required for Lombok and MapStruct:

1. Settings > Build, Execution, Deployment > Compiler > Annotation Processors
2. Enable annotation processing
3. Obtain processors from project classpath

#### 4. Install Plugins (Optional)

- Lombok Plugin
- MapStruct Support
- Spring Boot Assistant

#### 5. Run Configuration

1. Run > Edit Configurations
2. Add New > Spring Boot
3. Main class: `dev.bloco.wallet.hub.WalletHubApplication`
4. VM options (optional): `-Dspring.profiles.active=dev`
5. Environment variables (if needed):
   ```
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/wallet
   ```

### Visual Studio Code

#### 1. Required Extensions

Install from Extensions marketplace:
- Extension Pack for Java (Microsoft)
- Spring Boot Extension Pack (VMware)
- Lombok Annotations Support for VS Code

#### 2. Configure Java

Create `.vscode/settings.json`:

```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.compile.nullAnalysis.mode": "automatic",
  "java.jdt.ls.java.home": "/path/to/java-24",
  "spring-boot.ls.java.home": "/path/to/java-24"
}
```

#### 3. Launch Configuration

Create `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Wallet Hub",
      "request": "launch",
      "mainClass": "dev.bloco.wallet.hub.WalletHubApplication",
      "projectName": "wallet-hub",
      "env": {
        "SPRING_PROFILES_ACTIVE": "dev"
      }
    }
  ]
}
```

#### 4. Run Application

Press F5 or Run > Start Debugging

### Eclipse

#### 1. Import Maven Project

1. File > Import > Existing Maven Projects
2. Select `bloco-wallet-java` directory
3. Finish

#### 2. Install Lombok

1. Download lombok.jar from [Project Lombok](https://projectlombok.org/download)
2. Run: `java -jar lombok.jar`
3. Select Eclipse installation directory
4. Install/Update
5. Restart Eclipse

#### 3. Run Configuration

1. Run > Run Configurations
2. Java Application > New
3. Main class: `dev.bloco.wallet.hub.WalletHubApplication`
4. Arguments tab > VM arguments: `-Dspring.profiles.active=dev`

---

## Troubleshooting

### Build Issues

#### Problem: Maven build fails with "Java version mismatch"

**Solution:**
```bash
# Verify Java version
java -version

# Override compiler release (temporary workaround)
./mvnw clean package -Dmaven.compiler.release=21
```

#### Problem: "Package does not exist" for MapStruct

**Solution:**
Ensure annotation processors are running:
```bash
./mvnw clean compile
```

For IDEs, enable annotation processing in settings.

#### Problem: Hibernate enhancement fails

**Solution:**
The pom.xml includes `hibernate-enhance-maven-plugin`. If it fails:
```bash
# Temporarily disable by commenting out in pom.xml, or
./mvnw clean package -Dskip.enhance=true
```

### Runtime Issues

#### Problem: Application fails to start - "Bean definition override"

**Cause:** Duplicate `@Component` and `@Bean` annotations (known issue with `FundsAddedEventConsumer`)

**Solution 1** (Recommended): Remove `@Component` from consumer classes
**Solution 2**: Enable bean overriding (not recommended for production):
```yaml
spring:
  main:
    allow-bean-definition-overriding: true
```

#### Problem: Kafka connection errors

**Solution:**
```bash
# Use test binder for local development
export SPRING_CLOUD_STREAM_DEFAULTBINDER=test
./mvnw spring-boot:run

# Or verify Kafka is running
docker logs kafka
# Check broker is on localhost:9092
```

#### Problem: H2 database locked

**Cause:** Another process is using the database file

**Solution:**
```bash
# Stop all running instances
pkill -f wallet-hub

# Or use a different database file
export SPRING_DATASOURCE_URL=jdbc:h2:file:./db/wallet2
```

#### Problem: Port 8080 already in use

**Solution:**
```bash
# Use different port
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"

# Or find and kill process
lsof -ti:8080 | xargs kill -9
```

### Database Issues

#### Problem: PostgreSQL connection refused

**Solution:**
```bash
# Check PostgreSQL is running
docker compose ps postgres
# Or
pg_isready -h localhost -p 5432

# Verify connection details
psql -h localhost -U myuser -d mydatabase
```

#### Problem: Schema validation errors

**Cause:** Database schema out of sync with entities

**Solution:**
```yaml
# Recreate schema (DEV ONLY - DELETES DATA)
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
```

Or manually drop tables and restart with `ddl-auto: update`.

### Kafka Issues

#### Problem: Topics not created automatically

**Solution:**
Create topics manually:
```bash
docker exec -it kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic wallet-created-topic \
  --partitions 3 \
  --replication-factor 1
```

#### Problem: Consumer group errors

**Solution:**
Reset consumer groups:
```bash
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group wallet-hub \
  --reset-offsets \
  --to-earliest \
  --execute \
  --all-topics
```

### IDE Issues

#### Problem: Lombok annotations not working

**Solution:**
- IntelliJ: Install Lombok plugin, enable annotation processing
- VS Code: Install Lombok extension
- Eclipse: Run lombok.jar installer

#### Problem: MapStruct mappers not generated

**Solution:**
```bash
# Generate sources
./mvnw generate-sources

# In IDE, mark target/generated-sources as source root
```

### Performance Issues

#### Problem: Slow startup time

**Typical startup:** 5-10 seconds (JVM), <100ms (native)

**Solutions:**
- Use native compilation for instant startup
- Disable unnecessary autoconfiguration:
  ```java
  @SpringBootApplication(exclude = {
      MongoAutoConfiguration.class,
      RedisAutoConfiguration.class
  })
  ```
- Use Spring DevTools for fast restarts during development

#### Problem: High memory usage

**Solution:**
Tune JVM parameters:
```bash
java -Xms256m -Xmx512m -jar target/wallet-hub-0.0.1-SNAPSHOT.jar
```

Or use native image for minimal memory footprint.

### Docker Issues

#### Problem: Docker Compose services fail to start

**Solution:**
```bash
# Check logs
docker compose logs

# Remove old containers and volumes
docker compose down -v
docker compose up -d
```

#### Problem: Cannot connect to services from host

**Cause:** Dynamic port assignment

**Solution:**
```bash
# Check assigned ports
docker compose ps

# Or use fixed ports in compose.yaml:
services:
  postgres:
    ports:
      - "5432:5432"  # host:container
```

---

## Next Steps

Once setup is complete:

1. Run tests to verify everything works:
   ```bash
   ./mvnw test
   ```

2. Explore the codebase:
   - Domain models: `src/main/java/dev/bloco/wallet/hub/domain/model/`
   - Use cases: `src/main/java/dev/bloco/wallet/hub/usecase/`
   - Events: `src/main/java/dev/bloco/wallet/hub/domain/event/`

3. Read the [Testing Guide](TESTING.md) for testing strategies

4. Check [CLAUDE.md](../../CLAUDE.md) for architecture details

---

## Support and Resources

- **Documentation:** `docs/` directory
- **Architecture Guide:** `architecture.md`
- **Spring Boot Docs:** [https://docs.spring.io/spring-boot/](https://docs.spring.io/spring-boot/)
- **Spring Cloud Stream:** [https://docs.spring.io/spring-cloud-stream/](https://docs.spring.io/spring-cloud-stream/)
- **Kafka Docs:** [https://kafka.apache.org/documentation/](https://kafka.apache.org/documentation/)

---

**Last Updated:** 2025-12-10
