### Stage 1: Builder with CodeArtifact support ###
FROM amazoncorretto:21 AS builder

# Set working directory
WORKDIR /app

# Install only essential packages and pin versions for better caching
RUN yum install -y curl bash git awscli && yum clean all && rm -rf /var/cache/yum

# Copy Gradle wrapper and build configuration first (these change less frequently)
COPY gradlew ./
COPY gradle/ gradle/
COPY gradle.properties settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew

# Verify Gradle wrapper files are present
RUN echo "🔍 Verifying Gradle wrapper..." && \
    ls -la gradle/wrapper/ && \
    echo "✅ Gradle wrapper files verified"

# Set environment
ENV PIEQ_ENVIRONMENT=preprod

# Pass in CodeArtifact credentials
ARG CODEARTIFACT_AUTH_TOKEN
ARG CODEARTIFACT_REPO_URL
ENV CODEARTIFACT_TOKEN=${CODEARTIFACT_AUTH_TOKEN}
ENV CODEARTIFACT_URL=${CODEARTIFACT_REPO_URL}
ENV CODEARTIFACT_DOMAIN=pieq
ENV CODEARTIFACT_DOMAIN_OWNER=910020091862
ENV CODEARTIFACT_REGION=us-east-1
ENV CODEARTIFACT_REPOSITORY=pieq-artifact

# Set AWS region for CodeArtifact
ENV AWS_DEFAULT_REGION=us-east-1

# Parameterize module names to allow renaming via build args
ARG MODULE_API=agent-management-api
ARG MODULE_APP=agent-management-application
ARG MODULE_CLIENT=agent-management-client
ENV MODULE_API=${MODULE_API}
ENV MODULE_APP=${MODULE_APP}
ENV MODULE_CLIENT=${MODULE_CLIENT}

# Copy source code (this layer will be invalidated when source changes)
COPY ${MODULE_API}/ ${MODULE_API}/
COPY ${MODULE_APP}/ ${MODULE_APP}/
COPY ${MODULE_CLIENT}/ ${MODULE_CLIENT}/

# Build the application with optimizations
# This will download dependencies as part of the build process
RUN ./gradlew build -x test --no-daemon --parallel --build-cache --max-workers=4

# Verify build artifacts exist and copy them
RUN echo "🔍 Checking build artifacts..." && \
    find ${MODULE_APP}/build/ -name "*.jar" -type f && \
    ls -la ${MODULE_APP}/build/libs/ && \
    ls -la ${MODULE_APP}/src/main/resources/ && \
    cp ${MODULE_APP}/src/main/resources/config_*.yml ./ && \
    JAR_FILE=$(find ${MODULE_APP}/build/libs/ -name "${MODULE_APP}-*.jar" | head -1) && \
    echo "🔍 Looking for JAR file pattern: ${MODULE_APP}-*.jar" && \
    echo "🔍 Found JAR file: $JAR_FILE" && \
    if [ -n "$JAR_FILE" ]; then \
        cp "$JAR_FILE" ./app.jar && \
        echo "✅ Build artifacts copied successfully: $JAR_FILE -> app.jar"; \
    else \
        echo "❌ No JAR file found matching pattern: ${MODULE_APP}-*.jar" && \
        echo "Available files in ${MODULE_APP}/build/libs/:" && \
        ls -la ${MODULE_APP}/build/libs/ && \
        exit 1; \
    fi

### Stage 2: Runtime ###
FROM amazoncorretto:21 AS runtime

# Install only runtime dependencies and clean up immediately
RUN yum install -y curl bash tzdata shadow-utils && yum clean all && rm -rf /var/cache/yum

# Add user and group
RUN groupadd -g 1001 appgroup && useradd -u 1001 -g appgroup -m appuser

# Set working directory
WORKDIR /app

# Copy build artifacts
COPY --from=builder /app/app.jar ./app.jar
COPY --from=builder /app/config_*.yml ./

# Set permissions
RUN chown -R appuser:appgroup /app
USER appuser

# Expose ports
EXPOSE 8080 8081

# Set entrypoint
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar server config_${PIEQ_ENVIRONMENT}.yml"]

