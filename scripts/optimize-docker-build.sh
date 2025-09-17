#!/bin/bash

# Docker Build Optimization Script
# This script optimizes Docker builds for faster CI/CD pipelines

set -e

echo "🔧 Optimizing Docker build configuration..."

# Check if we're in a CI environment
if [ -n "$CI" ]; then
    echo "📋 CI environment detected"
    export DOCKER_BUILDKIT=1
    export COMPOSE_DOCKER_CLI_BUILD=1
fi

# Set Docker BuildKit for better performance
export DOCKER_BUILDKIT=1

# Function to clean up old images and containers
cleanup_docker() {
    echo "🧹 Cleaning up old Docker resources..."
    
    # Remove dangling images
    docker image prune -f 2>/dev/null || true
    
    # Remove stopped containers
    docker container prune -f 2>/dev/null || true
    
    # Remove unused networks
    docker network prune -f 2>/dev/null || true
    
    # Remove unused volumes (be careful with this in production)
    if [ "$1" = "--aggressive" ]; then
        docker volume prune -f 2>/dev/null || true
    fi
}

# Function to build with optimizations
build_with_optimizations() {
    local context="."
    local dockerfile="Dockerfile"
    local tag="${1:-latest}"
    local platform="${2:-linux/amd64}"
    
    echo "🚀 Building Docker image with optimizations..."
    echo "   Context: $context"
    echo "   Dockerfile: $dockerfile"
    echo "   Tag: $tag"
    echo "   Platform: $platform"
    
    # Build with optimizations
    docker build \
        --platform "$platform" \
        --tag "$tag" \
        --file "$dockerfile" \
        --build-arg BUILDKIT_INLINE_CACHE=1 \
        --build-arg BUILDKIT_STEP_LOG_MAX_SIZE=10485760 \
        --build-arg BUILDKIT_STEP_LOG_MAX_SPEED=10485760 \
        --progress=plain \
        "$context"
}

# Function to build for multiple platforms
build_multi_platform() {
    local context="."
    local dockerfile="Dockerfile"
    local tag="${1:-latest}"
    local platforms="${2:-linux/amd64,linux/arm64}"
    
    echo "🚀 Building multi-platform Docker image..."
    echo "   Context: $context"
    echo "   Dockerfile: $dockerfile"
    echo "   Tag: $tag"
    echo "   Platforms: $platforms"
    
    # Create and use a new builder instance for multi-platform builds
    docker buildx create --use --name multi-platform-builder 2>/dev/null || true
    
    # Build for multiple platforms
    docker buildx build \
        --platform "$platforms" \
        --tag "$tag" \
        --file "$dockerfile" \
        --build-arg BUILDKIT_INLINE_CACHE=1 \
        --cache-from "type=registry,ref=$tag" \
        --cache-to "type=inline" \
        --push \
        "$context"
}

# Function to analyze build performance
analyze_build() {
    local image_tag="${1:-latest}"
    
    echo "📊 Analyzing build performance..."
    
    # Get image size
    local image_size=$(docker images "$image_tag" --format "table {{.Size}}" | tail -n 1)
    echo "   Image size: $image_size"
    
    # Get layer count
    local layer_count=$(docker history "$image_tag" --format "table {{.CreatedBy}}" | wc -l)
    echo "   Layer count: $((layer_count - 1))"
    
    # Show layer breakdown
    echo "   Layer breakdown:"
    docker history "$image_tag" --format "table {{.Size}}\t{{.CreatedBy}}" | head -10
}

# Main execution
case "${1:-build}" in
    "build")
        build_with_optimizations "${2:-latest}" "${3:-linux/amd64}"
        ;;
    "multi-platform")
        build_multi_platform "${2:-latest}" "${3:-linux/amd64,linux/arm64}"
        ;;
    "cleanup")
        cleanup_docker "$2"
        ;;
    "analyze")
        analyze_build "${2:-latest}"
        ;;
    "all")
        cleanup_docker
        build_with_optimizations "${2:-latest}" "${3:-linux/amd64}"
        analyze_build "${2:-latest}"
        ;;
    *)
        echo "Usage: $0 [build|multi-platform|cleanup|analyze|all] [tag] [platform]"
        echo ""
        echo "Commands:"
        echo "  build          - Build with optimizations (default)"
        echo "  multi-platform - Build for multiple platforms"
        echo "  cleanup        - Clean up old Docker resources"
        echo "  analyze        - Analyze build performance"
        echo "  all            - Run cleanup, build, and analyze"
        echo ""
        echo "Examples:"
        echo "  $0 build myapp:latest linux/amd64"
        echo "  $0 multi-platform myapp:latest linux/amd64,linux/arm64"
        echo "  $0 cleanup --aggressive"
        echo "  $0 analyze myapp:latest"
        exit 1
        ;;
esac

echo "✅ Docker build optimization completed!" 