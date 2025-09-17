package com.pieq.agentmanagement

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.pieq.agentmanagement.config.AgentManagementApiConfiguration
import com.pieq.agentmanagement.dao.UserDao
import com.pieq.agentmanagement.resources.UserResource
import com.pieq.agentmanagement.service.UserService
import com.pieq.core.application.PieqApplication
import io.dropwizard.core.setup.Bootstrap
import io.dropwizard.core.setup.Environment
import io.dropwizard.jetty.ConnectorFactory
import io.dropwizard.jetty.HttpConnectorFactory
import io.dropwizard.jetty.HttpsConnectorFactory

/**
 * Main application class for the Pieq API server.
 *
 * This application extends the PieqApplication base class and provides user management
 * functionality through REST APIs. It configures Jackson for JSON serialization,
 * sets up dependency injection, and registers resources and services.
 *
 * Features:
 * - User CRUD operations via REST API
 * - JSON serialization with proper date/time handling
 * - Kotlin support in Jackson
 * - Database connectivity with health checks
 * - Logging configuration
 */
class AgentManagementApiApplication : PieqApplication<AgentManagementApiConfiguration>() {
    /**
     * Initializes the application bootstrap configuration.
     *
     * Configures Jackson ObjectMapper to support Kotlin data classes
     * and proper serialization/deserialization.
     *
     * @param bootstrap The Dropwizard bootstrap configuration
     */
    override fun initialize(bootstrap: Bootstrap<AgentManagementApiConfiguration>) {
        // Add Kotlin support to Jackson
        bootstrap.objectMapper.registerModule(KotlinModule.Builder().build())

        // Register HTTP connector subtypes for proper type resolution
        // This is required because the base PieqApplication may interfere with Dropwizard's default registration
        bootstrap.objectMapper.registerSubtypes(
            HttpConnectorFactory::class.java,
            HttpsConnectorFactory::class.java,
        )

        // Also register the base ConnectorFactory to ensure proper polymorphic deserialization
        bootstrap.objectMapper.registerSubtypes(ConnectorFactory::class.java)
    }

    /**
     * Runs the application with the provided configuration and environment.
     *
     * This method:
     * 1. Calls the parent run method to initialize core functionality
     * 2. Configures Jackson ObjectMapper for proper date/time serialization
     * 3. Creates and initializes DAOs and services
     * 4. Registers REST resources with JAX-RS
     * 5. Sets up health checks
     *
     * @param configuration The application configuration loaded from config_dev.yml
     * @param environment The Dropwizard environment for registering components
     */
    override fun run(
        configuration: AgentManagementApiConfiguration,
        environment: Environment,
    ) {
        // Call the parent run method first
        super.run(configuration, environment)

        // Log and validate database configuration
        logger.info { "Validating database configuration..." }
        logger.info { "Database driver: ${configuration.database.driverClass}" }
        logger.info { "Database user: ${configuration.database.user}" }
        logger.info { "Database URL: ${configuration.database.url}" }

        // Create a new instance of the UserDao
        val userDao = UserDao(this.pieqDbClientProvider)
        this.logger.info("Created UserDao instance")

        // Create Service instances
        val userService = UserService(userDao, this.pieqHttpClientProvider, this.pieqApplicationConfigurationProvider)
        userService.initialize()
        logger.info { "Created and initialized UserService" }

        // Create and register UserResource
        val userResource = UserResource(userService)
        environment.jersey().register(userResource)
        logger.info { "Created and registered UserResource" }

        // Add health checks if needed
        // TODO: Add additoinal health checks. Database health check is already added in the parent class.
    }
}

/**
 * Application entry point.
 *
 * Creates and runs the PieqApiApplication with command line arguments.
 * Supports standard Dropwizard command line options like 'server config_dev.yml'.
 *
 * @param args Command line arguments passed to the application
 */
fun main(args: Array<String>) {
    AgentManagementApiApplication().run(*args)
} 

