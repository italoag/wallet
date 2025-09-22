# Developer Setup

<cite>
**Referenced Files in This Document**   
- [README.md](file://README.md)
- [pom.xml](file://pom.xml)
- [mise.toml](file://mise.toml)
- [compose.yaml](file://compose.yaml)
- [application.yml](file://src/main/resources/application.yml)
- [WalletHubApplication.java](file://src/main/java/dev/bloco/wallet/hub/WalletHubApplication.java)
- [DinamoLibraryLoader.java](file://src/main/java/dev/bloco/wallet/hub/config/DinamoLibraryLoader.java)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Prerequisites](#prerequisites)
3. [Environment Setup](#environment-setup)
4. [Infrastructure Services with Docker Compose](#infrastructure-services-with-docker-compose)
5. [IDE Configuration](#ide-configuration)
6. [Building and Running the Application](#building-and-running-the-application)
7. [Common Setup Issues](#common-setup-issues)
8. [Verification Steps](#verification-steps)
9. [Development Tips](#development-tips)
10. [Conclusion](#conclusion)

## Introduction
This guide provides comprehensive instructions for setting up a local development environment for the `bloco-wallet-java` project. The application is a Spring Boot-based event-driven wallet service that uses Kafka for messaging, H2 as the default database, and supports optional infrastructure via Docker. This document covers all necessary steps to configure your environment, import the project, run services, and troubleshoot common issues.

**Section sources**
- [README.md](file://README.md#L1-L23)

## Prerequisites
Before beginning the setup, ensure the following tools are installed on your system:

- **JDK 25**: Required for compiling and running the application. GraalVM CE 25 is recommended.
- **Maven**: Use the provided Maven Wrapper (`./mvnw`) to ensure version consistency.
- **Docker**: Optional, used for running MongoDB, Postgres, and Redis via `compose.yaml`.
- **Kafka**: The application connects to Kafka at `localhost:9092` by default. A local Kafka instance or Docker-based setup is required for production-like testing.
- **mise (optional)**: For automatic toolchain management using the configuration in `mise.toml`.

The `pom.xml` specifies Java 25 as the target version, and the `mise.toml` file configures the Java toolchain to use `graalvm-community-25`, ensuring compatibility across development environments.

**Section sources**
- [README.md](file://README.md#L24-L30)
- [pom.xml](file://pom.xml#L35)
- [mise.toml](file://mise.toml#L2)

## Environment Setup
### Installing JDK 25 and Maven
1. **Install JDK 25**: Download and install a JDK 25-compatible distribution such as [GraalVM CE 25](https://www.graalvm.org/). Set the `JAVA_HOME` environment variable to point to the installation directory.
2. **Verify Installation**:
   ```bash
   java -version
   javac -version
   ```
3. **Maven**: The project includes the Maven Wrapper (`mvnw`). No separate Maven installation is required. Use `./mvnw` for all Maven commands.

### Optional: Using mise for Toolchain Management
The project includes a `mise.toml` file that specifies the Java version:
```toml
[tools]
java = "graalvm-community-25"
```
If you have [mise](https://mise.jdx.dev/) installed, run:
```bash
mise install
```
This automatically installs and activates the correct JDK version.

**Section sources**
- [README.md](file://README.md#L24-L30)
- [mise.toml](file://mise.toml#L1-L3)
- [pom.xml](file://pom.xml#L35)

## Infrastructure Services with Docker Compose
The `compose.yaml` file defines optional infrastructure services:
- MongoDB (port 27017)
- Postgres (port 5432)
- Redis (port 6379)

> **Note**: Host port mappings are not defined by default. To access services from the host, modify `compose.yaml` to include explicit port mappings (e.g., `"5432:5432"`).

### Starting Services
Run the following command to start all services in detached mode:
```bash
docker compose up -d
```

### Stopping Services
To stop and remove containers:
```bash
docker compose down
```

These services are optional. The application defaults to H2 file-based storage, but you can override the datasource using environment variables like `SPRING_DATASOURCE_URL`.

**Section sources**
- [README.md](file://README.md#L85-L98)
- [compose.yaml](file://compose.yaml#L1-L22)

## IDE Configuration
### IntelliJ IDEA
1. **Import Project**: Open the project directory and import as a Maven project.
2. **Enable Annotation Processing**:
   - Go to `Settings > Build > Compiler > Annotation Processors`.
   - Check `Enable annotation processing`.
3. **Lombok Support**:
   - Install the Lombok plugin from the marketplace.
   - Ensure `Preferences > Plugins > Lombok` is enabled.
4. **Spring Boot Plugin**: Install the Spring Boot plugin for enhanced code navigation and run configuration support.

### VS Code
1. **Install Extensions**:
   - **Java Extension Pack**
   - **Lombok Annotations Support**
   - **Spring Boot Tools**
2. **Configure Annotation Processing**:
   - Ensure `spring-boot-devtools` is present (it is in `pom.xml`).
   - Annotation processing is automatically handled by the Java Language Server.
3. **Run Configuration**:
   - Use the `Java: Create Run Configuration` command to set up a launch configuration for `WalletHubApplication`.

The application uses Lombok for boilerplate code generation and MapStruct for DTO mapping, both of which require annotation processing to be enabled.

**Section sources**
- [pom.xml](file://pom.xml#L198-L221)
- [pom.xml](file://pom.xml#L360-L390)
- [WalletHubApplication.java](file://src/main/java/dev/bloco/wallet/hub/WalletHubApplication.java#L23-L35)

## Building and Running the Application
### Build the Application
To compile and package the application (skip tests):
```bash
./mvnw -DskipTests package
```

### Run Locally
Start the application using Spring Boot:
```bash
./mvnw spring-boot:run
```

### Test Goals
- Run all tests: `./mvnw test`
- Run a single test: `./mvnw -Dtest=FullyQualifiedTestName test`
- Override compiler release for older JDKs: `./mvnw -Dmaven.compiler.release=8 -Dtest=dev.bloco.wallet.hub.DemoSanityTest test`

### Native Image (Optional)
If GraalVM `native-image` is installed:
- Build native executable: `./mvnw native:compile`
- Build native container image: `./mvnw spring-boot:build-image -Pnative`

**Section sources**
- [README.md](file://README.md#L58-L75)
- [pom.xml](file://pom.xml#L390-L422)

## Common Setup Issues
### Port Conflicts
- **H2 Console**: Available at `http://localhost:8080/h2-console`. If port 8080 is in use, set `SERVER_PORT` environment variable.
- **Kafka**: Ensure Kafka is running on `localhost:9092` or update `spring.cloud.stream.kafka.binder.brokers` in `application.yml`.

### Missing Native Libraries
The `DinamoLibraryLoader` loads OS-specific native libraries from `libs/windows` or `libs/linux`. Ensure:
- The `libs` directory exists with correct subdirectories.
- Required files are present:
  - Windows: `tacndlib.dll`, `tacndjavalib.dll`
  - Linux: `libtacndlib.so`, `libtacndjavalib.so`
- If files are missing, contact the project maintainer or check documentation for HSM integration.

### Dependency Resolution
Ensure Maven can download dependencies. If behind a corporate proxy, configure `settings.xml` accordingly. Use `./mvnw dependency:resolve` to verify.

**Section sources**
- [README.md](file://README.md#L117-L128)
- [DinamoLibraryLoader.java](file://src/main/java/dev/bloco/wallet/hub/config/DinamoLibraryLoader.java#L1-L118)

## Verification Steps
After setup, verify the environment:
1. **Application Startup**: `./mvnw spring-boot:run` should start without errors.
2. **H2 Console Access**: Navigate to `http://localhost:8080/h2-console` and connect using:
   - JDBC URL: `jdbc:h2:file:./db/wallet`
   - Username: `sa`
   - Password: (empty)
3. **Check Logs**: Confirm "Started WalletHubApplication" appears in logs.
4. **Docker Services**: Run `docker ps` to verify MongoDB, Postgres, and Redis containers are running.
5. **Native Build (if applicable)**: Run `./mvnw native:compile` and execute the resulting binary.

**Section sources**
- [application.yml](file://src/main/resources/application.yml#L1-L35)
- [README.md](file://README.md#L65-L70)

## Development Tips
### Debugging
- Attach a debugger to the `./mvnw spring-boot:run` process via IDE.
- Use `logging.level.dev.bloco.wallet=DEBUG` in `application.yml` for detailed logs.

### Hot-Reloading
With `spring-boot-devtools` enabled, code changes trigger automatic restarts. Disable in production.

### Profiling
- Enable Actuator endpoints: `management.endpoints.web.exposure.include=*`
- Access `/actuator/prometheus` for metrics.
- Use `/actuator/health` for health checks.

### Testing Messaging
Use the Spring Cloud Stream test binder:
- Set `spring.cloud.stream.defaultBinder=test` in test properties.
- Use `InputDestination` and `OutputDestination` for message simulation.

**Section sources**
- [pom.xml](file://pom.xml#L198-L203)
- [application.yml](file://src/main/resources/application.yml#L1-L35)

## Conclusion
This guide provides a complete setup workflow for the `bloco-wallet-java` project. By following these steps, developers can quickly configure their environment, run the application, and begin contributing. Always refer to the `README.md` for the latest updates and use the Maven Wrapper to maintain build consistency. For advanced features like native compilation or HSM integration, ensure all prerequisites are met and consult the relevant documentation.