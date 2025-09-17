plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.application)
}

application {
    mainClass.set("com.pieq.agentmanagement.AgentManagementApiApplicationKt")
}

// Configure the jar task to include dependencies and manifest
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.pieq.agentmanagement.AgentManagementApiApplicationKt"
    }
    // Include all dependencies in the JAR (with error handling)
    from(configurations.runtimeClasspath.map { config ->
        config.filter { it.exists() }.map { if (it.isDirectory) it else zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<JavaExec>("run") {
    jvmArgs("-Duser.timezone=UTC")
    workingDir = project.rootDir
    // Ensure the application can find config files in resources
    // args("server", "config_dev.yml")
}


dependencies {
    // project-specific dependencies only
    implementation(project(":agent-management-api"))
    implementation(libs.pieq.core.lib)
    implementation(libs.pieq.http.client.lib)
}
