import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spotless)
    alias(libs.plugins.jacoco)
}

// CodeArtifact Configuration Object
object CodeArtifactConfig {
    fun getDomain(project: Project): String {
        return project.findProperty("codeartifact.domain") as String? 
            ?: throw GradleException("Missing required property: codeartifact.domain")
    }
    
    fun getDomainOwner(project: Project): String {
        return project.findProperty("codeartifact.domainOwner") as String? 
            ?: throw GradleException("Missing required property: codeartifact.domainOwner")
    }
    
    fun getRegion(project: Project): String {
        return project.findProperty("codeartifact.region") as String? 
            ?: throw GradleException("Missing required property: codeartifact.region")
    }
    
    fun getRepository(project: Project): String {
        return project.findProperty("codeartifact.repository") as String? 
            ?: throw GradleException("Missing required property: codeartifact.repository")
    }
    
    fun getUrl(project: Project): String {
        val domain = getDomain(project)
        val domainOwner = getDomainOwner(project)
        val region = getRegion(project)
        val repository = getRepository(project)
        
        return "https://$domain-$domainOwner.d.codeartifact.$region.amazonaws.com/maven/$repository/"
    }
    
    fun validateConfiguration(project: Project) {
        // Validate that all required properties are present and non-empty
        try {
            getDomain(project)
            getDomainOwner(project)
            getRegion(project)
            getRepository(project)
        } catch (e: GradleException) {
            throw e
        } catch (e: Exception) {
            throw GradleException("Failed to validate CodeArtifact configuration: ${e.message}")
        }
    }
}

// function to get code artifact token with improved error handling
fun getCodeArtifactToken(): String? {
    // Cache the token to avoid multiple AWS CLI calls
    if (!project.hasProperty("_cached_codeartifact_token")) {
        val codeArtifactToken = System.getenv("CODEARTIFACT_AUTH_TOKEN") ?: try {
            // Validate configuration before making AWS call
            CodeArtifactConfig.validateConfiguration(project)
            
            val token = providers.exec {
                commandLine("aws",
                    "codeartifact",
                    "get-authorization-token",
                    "--domain", CodeArtifactConfig.getDomain(project),
                    "--domain-owner", CodeArtifactConfig.getDomainOwner(project),
                    "--region", CodeArtifactConfig.getRegion(project),
                    "--query", "authorizationToken",
                    "--output", "text")
            }.standardOutput.asText.get().trim()
            
            if (token.isBlank()) {
                throw GradleException("Received empty token from AWS CodeArtifact")
            }
            
            println("code artifact token: ${token.take(20)}...")

            // Cache the token
            project.ext.set("_cached_codeartifact_token", token)
            token
        } catch (e: Exception) {
            println("Could not get CodeArtifact token: ${e.message}")
            project.ext.set("_cached_codeartifact_token", null)
            null
        }
        return codeArtifactToken
    }
    return project.property("_cached_codeartifact_token") as String?
}

// function to add a code artifact repository with improved validation
fun RepositoryHandler.awsCodeArtifactRepository() {
    // Check if CodeArtifact repository is already added to avoid duplicates
    if (this.any { it.name?.contains("codeartifact", ignoreCase = true) == true }) {
        return
    }
    
    val codeArtifactToken = getCodeArtifactToken()
    if (codeArtifactToken != null) {
        maven {
            name = "aws-codeartifact"
            url = uri(CodeArtifactConfig.getUrl(project))
            credentials {
                username = "aws"
                password = codeArtifactToken
            }
            // allow insecure protocols for test builds
            isAllowInsecureProtocol = false
        }
    } else {
        println("Skipping CodeArtifact repository setup - no valid token available")
    }
}

// function to add common dependencies to all projects
fun DependencyHandler.addCommonDependencies(libs: VersionCatalog) {
    // kotlin dependencies
    add("implementation", libs.findLibrary("kotlin.std.lib").get())
    add("implementation", libs.findLibrary("kotlin.reflect").get())
    add("implementation", libs.findLibrary("kotlin.logging").get())

    // dropwizard dependencies
    add("implementation", libs.findLibrary("dropwizard.core").get())
    add("implementation", libs.findLibrary("dropwizard.jetty").get())
    add("implementation", libs.findLibrary("dropwizard.jdbi3").get())
    add("implementation", libs.findLibrary("dropwizard.validation").get())

    // jackson dependencies
    add("implementation", libs.findLibrary("jackson.module.kotlin").get())
    add("implementation", libs.findLibrary("jackson.datatype.jsr310").get())

    // jakarta.rs.api and jakarta.inject.api dependencies
    add("implementation", libs.findLibrary("jakarta.rs.api").get())
    add("implementation", libs.findLibrary("jakarta.inject.api").get())

    // sentry
    add("implementation", libs.findLibrary("sentry").get())

    // test dependencies
    add("testImplementation", libs.findLibrary("dropwizard.testing").get())
    add("testImplementation", libs.findLibrary("kotlin.test.junit5").get())
    add("testImplementation", libs.findLibrary("junit.jupiter").get())
    add("testImplementation", libs.findLibrary("mockk").get())
}

// add maven central and aws code artifact repository to all projects
allprojects {
    repositories {
        mavenCentral()
        awsCodeArtifactRepository()
    }
}

// apply common configuration to all subprojects
subprojects {
    // access the version catalog
    val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

    // apply JaCoCo plugin to all subprojects
    apply(plugin = "jacoco")

    // apply plugin from catalog
    plugins.withId("org.jetbrains.kotlin.jvm") {

        // set JVM toolchain
        extensions.getByType<KotlinJvmProjectExtension>().jvmToolchain(21)

        // set Kotlin compiler options
        tasks.withType<KotlinCompile>().configureEach {
            kotlinOptions {
                jvmTarget = "21"
                freeCompilerArgs += listOf("-Xjsr305=strict")
            }
        }

        // common dependencies across all projects
        dependencies {
            addCommonDependencies(libs)
        }
    }

    // apply plugin spotless
    plugins.withId("com.diffplug.spotless") {
        extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension>("spotless") {
            kotlin {
                target("src/**/*.kt")
                ktlint()
            }
            format("misc") {
                target("**/*.gradle.kts", "**/*.md", "**/.gitignore")
                trimTrailingWhitespace()
                endWithNewline()
            }
        }
    }

    // publish to maven
    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension>("publishing") {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                    groupId = project.group.toString()
                    artifactId = project.name
                    version = project.version.toString()
                    
                    // Add POM metadata for pieq-client
                    if (project.name == "pieq-client") {
                        pom {
                            name.set("PIEQ Client Library")
                            description.set("HTTP client library for consuming PIEQ User API endpoints")
                            url.set("https://github.com/pieq-ai/pieq-api-template")
                            
                            licenses {
                                license {
                                    name.set("MIT License")
                                    url.set("https://opensource.org/licenses/MIT")
                                }
                            }
                            
                            developers {
                                developer {
                                    id.set("pieq-team")
                                    name.set("PIEQ Development Team")
                                    email.set("dev@pieq.ai")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // apply Jar task configuration to all subprojects
    tasks.withType<Jar>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    // apply junit platform to all test tasks
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    // apply spotless to the build task
    tasks.findByName("build")?.let {
        it.dependsOn("spotlessApply")
    }

    // centralized CodeArtifact configuration task
    tasks.register("generateCodeArtifactConfig") {
        group = "CodeArtifact"
        description = "Generate CodeArtifact configuration for CI/CD workflows and Docker builds"

        doLast {
            // Validate configuration before generating
            CodeArtifactConfig.validateConfiguration(project)
            
            val domain = CodeArtifactConfig.getDomain(project)
            val domainOwner = CodeArtifactConfig.getDomainOwner(project)
            val region = CodeArtifactConfig.getRegion(project)
            val repository = CodeArtifactConfig.getRepository(project)
            val url = CodeArtifactConfig.getUrl(project)

            // Generate environment variables for CI/CD and Docker builds
            val envVars = """
                # CodeArtifact Configuration (generated by Gradle)
                CODEARTIFACT_DOMAIN=$domain
                CODEARTIFACT_DOMAIN_OWNER=$domainOwner
                CODEARTIFACT_REGION=$region
                CODEARTIFACT_REPOSITORY=$repository
                CODEARTIFACT_URL=$url
            """.trimIndent()

            // Generate in root project directory
            val envFile = rootProject.file("codeartifact.env")
            envFile.writeText(envVars)

            println("Generated CodeArtifact configuration:")
            println("  Environment file: ${envFile.absolutePath}")
            println("  Domain: $domain")
            println("  Domain Owner: $domainOwner")
            println("  Region: $region")
            println("  Repository: $repository")
            println("  URL: $url")
            println("")
            println("Usage:")
            println("  - For Docker builds: Use with docker-build-with-codeartifact.sh")
            println("  - For CI/CD: Use with setup-codeartifact.sh")
            println("  - For local development: source codeartifact.env")
        }
    }

    // task to get CodeArtifact token and set it as environment variable
    tasks.register("getCodeArtifactToken") {
        group = "CodeArtifact"
        description = "Get CodeArtifact authorization token and set it as environment variable"

        doLast {
            try {
                // Validate configuration before getting token
                CodeArtifactConfig.validateConfiguration(project)
                
                val token = getCodeArtifactToken()

                if (token == null) {
                    throw GradleException("AWS CodeArtifact token is null")
                }

                // Set environment variable for this process
                System.setProperty("CODE_ARTIFACT_TOKEN", token)

                // Also write to a file for CI/CD workflows
                file("codeartifact.token").writeText(token)

                println("CodeArtifact token retrieved and set as environment variable and in a property file")
                println("   Token saved as property 'codeartifact.token' and 'CODE_ARTIFACT_TOKEN' as environment variable")

            } catch (e: Exception) {
                println("Failed to get CodeArtifact token: ${e.message}")
                throw e
            }
        }
    }

    // task to verify CodeArtifact configuration
    tasks.register("verifyCodeArtifactConfig") {
        group = "CodeArtifact"
        description = "Verify CodeArtifact configuration and connectivity"

        dependsOn("getCodeArtifactToken")

        doLast {
            // Validate configuration before verification
            CodeArtifactConfig.validateConfiguration(project)
            
            val domain = CodeArtifactConfig.getDomain(project)
            val domainOwner = CodeArtifactConfig.getDomainOwner(project)
            val region = CodeArtifactConfig.getRegion(project)
            val repository = CodeArtifactConfig.getRepository(project)

            println("🔍 Verifying CodeArtifact configuration...")
            println("  Domain: $domain")
            println("  Domain Owner: $domainOwner")
            println("  Region: $region")
            println("  Repository: $repository")

            try {
                // Test connectivity by listing packages
                val result = providers.exec {
                    commandLine("aws",
                        "codeartifact",
                        "list-packages",
                        "--domain", domain,
                        "--domain-owner", domainOwner,
                        "--repository", repository,
                        "--region", region,
                        "--max-items", "5")
                }.standardOutput.asText.get()

                println("CodeArtifact connectivity verified")
                println("Sample packages available:")
                println(result.lines().take(10).joinToString("\n"))

            } catch (e: Exception) {
                println("Warning: Could not verify CodeArtifact connectivity: ${e.message}")
            }
        }
    }

    // task to monitor CodeArtifact repository usage
    tasks.register("monitorCodeArtifactUsage") {
        group = "CodeArtifact"
        description = "Monitor and debug CodeArtifact repository usage across projects"

        doLast {
            println("=== CodeArtifact Repository Usage Monitor ===")
            println("Root project repositories:")
            rootProject.repositories.forEach { repo ->
                val repoUrl = when (repo) {
                    is MavenArtifactRepository -> repo.url.toString()
                    else -> "unknown type"
                }
                println("  - ${repo.name ?: "unnamed"}: $repoUrl")
            }
            
            println("\nSubproject repositories:")
            subprojects.forEach { subproject ->
                println("  ${subproject.name}:")
                subproject.repositories.forEach { repo ->
                    val repoUrl = when (repo) {
                        is MavenArtifactRepository -> repo.url.toString()
                        else -> "unknown type"
                    }
                    println("    - ${repo.name ?: "unnamed"}: $repoUrl")
                }
            }
            
            println("\nCached token status: ${if (project.hasProperty("_cached_codeartifact_token")) "Available" else "Not cached"}")
        }
    }

    // JaCoCo aggregate report configuration
    tasks.register<JacocoReport>("jacocoAggregateReport") {
        dependsOn(subprojects.map { it.tasks.named("test") })

        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }

        // Collect execution data from all subprojects
        executionData(
            files(subprojects.map { it.tasks.named("test").get().outputs.files })
                .filter { it.exists() }
                .map { fileTree(it) { include("**/jacoco/test.exec") } }
        )

        // Collect source files from all subprojects
        sourceDirectories.setFrom(
            files(subprojects.map { it.extensions.findByType<SourceSetContainer>()?.getByName("main")?.allSource?.srcDirs }.filterNotNull().flatten())
        )

        // Collect class files from all subprojects
        classDirectories.setFrom(
            files(subprojects.map { it.extensions.findByType<SourceSetContainer>()?.getByName("main")?.output }.filterNotNull())
        )

        // Customize the report
        doLast {
            println("JaCoCo aggregate report generated:")
            println("  HTML: ${reports.html.outputLocation.get()}")
            println("  XML:  ${reports.xml.outputLocation.get()}")
        }
    }

    // JaCoCo coverage verification
    tasks.register<JacocoCoverageVerification>("jacocoAggregateCoverageVerification") {
        dependsOn(tasks.named("jacocoAggregateReport"))

        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.8".toBigDecimal()
                }
            }
            rule {
                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = "0.7".toBigDecimal()
                }
            }
        }

        executionData(
            files(subprojects.map { it.tasks.named("test").get().outputs.files })
                .filter { it.exists() }
                .map { fileTree(it) { include("**/jacoco/test.exec") } }
        )

        sourceDirectories.setFrom(
            files(subprojects.map { it.extensions.findByType<SourceSetContainer>()?.getByName("main")?.allSource?.srcDirs }.filterNotNull().flatten())
        )

        classDirectories.setFrom(
            files(subprojects.map { it.extensions.findByType<SourceSetContainer>()?.getByName("main")?.output }.filterNotNull())
        )
    }
}