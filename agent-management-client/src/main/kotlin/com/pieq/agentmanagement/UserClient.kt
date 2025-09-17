package com.pieq.agentmanagement

import com.pieq.core.provider.PieqHttpClientProvider
import com.pieq.agentmanagement.CreateUserRequest
import com.pieq.agentmanagement.UpdateUserRequest
import com.pieq.agentmanagement.User
import jakarta.ws.rs.BadRequestException
import mu.KotlinLogging
import java.util.UUID

/**
 * Client for interacting with the User API endpoints using PieqHttpClient
 */
class UserClient(
    private val baseUrl: String,
    private val pieqHttpClientProvider: PieqHttpClientProvider,
) {
    private val logger = KotlinLogging.logger {}
    private val httpClient = pieqHttpClientProvider.get()
    private val basePath = "$baseUrl/api/users"

    /**
     * Get all users with optional limit
     */
    fun getAllUsers(limit: Int = 20): List<User> {
        logger.info { "Fetching all users with limit: $limit" }

        val url = "$basePath/all?limit=$limit"
        val response = httpClient.get(url)

        return when (response.status) {
            200 -> response.readEntity(Array<User>::class.java).toList()
            else -> {
                val errorBody = response.readEntity(String::class.java)
                throw RuntimeException("Failed to get users: ${response.status} - $errorBody")
            }
        }
    }

    /**
     * Get user by GUID
     */
    fun getUserByGuid(guid: UUID): User {
        logger.info { "Fetching user by GUID: $guid" }

        val url = "$basePath/$guid"
        val response = httpClient.get(url)

        return when (response.status) {
            200 -> response.readEntity(User::class.java)
            404 -> throw RuntimeException("User not found with GUID: $guid")
            400 -> throw BadRequestException("Invalid UUID format: $guid")
            else -> {
                val errorBody = response.readEntity(String::class.java)
                throw RuntimeException("Failed to get user: ${response.status} - $errorBody")
            }
        }
    }

    /**
     * Get user by username
     */
    fun getUserByUsername(username: String): User {
        logger.info { "Fetching user by username: $username" }

        val url = "$basePath/userName/$username"
        val response = httpClient.get(url)

        return when (response.status) {
            200 -> response.readEntity(User::class.java)
            404 -> throw RuntimeException("User not found with username: $username")
            else -> {
                val errorBody = response.readEntity(String::class.java)
                throw RuntimeException("Failed to get user: ${response.status} - $errorBody")
            }
        }
    }

    /**
     * Create a new user
     */
    fun createUser(request: CreateUserRequest): User {
        logger.info { "Creating user with username: ${request.userName}" }

        val headers = mapOf("Content-Type" to "application/json")
        val response = httpClient.post(basePath, request, headers)

        return when (response.status) {
            201 -> response.readEntity(User::class.java)
            400 -> throw BadRequestException("Invalid user data provided")
            else -> {
                val errorBody = response.readEntity(String::class.java)
                throw RuntimeException("Failed to create user: ${response.status} - $errorBody")
            }
        }
    }

    /**
     * Update an existing user
     */
    fun updateUser(
        guid: UUID,
        request: UpdateUserRequest,
    ): User {
        logger.info { "Updating user with GUID: $guid" }

        val url = "$basePath/$guid"
        val headers = mapOf("Content-Type" to "application/json")
        val response = httpClient.put(url, request, headers)

        return when (response.status) {
            200 -> response.readEntity(User::class.java)
            404 -> throw RuntimeException("User not found with GUID: $guid")
            400 -> throw BadRequestException("Invalid user data or UUID format")
            else -> {
                val errorBody = response.readEntity(String::class.java)
                throw RuntimeException("Failed to update user: ${response.status} - $errorBody")
            }
        }
    }

    /**
     * Deactivate a user
     */
    fun deactivateUser(
        guid: UUID,
        request: UpdateUserRequest,
    ): User {
        logger.info { "Deactivating user with GUID: $guid" }

        val url = "$basePath/$guid/deactivate"
        val headers = mapOf("Content-Type" to "application/json")
        val response = httpClient.post(url, request, headers)

        return when (response.status) {
            200 -> response.readEntity(User::class.java)
            404 -> throw RuntimeException("User not found with GUID: $guid")
            400 -> throw BadRequestException("Invalid user data or UUID format")
            else -> {
                val errorBody = response.readEntity(String::class.java)
                throw RuntimeException("Failed to deactivate user: ${response.status} - $errorBody")
            }
        }
    }

    /**
     * Delete a user
     */
    fun deleteUser(guid: UUID) {
        logger.info { "Deleting user with GUID: $guid" }

        val url = "$basePath/$guid"
        val response = httpClient.delete(url)

        when (response.status) {
            204 -> logger.info { "User deleted successfully" }
            404 -> throw RuntimeException("User not found with GUID: $guid")
            400 -> throw BadRequestException("Invalid UUID format: $guid")
            else -> {
                val errorBody = response.readEntity(String::class.java)
                throw RuntimeException("Failed to delete user: ${response.status} - $errorBody")
            }
        }
    }

    /**
     * Close the client connection if needed
     * Note: PieqHttpClient may have different lifecycle management
     */
    fun close() {
        // PieqHttpClient lifecycle is managed by the provider
        // No explicit close needed for individual clients
        logger.info { "UserClient closed" }
    }
} 

