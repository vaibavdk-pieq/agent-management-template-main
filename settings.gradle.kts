rootProject.name = "pieq-api-agentmanagement"

include("agent-management-api", "agent-management-client", "agent-management-application")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
}

