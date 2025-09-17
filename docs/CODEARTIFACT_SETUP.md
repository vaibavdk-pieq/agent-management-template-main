# Centralized CodeArtifact Configuration

This document describes the centralized CodeArtifact configuration setup that eliminates duplication across multiple files and provides a single source of truth for all CodeArtifact-related settings.

## Overview

Previously, CodeArtifact configuration was duplicated across:
- GitHub Workflows (`api-dev-workflow.yml`, `api-preprod-workflow.yml`)
- `gradle.properties`
- `build.gradle.kts`

Now, all configuration is centralized in `gradle.properties` and accessed through Gradle tasks and a setup script.

## Configuration Files

### 1. `gradle.properties` - Single Source of Truth

All CodeArtifact configuration is defined here:

```properties
# PIEQ Library Versions
pieq.core.lib.version=1.0.12
pieq.http.client.lib.version=1.0.2

# CodeArtifact Configuration
codeartifact.domain=preprod-pieq
codeartifact.domainOwner=910020091862
codeartifact.region=us-east-1
codeartifact.repository=pieq-artifact
```

### 2. `build.gradle.kts` - Gradle Tasks

The root `build.gradle.kts` provides several tasks for managing CodeArtifact configuration:

#### Available Tasks

```bash
# Generate CodeArtifact configuration files
./gradlew generateCodeArtifactConfig

# Get CodeArtifact authorization token
./gradlew getCodeArtifactToken

# Verify CodeArtifact configuration and connectivity
./gradlew verifyCodeArtifactConfig
```

#### Task Details

- **`generateCodeArtifactConfig`**: Creates Maven `settings.xml` and environment variables file
- **`getCodeArtifactToken`**: Retrieves and stores CodeArtifact authorization token
- **`verifyCodeArtifactConfig`**: Tests connectivity and verifies library versions

### 3. `scripts/setup-codeartifact.sh` - CI/CD Setup Script

This script is used by GitHub Actions workflows to set up CodeArtifact configuration:

```bash
# Run the setup script
./scripts/setup-codeartifact.sh
```

The script:
- Reads configuration from `gradle.properties`
- Gets CodeArtifact authorization token
- Creates Maven `settings.xml`
- Exports environment variables
- Creates `codeartifact.env` file (temporary, contains sensitive tokens)

### 4. `scripts/docker-build-with-codeartifact.sh` - Dynamic Docker Build Script

This script provides fully dynamic Docker builds:

```bash
# Basic usage
./scripts/docker-build-with-codeartifact.sh -t your-image:tag

# With custom Dockerfile
./scripts/docker-build-with-codeartifact.sh -t your-image:tag -f Dockerfile.prod

# Show help
./scripts/docker-build-with-codeartifact.sh --help
```

The script:
- Automatically loads all configuration from `gradle.properties`
- Generates all necessary Docker build arguments dynamically
- Validates required environment variables
- Converts property names to Docker build arg format
- Provides clear logging and error handling

## Usage in CI/CD Workflows

### GitHub Actions

Workflows now use the centralized setup:

```yaml
- name: Setup CodeArtifact configuration
  run: |
    # Use centralized CodeArtifact setup script
    ./scripts/setup-codeartifact.sh
    
    # Load all environment variables dynamically from the generated file
    # This makes it truly dynamic - no hardcoded variable names
    if [ -f "codeartifact.env" ]; then
      echo "📋 Loading CodeArtifact configuration from gradle.properties..."
      
      # Export all variables from the generated env file
      export $(cat codeartifact.env | grep -v '^#' | xargs)
      
      # Export all variables to GitHub Actions environment
      # This is dynamic - any new variables added to gradle.properties will be automatically included
      while IFS='=' read -r key value; do
        # Skip comments and empty lines
        if [[ ! "$key" =~ ^#.*$ ]] && [[ -n "$key" ]]; then
          echo "$key=$value" >> $GITHUB_ENV
          echo "✅ Exported: $key"
        fi
      done < codeartifact.env
      
      echo "🎉 All CodeArtifact configuration loaded dynamically from gradle.properties"
    else
      echo "❌ Error: codeartifact.env file not found"
      exit 1
    fi
```

### Docker Builds

Docker builds use the centralized configuration with a dynamic build script:

#### Option 1: Using the Dynamic Build Script (Recommended)

```bash
# Use the dynamic Docker build script
./scripts/docker-build-with-codeartifact.sh -t your-image:tag
```

This script automatically:
- Loads all configuration from `gradle.properties`
- Generates all necessary build arguments
- Validates required environment variables
- Executes the Docker build with proper configuration

#### Option 2: Manual Build with Environment Variables

```bash
# Load environment variables from centralized configuration
export $(cat codeartifact.env | grep -v '^#' | xargs)

docker build \
  --build-arg CODEARTIFACT_DOMAIN=$CODEARTIFACT_DOMAIN \
  --build-arg CODEARTIFACT_DOMAIN_OWNER=$CODEARTIFACT_DOMAIN_OWNER \
  --build-arg CODEARTIFACT_REGION=$CODEARTIFACT_REGION \
  --build-arg CODEARTIFACT_REPOSITORY=$CODEARTIFACT_REPOSITORY \
  --build-arg CODEARTIFACT_TOKEN=$CODEARTIFACT_AUTH_TOKEN \
  --build-arg PIEQ_CORE_LIB_VERSION=$PIEQ_CORE_LIB_VERSION \
  --build-arg PIEQ_HTTP_CLIENT_LIB_VERSION=$PIEQ_HTTP_CLIENT_LIB_VERSION \
  -t $IMAGE_TAG .
```

## Benefits

### 1. Single Source of Truth
- All CodeArtifact configuration is in `gradle.properties`
- No more duplication across multiple files
- Easy to update and maintain

### 2. Reduced Developer Error
- Configuration is centralized and validated
- Consistent settings across all environments
- Clear documentation and examples

### 3. Simplified CI/CD
- Workflows use a single setup script
- Environment variables are automatically generated
- Less boilerplate code in workflows

### 4. Better Developer Experience
- Gradle tasks for common operations
- Clear error messages and fallbacks
- Easy verification and testing

## Migration Guide

### From Old Configuration

If you have existing CodeArtifact configuration in workflows:

1. **Remove hardcoded values** from workflow files
2. **Add configuration** to `gradle.properties`
3. **Replace manual setup** with `./scripts/setup-codeartifact.sh`
4. **Update Docker builds** to use environment variables

### Example Migration

**Before:**
```yaml
env:
  CODEARTIFACT_DOMAIN: preprod-pieq
  CODEARTIFACT_DOMAIN_OWNER: 910020091862
  # ... more hardcoded values
```

**After:**
```yaml
# No hardcoded values in workflow
# Configuration comes from gradle.properties via setup script
```

## Troubleshooting

### Common Issues

1. **Token not found**: Ensure AWS credentials are configured
2. **Configuration not found**: Check `gradle.properties` exists
3. **Script not executable**: Run `chmod +x scripts/setup-codeartifact.sh`

### Verification

Use the verification task to test your setup:

```bash
./gradlew verifyCodeArtifactConfig
```

This will:
- Check connectivity to CodeArtifact
- Verify library versions are available
- Display current configuration

## Environment-Specific Configuration

For different environments (dev, staging, prod), you can:

1. **Use different `gradle.properties` files**:
   - `gradle.properties.dev`
   - `gradle.properties.prod`

2. **Override properties** via environment variables:
   ```bash
   export CODEARTIFACT_DOMAIN=prod-pieq
   ./scripts/setup-codeartifact.sh
   ```

3. **Use Gradle profiles** for different configurations

## Security Considerations

- CodeArtifact tokens are automatically retrieved and not stored in version control
- Environment variables are used for sensitive data
- AWS IAM roles provide secure access to CodeArtifact
- Tokens expire automatically and are refreshed as needed
- **`codeartifact.env` and `codeartifact.token` files are automatically ignored by git** (added to `.gitignore`)
- **Temporary files are automatically cleaned up** after CI/CD workflows complete 