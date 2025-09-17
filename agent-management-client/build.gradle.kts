plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

group = "com.pieq"

// Simple version increment based on last digit
// Use Gradle property if provided, otherwise use hardcoded version
version = project.findProperty("version") as String? ?: "1.0.1"

dependencies {
    // project-specific dependencies only
    implementation(project(":agent-management-application"))
}

// Publishing configuration for CodeArtifact
publishing {
    repositories {
        // Only add CodeArtifact repository if token is available
        if (providers.environmentVariable("CODEARTIFACT_AUTH_TOKEN").isPresent) {
            maven {
                name = "aws-codeartifact"
                url = uri("https://pieq-910020091862.d.codeartifact.us-east-1.amazonaws.com/maven/pieq-artifact/")
                credentials {
                    username = "aws"
                    password = providers.environmentVariable("CODEARTIFACT_AUTH_TOKEN").get()
                }
            }
        }
    }
}

