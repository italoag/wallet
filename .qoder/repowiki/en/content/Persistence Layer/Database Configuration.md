# Database Configuration

<cite>
**Referenced Files in This Document**   
- [application.yml](file://src/main/resources/application.yml)
- [pom.xml](file://pom.xml)
- [README.md](file://README.md)
- [JpaTransactionRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaTransactionRepository.java)
- [JpaWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaWalletRepository.java)
- [SpringDataTransactionRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/SpringDataTransactionRepository.java)
- [SpringDataWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/SpringDataWalletRepository.java)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Core Database Configuration](#core-database-configuration)
3. [JPA and Hibernate Settings](#jpa-and-hibernate-settings)
4. [H2 Console Access](#h2-console-access)
5. [Environment-Specific Configuration](#environment-specific-configuration)
6. [Connection and Dependency Management](#connection-and-dependency-management)
7. [Caching Configuration](#caching-configuration)
8. [Production Database Migration](#production-database-migration)
9. [Security and Best Practices](#security-and-best-practices)

## Introduction

The bloco-wallet-java application utilizes Spring Boot's auto-configuration capabilities to manage its database layer through declarative configuration in `application.yml`. The system is designed for development convenience with H2 as the default embedded database while maintaining flexibility for production deployment with external databases like PostgreSQL. This documentation details the complete database configuration strategy, including datasource settings, JPA/Hibernate behavior, H2 console access, and environment-specific overrides.

**Section sources**
- [application.yml](file://src/main/resources/application.yml#L1-L34)
- [README.md](file://README.md#L58-L90)

## Core Database Configuration

The application's datasource configuration is defined in `application.yml` with H2 as the default database. The configuration includes the JDBC URL, driver specification, and authentication credentials.

```mermaid
flowchart TD
A["Datasource Configuration"] --> B["URL: jdbc:h2:file:./db/wallet"]
A --> C["Driver: org.h2.Driver"]
A --> D["Username: sa"]
A --> E["Password: (empty)"]
A --> F["File-based persistence"]
B --> G["DB_CLOSE_ON_EXIT=FALSE"]
B --> H["AUTO_RECONNECT=TRUE"]
```

**Diagram sources**
- [application.yml](file://src/main/resources/application.yml#L10-L14)

The datasource settings specify a file-based H2 database stored in the local `./db/wallet` directory, ensuring data persistence between application restarts. The `DB_CLOSE_ON_EXIT=FALSE` parameter prevents the database from closing when the last connection is closed, while `AUTO_RECONNECT=TRUE` enables automatic reconnection if the connection is lost.

**Section sources**
- [application.yml](file://src/main/resources/application.yml#L10-L14)

## JPA and Hibernate Settings

The application configures JPA and Hibernate through Spring Boot properties that control schema management, SQL logging, and dialect specification.

```mermaid
classDiagram
class JpaConfiguration {
+String hibernate.ddl-auto = "update"
+boolean show-sql = true
+String database-platform = "org.hibernate.dialect.H2Dialect"
}
class RepositoryInterface {
<<interface>>
+save(entity)
+findById(id)
+findAll()
+delete(id)
}
class JpaImplementation {
-repository : RepositoryInterface
-mapper : EntityMapper
+save(domain)
+findById(id)
+findAll()
}
JpaConfiguration --> JpaImplementation : "configures"
RepositoryInterface <|-- SpringDataTransactionRepository : "implements"
RepositoryInterface <|-- SpringDataWalletRepository : "implements"
JpaImplementation --> RepositoryInterface : "uses"
```

**Diagram sources**
- [application.yml](file://src/main/resources/application.yml#L20-L23)
- [SpringDataTransactionRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/SpringDataTransactionRepository.java#L15-L23)
- [SpringDataWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/SpringDataWalletRepository.java#L25-L26)

The JPA configuration includes:
- `ddl-auto: update` - Automatically updates the database schema based on entity changes
- `show-sql: true` - Logs all SQL statements to the console for debugging
- `database-platform: org.hibernate.dialect.H2Dialect` - Specifies the H2 dialect for optimal SQL generation

These settings enable rapid development by automatically synchronizing the database schema with entity definitions while providing visibility into generated SQL queries.

**Section sources**
- [application.yml](file://src/main/resources/application.yml#L20-L23)
- [JpaTransactionRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaTransactionRepository.java#L44-L151)
- [JpaWalletRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaWalletRepository.java#L35-L117)

## H2 Console Access

The application enables the H2 web console for development and debugging purposes, allowing direct database inspection and query execution.

```mermaid
sequenceDiagram
participant Developer
participant Browser
participant H2Console
participant Database
Developer->>Browser : Navigate to http : //localhost : 8080/h2-console
Browser->>H2Console : GET /h2-console
H2Console-->>Browser : Console login page
Developer->>Browser : Enter JDBC URL, username, password
Browser->>H2Console : Submit connection details
H2Console->>Database : Establish connection
H2Console-->>Browser : Console interface with query editor
Developer->>Browser : Execute SQL queries
Browser->>H2Console : Send SQL
H2Console->>Database : Execute query
Database-->>H2Console : Return results
H2Console-->>Browser : Display results
```

**Diagram sources**
- [application.yml](file://src/main/resources/application.yml#L16-L18)
- [README.md](file://README.md#L58-L90)

The H2 console is enabled through the `spring.h2.console.enabled=true` property and is accessible at `http://localhost:8080/h2-console` when the application is running. The console allows developers to:
- View and modify database contents directly
- Execute ad-hoc SQL queries for testing and debugging
- Inspect table structures and relationships
- Monitor database performance

**Section sources**
- [application.yml](file://src/main/resources/application.yml#L16-L18)
- [README.md](file://README.md#L58-L90)

## Environment-Specific Configuration

The application supports multiple environments through Spring Boot's configuration hierarchy, allowing property overrides without modifying the base configuration.

```mermaid
graph TD
A["Configuration Hierarchy"] --> B["application.yml (default)"]
A --> C["Environment Variables"]
A --> D["Command Line Arguments"]
A --> E["External Configuration Files"]
B --> F["H2 Database Configuration"]
C --> G["SPRING_DATASOURCE_URL=jdbc:postgresql://..."]
C --> H["SPRING_DATASOURCE_USERNAME=myuser"]
C --> I["SPRING_DATASOURCE_PASSWORD=secret"]
J["Configuration Precedence"] --> K["Higher Priority"]
J --> L["Lower Priority"]
K --> C
K --> D
K --> E
L --> B
```

**Diagram sources**
- [application.yml](file://src/main/resources/application.yml#L1-L34)
- [README.md](file://README.md#L58-L90)

The configuration system follows Spring Boot's property precedence rules, where environment variables override the values in `application.yml`. This enables seamless environment transitions without code changes. The README documentation provides examples of common environment overrides for datasource configuration.

**Section sources**
- [application.yml](file://src/main/resources/application.yml#L1-L34)
- [README.md](file://README.md#L58-L90)

## Connection and Dependency Management

The application manages database connections through Spring Data JPA repositories that abstract the underlying persistence mechanism.

```mermaid
classDiagram
class JpaTransactionRepository {
-springDataTransactionRepository : SpringDataTransactionRepository
-transactionMapper : TransactionMapper
+save(transaction)
+findById(id)
+findByWalletId(walletId)
+findAll()
+delete(id)
}
class SpringDataTransactionRepository {
<<interface>>
+save(entity)
+findById(id)
+findByHash(hash)
+findByNetworkId(networkId)
+findByStatus(status)
}
class TransactionMapper {
+toEntity(domain)
+toDomain(entity)
}
class TransactionEntity {
+id : UUID
+networkId : UUID
+hash : String
+fromAddress : String
+toAddress : String
+value : BigDecimal
+status : TransactionStatus
}
JpaTransactionRepository --> SpringDataTransactionRepository : "delegates to"
JpaTransactionRepository --> TransactionMapper : "uses"
TransactionMapper --> TransactionEntity : "maps to/from"
SpringDataTransactionRepository --> TransactionEntity : "manages"
```

**Diagram sources**
- [JpaTransactionRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaTransactionRepository.java#L44-L151)
- [SpringDataTransactionRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/SpringDataTransactionRepository.java#L15-L23)
- [TransactionMapper.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/mapper/TransactionMapper.java#L36-L74)

The repository pattern implements a layered architecture where:
- Domain repositories (e.g., `JpaTransactionRepository`) provide business logic interfaces
- Spring Data interfaces (e.g., `SpringDataTransactionRepository`) define data access contracts
- Entity mappers (e.g., `TransactionMapper`) convert between domain and persistence models
- JPA entities (e.g., `TransactionEntity`) represent database tables

This separation of concerns ensures that business logic remains decoupled from database implementation details.

**Section sources**
- [JpaTransactionRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/JpaTransactionRepository.java#L44-L151)
- [SpringDataTransactionRepository.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/data/repository/SpringDataTransactionRepository.java#L15-L23)
- [TransactionMapper.java](file://src/main/java/dev/bloco/wallet/hub/infra/provider/mapper/TransactionMapper.java#L36-L74)

## Caching Configuration

The application configures Spring's caching abstraction to improve performance by reducing database access for frequently accessed data.

```mermaid
flowchart LR
A["Cache Configuration"] --> B["Cache Manager"]
A --> C["Cache Names"]
B --> D["ConcurrentMapCacheManager"]
C --> E["wallet-hub cache"]
F["Cache Operations"] --> G["@Cacheable methods"]
F --> H["@CacheEvict operations"]
F --> I["@CachePut updates"]
G --> J["Caches method results"]
H --> K["Removes stale entries"]
I --> L["Updates cache entries"]
```

**Diagram sources**
- [application.yml](file://src/main/resources/application.yml#L3-L4)

The caching configuration defines a single cache named "wallet-hub" that can be used by service methods annotated with Spring's cache annotations (`@Cacheable`, `@CacheEvict`, `@CachePut`). This provides a simple in-memory caching solution for development and testing environments.

**Section sources**
- [application.yml](file://src/main/resources/application.yml#L3-L4)

## Production Database Migration

The application can be migrated from H2 to PostgreSQL or other production databases by modifying the datasource configuration.

```mermaid
graph LR
A["Development Environment"] --> |H2 File Database| B["application.yml"]
C["Production Environment"] --> |PostgreSQL Database| D["Environment Variables"]
B --> E["spring.datasource.url=jdbc:h2:file:./db/wallet"]
B --> F["spring.datasource.driver-class-name=org.h2.Driver"]
D --> G["SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/dbname"]
D --> H["SPRING_DATASOURCE_USERNAME=user"]
D --> I["SPRING_DATASOURCE_PASSWORD=password"]
D --> J["SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver"]
K["Migration Process"] --> L["Update datasource properties"]
K --> M["Add PostgreSQL dependency"]
K --> N["Verify dialect compatibility"]
K --> O["Test connection pooling"]
```

**Diagram sources**
- [application.yml](file://src/main/resources/application.yml#L10-L14)
- [pom.xml](file://pom.xml#L1-L424)
- [README.md](file://README.md#L58-L90)

The migration process involves:
1. Adding the PostgreSQL dependency in `pom.xml` (already included)
2. Updating datasource properties via environment variables
3. Changing the dialect to `org.hibernate.dialect.PostgreSQLDialect`
4. Configuring connection pooling for production workloads

The `pom.xml` file already includes both H2 and PostgreSQL dependencies, enabling seamless database switching without recompilation.

**Section sources**
- [application.yml](file://src/main/resources/application.yml#L10-L14)
- [pom.xml](file://pom.xml#L1-L424)
- [README.md](file://README.md#L58-L90)

## Security and Best Practices

The current configuration follows several best practices while highlighting areas for production enhancement.

```mermaid
flowchart TD
A["Security Considerations"] --> B["Current Configuration"]
A --> C["Production Recommendations"]
B --> D["Empty password for H2"]
B --> E["Default 'sa' username"]
B --> F["File-based H2 database"]
B --> G["H2 console enabled"]
C --> H["Use environment variables for credentials"]
C --> I["Implement connection pooling"]
C --> J["Disable H2 console in production"]
C --> K["Use encrypted secrets management"]
C --> L["Configure proper backup strategy"]
C --> M["Implement connection validation"]
```

**Diagram sources**
- [application.yml](file://src/main/resources/application.yml#L1-L34)
- [pom.xml](file://pom.xml#L1-L424)

Key security considerations:
- The development configuration uses default H2 credentials (username: sa, empty password) which are acceptable for local development but must be secured in production
- Database credentials should be managed through environment variables or secret management systems like Spring Cloud Vault
- The H2 console should be disabled in production environments to prevent unauthorized database access
- Connection pooling should be configured for production workloads to manage database connections efficiently
- Regular backups and disaster recovery procedures should be implemented for production databases

The application already includes Spring Cloud Vault and Spring Vault dependencies in `pom.xml`, providing a foundation for secure secret management in production deployments.

**Section sources**
- [application.yml](file://src/main/resources/application.yml#L1-L34)
- [pom.xml](file://pom.xml#L1-L424)