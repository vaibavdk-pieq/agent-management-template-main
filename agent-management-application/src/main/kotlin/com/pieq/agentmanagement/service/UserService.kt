package com.pieq.agentmanagement.service

import com.pieq.agentmanagement.dao.UserDao
import com.pieq.core.provider.PieqConfigurationProvider
import com.pieq.core.provider.PieqHttpClientProvider
import com.pieq.core.service.PieqService
import com.pieq.agentmanagement.CreateUserRequest
import com.pieq.agentmanagement.UpdateUserRequest
import com.pieq.agentmanagement.User
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import java.time.LocalDateTime
import java.util.UUID

/**
 * Service class responsible for handling user-related business logic.
 *
 * This service provides methods for creating, retrieving, updating, and deleting users.
 * It handles validation, business rules, and coordinates with the UserDao for data persistence.
 *
 * @param userDao Data access object for user operations
 * @param pieqHttpClientProvider HTTP client provider for external API calls
 * @param pieqApplicationConfigurationProvider Configuration provider for application settings
 */
@Singleton
class UserService
    @Inject
    constructor(
        private val userDao: UserDao,
        pieqHttpClientProvider: PieqHttpClientProvider,
        pieqApplicationConfigurationProvider: PieqConfigurationProvider,
    ) : PieqService<User>(userDao, pieqHttpClientProvider, pieqApplicationConfigurationProvider) {
        override fun initialize() {
            logger.info { "Initializing UserService" }
        }

        /**
         * Creates a new user in the system.
         *
         * Validates that the username and email are unique before creating the user.
         * Automatically generates a UUID and sets creation/update timestamps.
         *
         * @param request The user creation request containing user details
         * @return The created User entity with generated ID and timestamps
         * @throws BadRequestException if username or email already exists
         */
        fun createUser(request: CreateUserRequest): User {
            // Check if username already exists
            if (userDao.findByUsername(request.userName) != null) {
                throw BadRequestException("Username '${request.userName}' already exists")
            }

            // Check if email already exists
            if (userDao.findByEmail(request.email) != null) {
                throw BadRequestException("Email '${request.email}' already exists")
            }

            val now = LocalDateTime.now()
            val user =
                User(
                    guid = UUID.randomUUID(),
                    userName = request.userName,
                    email = request.email,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    active = true,
                    createdAt = now,
                    updatedAt = now,
                )

            return userDao.save(user)
        }

        /**
         * Retrieves a user by their unique identifier.
         *
         * @param guid The unique identifier of the user to retrieve
         * @return The User entity matching the provided GUID
         * @throws NotFoundException if no user exists with the given GUID
         */
        fun getUserByGuid(guid: UUID): User {
            return userDao.findByGuid(guid) ?: throw NotFoundException("User with id '$guid' not found")
        }

        /**
         * Retrieves a user by their username.
         *
         * @param username The username of the user to retrieve
         * @return The User entity matching the provided username
         * @throws NotFoundException if no user exists with the given username
         */
        fun getUserByUsername(username: String): User {
            return userDao.findByUsername(username) ?: throw NotFoundException("User with username '$username' not found")
        }

        /**
         * Retrieves all users with pagination support.
         *
         * Applies validation to limit and offset parameters to prevent abuse.
         * Maximum limit is capped at 100 users per request.
         *
         * @param limit Maximum number of users to return (default: 20, max: 100)
         * @param offset Number of users to skip for pagination (default: 0)
         * @return List of User entities ordered by creation date (newest first)
         */
        fun getAllUsers(
            limit: Int = 20,
            offset: Int = 0,
        ): List<User> {
            val validLimit =
                if (limit > 100) {
                    100
                } else if (limit < 1) {
                    20
                } else {
                    limit
                }
            val validOffset = if (offset < 0) 0 else offset
            return userDao.findAll(validLimit, validOffset)
        }

        /**
         * Updates an existing user's information.
         *
         * Only updates fields that are provided in the request (non-null values).
         * Validates that the new email is unique if being changed.
         * Automatically updates the updatedAt timestamp.
         *
         * @param guid The unique identifier of the user to update
         * @param request The update request containing the fields to modify
         * @return The updated User entity
         * @throws NotFoundException if no user exists with the given GUID
         * @throws BadRequestException if the new email already exists for another user
         */
        fun updateUser(
            guid: UUID,
            request: UpdateUserRequest,
        ): User {
            val existingUser = getUserByGuid(guid) // This will throw NotFoundException if not found

            // Check if email is being updated and already exists for another user
            request.email?.let { newEmail ->
                if (newEmail != existingUser.email && userDao.findByEmail(newEmail) != null) {
                    throw BadRequestException("Email '$newEmail' already exists")
                }
            }

            val updatedUser =
                existingUser.copy(
                    email = request.email ?: existingUser.email,
                    firstName = request.firstName ?: existingUser.firstName,
                    lastName = request.lastName ?: existingUser.lastName,
                    active = request.active ?: existingUser.active,
                    updatedAt = LocalDateTime.now(),
                )

            return userDao.save(updatedUser)
        }

        /**
         * Permanently deletes a user from the system.
         *
         * This operation cannot be undone. Consider deactivating users instead
         * by updating their active status to false.
         *
         * @param guid The unique identifier of the user to delete
         * @throws RuntimeException if the deletion fails
         */
        fun deleteUser(guid: UUID) {
            val result = userDao.delete(guid)
            if (result == false) {
                throw RuntimeException("Failed to delete user")
            }
        }
    } 

