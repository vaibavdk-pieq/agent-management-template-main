#!/bin/bash

# Docker build script with dynamic CodeArtifact configuration
# This script automatically generates all build arguments from gradle.properties

set -e

# Default values
IMAGE_TAG="latest"
DOCKERFILE="Dockerfile"
BUILD_CONTEXT="."

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    -t|--tag)
      IMAGE_TAG="$2"
      shift 2
      ;;
    -f|--file)
      DOCKERFILE="$2"
      shift 2
      ;;
    -c|--context)
      BUILD_CONTEXT="$2"
      shift 2
      ;;
    -h|--help)
      echo "Usage: $0 [OPTIONS]"
      echo "Options:"
      echo "  -t, --tag TAG       Docker image tag (default: latest)"
      echo "  -f, --file FILE     Dockerfile path (default: Dockerfile)"
      echo "  -c, --context DIR   Build context directory (default: .)"
      echo "  -h, --help          Show this help message"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

echo "🔧 Setting up CodeArtifact configuration for Docker build..."

# Load CodeArtifact configuration
if [ -f "codeartifact.env" ]; then
  echo "📋 Loading existing CodeArtifact configuration..."
  export $(cat codeartifact.env | grep -v '^#' | xargs)
else
  echo "📋 Generating CodeArtifact configuration..."
  ./scripts/setup-codeartifact.sh
  export $(cat codeartifact.env | grep -v '^#' | xargs)
fi

# Validate required environment variables
required_vars=("CODEARTIFACT_DOMAIN" "CODEARTIFACT_DOMAIN_OWNER" "CODEARTIFACT_REGION" "CODEARTIFACT_REPOSITORY" "CODEARTIFACT_AUTH_TOKEN")
for var in "${required_vars[@]}"; do
  if [ -z "${!var}" ]; then
    echo "❌ Error: Required environment variable $var is not set"
    exit 1
  fi
done

echo "📋 CodeArtifact Configuration:"
echo "  Domain: $CODEARTIFACT_DOMAIN"
echo "  Domain Owner: $CODEARTIFACT_DOMAIN_OWNER"
echo "  Region: $CODEARTIFACT_REGION"
echo "  Repository: $CODEARTIFACT_REPOSITORY"
echo "  URL: $CODEARTIFACT_URL"

if [ -n "$PIEQ_CORE_LIB_VERSION" ]; then
  echo "  Core Lib Version: $PIEQ_CORE_LIB_VERSION"
fi

if [ -n "$PIEQ_HTTP_CLIENT_LIB_VERSION" ]; then
  echo "  HTTP Client Lib Version: $PIEQ_HTTP_CLIENT_LIB_VERSION"
fi

# Build Docker build arguments dynamically
echo "🔨 Building Docker image with dynamic configuration..."

# Start with basic build args
build_args=(
  "--build-arg" "CODEARTIFACT_DOMAIN=$CODEARTIFACT_DOMAIN"
  "--build-arg" "CODEARTIFACT_DOMAIN_OWNER=$CODEARTIFACT_DOMAIN_OWNER"
  "--build-arg" "CODEARTIFACT_REGION=$CODEARTIFACT_REGION"
  "--build-arg" "CODEARTIFACT_REPOSITORY=$CODEARTIFACT_REPOSITORY"
  "--build-arg" "CODEARTIFACT_TOKEN=$CODEARTIFACT_AUTH_TOKEN"
)

# Add PIEQ library versions if available
if [ -n "$PIEQ_CORE_LIB_VERSION" ]; then
  build_args+=("--build-arg" "PIEQ_CORE_LIB_VERSION=$PIEQ_CORE_LIB_VERSION")
fi

if [ -n "$PIEQ_HTTP_CLIENT_LIB_VERSION" ]; then
  build_args+=("--build-arg" "PIEQ_HTTP_CLIENT_LIB_VERSION=$PIEQ_HTTP_CLIENT_LIB_VERSION")
fi

# Add AWS role ARN if available
if [ -n "$AWS_ROLE_ARN" ]; then
  build_args+=("--build-arg" "AWS_ROLE_ARN=$AWS_ROLE_ARN")
fi

# Add any other CodeArtifact-related variables from gradle.properties
while IFS='=' read -r key value; do
  # Skip comments and empty lines
  if [[ ! "$key" =~ ^#.*$ ]] && [[ -n "$key" ]]; then
    # Convert to uppercase and replace dots with underscores for Docker build args
    docker_key=$(echo "$key" | tr '[:lower:]' '[:upper:]' | tr '.' '_')
    build_args+=("--build-arg" "$docker_key=$value")
    echo "  Added build arg: $docker_key"
  fi
done < codeartifact.env

# Execute Docker build
echo "🚀 Executing Docker build..."
echo "Command: docker build ${build_args[@]} -t $IMAGE_TAG -f $DOCKERFILE $BUILD_CONTEXT"

docker build "${build_args[@]}" -t "$IMAGE_TAG" -f "$DOCKERFILE" "$BUILD_CONTEXT"

echo "✅ Docker build completed successfully!"
echo "📦 Image tagged as: $IMAGE_TAG" 