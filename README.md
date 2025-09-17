# PIEQ API Template
//agent
A multi-module Kotlin Dropwizard API template that demonstrates integration with PIEQ libraries and provides a complete User management system with PostgreSQL database support. 

# Table of Contents test

- [Features](#features)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
  - [Clone and Setup](#1-clone-and-setup)
  - [Environment Configuration](#2-environment-configuration)
  - [Start Database](#3-start-database)
  - [Set Environment Variable](#4-set-environment-variable)
  - [Build and Run](#5-build-and-run)
- [Configuration Details](#configuration-details)
  - [Application Configuration](#application-configuration)
  - [Server Configuration](#server-configuration)
  - [Client Configuration](#client-configuration)
  - [Security Configuration](#security-configuration)
  - [Monitoring and Health](#monitoring-and-health)
  - [Database Configuration](#database-configuration)
- [API Endpoints](#api-endpoints)
  - [User Management](#user-management)
- [Database Schema](#database-schema)
- [Deployment](#deployment)
  - [Environment Setup](#environment-setup)
  - [Production Deployment Example](#production-deployment-example)
- [Development](#development)
  - [Running Tests](#running-tests)
  - [Building Individual Modules](#building-individual-modules)
  - [Code Formatting](#code-formatting)
  - [Publishing to Local Repository](#publishing-to-local-repository)
- [Modules](#modules)
  - [pieq-api](#pieq-api)
  - [pieq-application](#pieq-application)
  - [pieq-client](#pieq-client)
- [Health Checks](#health-checks)
- [PIEQ Libraries](#pieq-libraries)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Multi-Module Gradle Project**: Clean separation of concerns with `pieq-api`, `pieq-application`, and `pieq-client` modules
- **Kotlin + Dropwizard 4.0**: Modern REST API framework with Kotlin support
- **PIEQ Libraries Integration**: Uses `pieq-core-lib` and `pieq-http-client-lib` from AWS CodeArtifact
- **Centralized Version Management**: All PIEQ library versions managed in `gradle.properties`
- **Latest Version Strategy**: Automatically uses latest versions with option to pin for production
- **PostgreSQL Database**: Full CRUD operations with JDBI3
- **User Management**: Complete user resource with validation and business logic
- **REST Client**: Included client module for API consumption
- **Comprehensive Testing**: Unit tests with Dropwizard testing framework
- **Code Quality**: Spotless formatting and Kotlin linting
- **Production Ready**: Proper logging, health checks, and configuration
- **Security Integration**: Keycloak authentication support
- **Monitoring**: Sentry integration for error tracking
- **Health Checks**: Database connectivity monitoring
- **AWS Integration**: CodeArtifact for dependency management, Secrets Manager for configuration

## Project Structure

This is a multi-module Gradle project with the following structure:

```
pieq-api-template/
├── build.gradle.kts                           # Root build configuration
├── settings.gradle.kts                        # Module definitions
├── gradle.properties                          # PIEQ library versions and CodeArtifact config
├── gradlew & gradlew.bat                     # Gradle wrapper scripts
├── pieq-api/                                 # Data models and DTOs
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/pieq/model/
│       └── User.kt                           # User model and request/response DTOs
├── pieq-application/                         # Main application module
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── kotlin/com/pieq/application/
│       │   │   ├── PieqApiApplication.kt     # Main application class
│       │   │   ├── config/
│       │   │   │   └── PieqApiConfiguration.kt # Configuration classes
│       │   │   ├── dao/
│       │   │   │   └── UserDao.kt            # Database access layer
│       │   │   ├── resources/
│       │   │   │   └── UserResource.kt       # REST endpoints
│       │   │   └── service/
│       │   │       └── UserService.kt        # Business logic layer
│       │   └── resources/
│       │       ├── config_dev.yml            # Development configuration
│       │       ├── config_preprod.yml        # Pre-production configuration template
│       │       └── config_prod.yml           # Production configuration template
│       └── test/
│           ├── kotlin/com/pieq/application/
│           │   └── resources/
│           │       └── UserResourceTest.kt   # Integration tests
│           └── resources/
│               └── config_test.yml           # Test configuration
└── pieq-client/                              # API client module
    ├── build.gradle.kts
    └── src/main/kotlin/com/pieq/client/
        └── UserClient.kt                     # HTTP client for User API
```

---

## Shared and Project-Specific Dependencies

### Shared Dependencies

Shared dependencies for all subprojects are defined in the root `build.gradle.kts` using the version catalog. This is done inside a `subprojects { ... }` block, so all subprojects automatically get these dependencies:

```kotlin
subprojects {
    val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
    plugins.withId("org.jetbrains.kotlin.jvm") {
        dependencies {
            implementation(libs.findLibrary("kotlin-std-lib").get())
            implementation(libs.findLibrary("kotlin-reflect").get())
            implementation(libs.findLibrary("kotlin-logging-jvm").get())
            implementation(libs.findLibrary("dropwizard-core").get())
            implementation(libs.findLibrary("dropwizard-jetty").get())
            implementation(libs.findLibrary("dropwizard-jdbi3").get())
            implementation(libs.findLibrary("dropwizard-validation").get())
            implementation(libs.findLibrary("jackson-module-kotlin").get())
            implementation(libs.findLibrary("jackson-datatype-jsr310").get())
            implementation(libs.findLibrary("rs-api").get())
            implementation(libs.findLibrary("inject-api").get())
            implementation(libs.findLibrary("sentry").get())
            testImplementation(libs.findLibrary("dropwizard-testing").get())
            testImplementation(libs.findLibrary("kotlin-test-junit5").get())
            testImplementation(libs.findLibrary("junit-jupiter").get())
            testImplementation(libs.findLibrary("mockk").get())
        }
    }
}
```

### Project-Specific Dependencies

If a subproject needs additional dependencies, add them in that subproject's `build.gradle.kts`:

```kotlin
// pieq-api/build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.jvm)
    // ... other plugins ...
}

dependencies {
    // Only project-specific dependencies here
    implementation(libs.pieqCoreLib)
    implementation(libs.pieqHttpClientLib)
    // etc.
}
```

- Do **not** add a `dependencies { ... }` block at the root level.
- Only add project-specific dependencies in each subproject's build file.

### Version Catalog

All dependencies are managed via the version catalog in `gradle/libs.versions.toml`. Update this file to add or change dependency versions for the whole project.

---

## Prerequisites

- Java 21+
- Gradle 7.0+ (or use included Gradle wrapper) 
- PostgreSQL database
- AWS CodeArtifact access (for PIEQ libraries)
- Keycloak server (for authentication)

## Quick Start

### 1. Clone and Setup

```bash
git clone <repository-url>
cd pieq-api-template
```

### 2. Environment Configuration

The project includes multiple environment configurations: 

- **Development**: Uses `config_dev.yml` with default PostgreSQL settings and local Keycloak
- **Pre-production**: Uses `config_preprod.yml` template - requires placeholder replacement
- **Production**: Uses `config_prod.yml` template - requires placeholder replacement 
- **Testing**: Uses `config_test.yml` for automated tests

#### Configuration Structure

The application uses a hierarchical configuration structure:

```yaml
application:
  name: "pieq-api-application"
  version: "1.0"
  enableCors: false
  corsOrigins:
    - "*"

sentry:
  dsn: null  # Set to null to disable Sentry in development
  debug: false

server:
  applicationConnectors:
    - type: http
      port: 8080

jerseyClient:
  timeout: 45s
  connectionTimeout: 15s

keycloak:
  certUrl: http://localhost:9000/realms/pieq-mlb/protocol/openid-connect/certs

database:
  driverClass: org.postgresql.Driver
  user: postgres
  password: postgres
  url: jdbc:postgresql://localhost:5432/postgres

logging:
  level: WARN
  loggers:
    com.pieq: DEBUG
    io.dropwizard: INFO
  appenders:
    - type: console

health:
  healthChecks:
    - name: database
      type: ready
      critical: true
      initialState: false
      schedule:
       checkInterval: 5s
       downtimeInterval: 30s
       initialDelay: 5s
       failureAttempts: 3
       successAttempts: 2
```

#### Environment-Specific Configuration

**Development (`config_dev.yml`)**:
- CORS disabled
- Sentry disabled (`dsn: null`)
- Local Keycloak server (`http://localhost:9000/realms/pieq-mlb/protocol/openid-connect/certs`)
- Debug logging for PIEQ components, INFO for Dropwizard
- Default PostgreSQL settings
- HTTP server on port 8080
- Jersey client with 45s timeout, 15s connection timeout
- Database health checks with 5s intervals

**Pre-production (`config_preprod.yml`)**:
- CORS enabled with wildcard origins (`*`)
- Sentry integration (requires `sm:sentry_dsn`)
- Keycloak integration (requires `sm:keycloak_cert_url`)
- Database credentials via Secrets Manager (`sm:db_credential:db_user`, `sm:db_credential:db_password`, `sm:db_credential:db_url`)
- INFO level logging for both PIEQ and Dropwizard components
- HTTP server on port 8080
- Jersey client with 45s timeout, 15s connection timeout
- Database health checks with 5s intervals

**Production (`config_prod.yml`)**:
- CORS enabled with specific origins (requires `<CORS_ORIGIN>` placeholder)
- Sentry integration (requires `sm:sentry_dsn`)
- Keycloak integration (requires `sm:keycloak_cert_url`)
- Database credentials via Secrets Manager (`sm:db_credential:db_user`, `sm:db_credential:db_password`, `sm:db_credential:db_url`)
- WARN level logging for Dropwizard, INFO for PIEQ components
- HTTP server on port 8080
- Jersey client with 45s timeout, 15s connection timeout
- Database health checks with 5s intervals

**Testing (`config_test.yml`)**:
- CORS disabled
- Sentry disabled (`dsn: null`)
- Local Keycloak server (`http://localhost:9000/realms/master/protocol/openid-connect/certs`)
- INFO level logging for application components (`com.pieq.application: INFO`)
- Console logging with UTC timezone and INFO threshold
- HTTP server on port 8080
- Jersey client with 45s timeout, 15s connection timeout
- Database health checks with 5s intervals

### 3. Start Database

You'll need a PostgreSQL database running. The development configuration uses default PostgreSQL settings:

```yaml
database:
  driverClass: org.postgresql.Driver
  user: postgres
  password: postgres
  url: jdbc:postgresql://localhost:5432/postgres
```

### 4. Set Environment Variable

Before running the application, set the `PIEQ_ENVIRONMENT` variable to specify which configuration to use:

```bash
# For development (uses config_dev.yml)
export PIEQ_ENVIRONMENT=dev

# For pre-production (uses config_preprod.yml)
export PIEQ_ENVIRONMENT=preprod

# For production (uses config_prod.yml)
export PIEQ_ENVIRONMENT=prod
```

### 5. Build and Run

#### Using Gradle (Recommended)
```bash
# Build all modules
./gradlew build

# Run the application directly (development)
./gradlew run --args="server"

# Or run from the application module
cd pieq-application
../gradlew run --args="server"
```

#### Using JAR (Alternative)
```bash
# Build the application
./gradlew build

# Run the JAR file (development)
java -jar pieq-application/build/libs/pieq-application-1.0.jar server

# For other environments
java -jar pieq-application/build/libs/pieq-application-1.0.jar server
```

The API will be available at:
- Application: http://localhost:8080
- Admin: http://localhost:8081

## Configuration Details

### Application Configuration

The application configuration extends `PieqApplicationConfiguration` and includes:

- **API Version**: Configurable API version for client compatibility
- **CORS Settings**: Cross-origin resource sharing configuration
- **CORS Origins**: List of allowed origins for cross-origin requests

### Server Configuration

- **HTTP Server**: Configurable application connectors on port 8080
- **Application Connectors**: HTTP connector with customizable port settings

### Client Configuration

- **Jersey Client**: HTTP client with configurable timeouts
- **Connection Timeout**: 15 seconds for initial connection establishment
- **Request Timeout**: 45 seconds for complete request processing

### Security Configuration

- **Keycloak Integration**: OAuth2/OpenID Connect authentication
- **Certificate URL**: Configurable endpoint for Keycloak certificate validation

### Monitoring and Health

- **Sentry Integration**: Error tracking and monitoring
- **Health Checks**: Database connectivity monitoring with configurable intervals
  - **Check Interval**: 5 seconds between health checks
  - **Downtime Interval**: 30 seconds to mark service as down
  - **Initial Delay**: 5 seconds before first health check
  - **Failure Attempts**: 3 consecutive failures to mark as unhealthy
  - **Success Attempts**: 2 consecutive successes to mark as healthy
- **Logging**: Environment-specific logging levels and appenders

### Database Configuration

- **PostgreSQL**: Primary database with JDBI3 integration
- **Connection Pooling**: Configurable timeouts and connection settings
- **Health Monitoring**: Database readiness checks with failure/success thresholds

## API Endpoints

### User Management

#### Create User
```http
POST /users
Content-Type: application/json

{
  "userName": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

#### Get All Users
```http
GET /users?limit=20&offset=0&activeOnly=false
```

#### Get User by ID
```http
GET /users/{guid}
```

#### Get User by Username
```http
GET /users/username/{username}
```

#### Update User
```http
PUT /users/{guid}
Content-Type: application/json

{
  "email": "newemail@example.com",
  "firstName": "UpdatedName",
  "active": true
}
```

#### Deactivate User
```http
POST /users/{guid}/deactivate
```

#### Delete User
```http
DELETE /users/{guid}
```

#### Get User Count
```http
GET /users/count
```

## Configuration

### Environment-Specific Configuration
The project supports multiple environment configurations:

#### Development Configuration (`config_dev.yml`)
The development configuration file is located at `pieq-application/src/main/resources/config_dev.yml`:

```yaml
application:
  name: "pieq-api-application"
  version: "1.0"
  enableCors: false
  corsOrigins:
    - "*"

server:
  applicationConnectors:
    - type: http
      port: 8080

database:
  driverClass: org.postgresql.Driver
  user: postgres
  password: postgres
  url: jdbc:postgresql://localhost:5432/postgres

logging:
  level: WARN
  loggers:
    com.pieq: DEBUG
    io.dropwizard: INFO
```

#### Pre-Production Configuration (`config_preprod.yml`)
Template configuration for pre-production environments with placeholder values:

```yaml
application:
  name: <APP_NAME>
  version: "1.0"
  enableCors: true
  corsOrigins:
    - *

database:
  driverClass: org.postgresql.Driver
  user: <DB_USER>
  password: <DB_PASSWORD>
  url: <DB_URL>

sentry:
  dsn: <SENTRY_DSN>
  debug: false

logging:
  level: INFO
  loggers:
    com.pieq: INFO
    io.dropwizard: INFO
```

#### Production Configuration (`config_prod.yml`)
Template configuration for production environments with placeholder values:

```yaml
application:
  name: <APP_NAME>
  version: "1.0"
  enableCors: true
  corsOrigins: 
    - <CORS_ORIGIN>

database:
  driverClass: org.postgresql.Driver
  user: <DB_USER>
  password: <DB_PASSWORD>
  url: <DB_URL>

sentry:
  dsn: <SENTRY_DSN>
  debug: false

logging:
  level: WARN
  loggers:
    com.pieq: INFO
    io.dropwizard: WARN
```

#### Test Configuration (`config_test.yml`)
Test configuration is available at `pieq-application/src/test/resources/config_test.yml` for testing purposes.

### Configuration Placeholders
For production and pre-production environments, replace the following placeholders:

**Application Configuration:**
- `<APP_NAME>`: Your application name
- `<CORS_ORIGIN>`: Allowed CORS origins for production (only in production config)

**Secrets Manager Integration:**
- `sm:sentry_dsn`: Sentry DSN for error tracking
- `sm:keycloak_cert_url`: Keycloak certificate URL
- `sm:db_credential:db_user`: Database username from Secrets Manager
- `sm:db_credential:db_password`: Database password from Secrets Manager
- `sm:db_credential:db_url`: Database connection URL from Secrets Manager

### Environment Variable
The application automatically selects the appropriate configuration file based on the `PIEQ_ENVIRONMENT` environment variable:
- `PIEQ_ENVIRONMENT=dev` → Uses `config_dev.yml`
- `PIEQ_ENVIRONMENT=preprod` → Uses `config_preprod.yml`
- `PIEQ_ENVIRONMENT=prod` → Uses `config_prod.yml`

**Important**: The `PIEQ_ENVIRONMENT` variable must be set before starting the application.

### PIEQ Libraries Configuration
The project uses PIEQ libraries from AWS CodeArtifact with centralized version management:

#### Version Management
All PIEQ library versions are centrally managed in `gradle.properties`:

```properties
# PIEQ Library Versions
# Use 'latest' to always get the latest version from CodeArtifact
# Change to specific version (e.g., '1.0.9') when you need to pin for production
pieq.core.lib.version=latest
pieq.http.client.lib.version=latest

# CodeArtifact Configuration
codeartifact.domain= pieq
codeartifact.domainOwner=910020091862
codeartifact.region=us-east-1
codeartifact.repository=pieq-artifact
```

#### Version Strategy
- **Development**: Uses `latest` to automatically get the newest versions
- **Production**: Pin to specific versions for stability (e.g., `1.0.9`)
- **Flexible**: Easy to switch between latest and pinned versions

#### Available Libraries
- `com.pieq:pieq-core-lib`: Core utilities, database providers, HTTP clients, and configuration management
- `com.pieq:pieq-http-client-lib`: HTTP client utilities for external API calls

## Database Schema

The `User` model includes:
- `guid`: UUID primary key
- `userName`: Unique username (3-50 characters)
- `email`: Unique email address
- `firstName`: User's first name (max 100 characters)
- `lastName`: User's last name (max 100 characters)
- `active`: Boolean flag for soft deletion
- `createdAt`: Creation timestamp
- `updatedAt`: Last update timestamp

## Deployment

### Environment Setup
Before deploying to pre-production or production environments:

1. **Version Management**: Consider pinning PIEQ library versions in `gradle.properties` for production stability
2. **Replace Configuration Placeholders**: Update `config_preprod.yml` or `config_prod.yml` with actual values
3. **Set Environment Variables**: Ensure required environment variables are set:
   - `PIEQ_ENVIRONMENT`: Environment type (`dev`, `preprod`, or `prod`)
4. **Configure Secrets Manager**: Set up the following secrets in your Secrets Manager:
   - `sentry_dsn`: Sentry DSN for error tracking
   - `keycloak_cert_url`: Keycloak certificate URL
   - `db_credential`: Database credentials (user, password, url)
5. **Configure External Services**: Set up Sentry for error tracking and Keycloak for authentication
6. **CORS Configuration**: Update CORS origins for your specific domains (production only)
7. **AWS CodeArtifact Access**: Ensure deployment environment has access to CodeArtifact repository

### Production Deployment Example
```bash
# Set environment variables
export PIEQ_ENVIRONMENT=prod

# Build and run with production config
./gradlew build
java -jar pieq-application/build/libs/pieq-application-1.0.jar server
```

**Note**: Production and pre-production environments use Secrets Manager for sensitive configuration. Ensure your Secrets Manager is properly configured with the required secrets before deployment.

## Development

### Running Tests
```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :pieq-application:test
```

### Building Individual Modules
```bash
# Build specific module
./gradlew :pieq-api:build
./gradlew :pieq-application:build
./gradlew :pieq-client:build
```

### Code Formatting
The project uses Spotless for code formatting:
```bash
# Apply formatting
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck
```

### Publishing to Local Repository
```bash
# Publish all modules to local Maven repository
./gradlew publishToMavenLocal
```

### Version Management
```bash
# Verify PIEQ library versions being used
./gradlew verifyPieqVersions

# Update to specific versions (edit gradle.properties)
# Change from 'latest' to specific version like '1.0.9'
```

### AWS CodeArtifact Setup
The project automatically authenticates with AWS CodeArtifact to fetch PIEQ libraries. Ensure you have:
- AWS credentials configured
- Access to the CodeArtifact repository
- Proper IAM permissions for CodeArtifact access

## Modules

### pieq-api
Contains the data models and DTOs:
- `User`: Main user entity with validation annotations
- `CreateUserRequest`: DTO for user creation
- `UpdateUserRequest`: DTO for user updates

### pieq-application
The main application module containing:
- REST resources and endpoints
- Business logic services
- Database access objects (DAOs)
- Application configuration
- Main application class

### pieq-client
HTTP client for consuming the User API:
- `UserClient`: JAX-RS client for making HTTP requests to the API

## Health Checks

- Application health: http://localhost:8081/healthcheck
- Metrics: http://localhost:8081/metrics
- Admin tasks: http://localhost:8081/tasks

## CodeArtifact Publishing

The `pieq-client` module is automatically published to AWS CodeArtifact for use in other REST APIs.

### Publishing Workflow

The `api-publish-workflow.yml` handles automatic publishing:

- **Trigger**: Runs after successful completion of `Dev-Test` workflow on `main` branch
- **Manual Trigger**: Can be manually triggered via GitHub Actions UI (only when Dev-Test has succeeded)
- **Versioning**: Uses simple incremental versioning (e.g., 1.0.0 → 1.0.1 → 1.0.2)
- **Repository**: Publishes to `pieq-artifact` CodeArtifact repository

### Usage in Other Projects

Once published, other REST APIs can use the client:

```kotlin
dependencies {
    implementation("com.pieq:pieq-client:1.0.1")  // Latest version
}
```

### Publishing Configuration

The publishing is configured in `pieq-client/build.gradle.kts`:

- **Group ID**: `com.pieq`
- **Artifact ID**: `pieq-client`
- **Repository**: AWS CodeArtifact Maven repository
- **Authentication**: Uses `CODEARTIFACT_AUTH_TOKEN` environment variable

## PIEQ Libraries

This template demonstrates integration with PIEQ libraries from AWS CodeArtifact:

### Core Libraries
- **`pieq-core-lib`**: Core framework providing:
  - Base application classes (`PieqApplication`)
  - Database providers (`PieqDbClientProvider`)
  - HTTP client providers (`PieqHttpClientProvider`)
  - Configuration management (`PieqApplicationConfiguration`)
  - Authorization framework (`@PieqAuthorization`)
  - Service and DAO base classes
  - Secrets Manager integration

- **`pieq-http-client-lib`**: HTTP client utilities for external API calls

### Version Management
- **Centralized**: All versions managed in `gradle.properties`
- **Latest Strategy**: Uses `latest` for development, pin specific versions for production
- **Automatic Resolution**: Gradle automatically resolves latest versions from CodeArtifact
- **Flexible**: Easy to switch between latest and pinned versions

### Usage Examples
```kotlin
// Application extends PIEQ base class
class PieqApiApplication : PieqApplication<PieqApiConfiguration>()

// DAO extends PIEQ base DAO
class UserDao : PieqDao<User>(pieqDbClientProvider)

// Service extends PIEQ base service
class UserService : PieqService<User>(userDao, pieqHttpClientProvider, pieqApplicationConfigurationProvider)

// Resource extends PIEQ base resource with authorization
class UserResource : PieqResource<User>() {
    @PieqAuthorization(scopes = ["user:read"])
    fun getUser(guid: UUID): User { ... }
}
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Run `./gradlew spotlessApply` to format code
6. Ensure all tests pass with `./gradlew test`
7. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
