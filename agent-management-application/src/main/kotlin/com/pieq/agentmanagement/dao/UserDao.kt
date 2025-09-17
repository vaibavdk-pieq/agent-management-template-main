package com.pieq.agentmanagement.dao

import com.pieq.core.dao.PieqDao
import com.pieq.core.provider.PieqDbClientProvider
import com.pieq.agentmanagement.User
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.jdbi.v3.core.Jdbi
import java.util.UUID

/**
 * Data Access Object for User entities using JDBI and PostgreSQL.
 *
 * This DAO provides methods for performing CRUD operations on user data in the database.
 * Uses ObjectMapper for cleaner parameter binding and result mapping.
 * All database operations are logged for debugging and monitoring purposes.
 *
 * @param pieqDbClientProvider Database client provider for obtaining JDBI instance
 */
@Singleton
class UserDao
    @Inject
    constructor(pieqDbClientProvider: PieqDbClientProvider) : PieqDao<User>(pieqDbClientProvider) {
        private val jdbi: Jdbi = pieqDbClientProvider.getDbClient()

        /**
         * Finds a user by their unique identifier.
         *
         * @param guid The unique identifier of the user to find
         * @return The User entity if found, null otherwise
         */
        override fun findByGuid(guid: UUID): User? {
            logger.info { "Finding user with guid: $guid" }
            return jdbi.withHandle<User?, Exception> { handle ->
                handle.createQuery(
                    """
                    SELECT guid, active as "active", user_name as "userName", email, first_name as "firstName", 
                    last_name as "lastName", created_at as "createdAt", updated_at as "updatedAt"
                    FROM public."USER"
                    WHERE guid = :guid
                    """,
                )
                    .bind("guid", guid)
                    .mapTo(User::class.java)
                    .findFirst()
                    .orElse(null)
            }
        }

        /**
         * Saves a user entity to the database.
         *
         * Performs an upsert operation - updates existing user if found by GUID,
         * otherwise inserts a new user record. Timestamps are automatically managed.
         *
         * @param entity The User entity to save
         * @return The saved User entity with updated timestamps from the database
         */
        override fun save(entity: User): User {
            logger.info { "Saving user: ${entity.guid}" }
            return jdbi.withHandle<User, Exception> { handle ->
                val existingUser = findByGuid(entity.guid)
                if (existingUser != null) {
                    handle.createUpdate(
                        """
                        UPDATE public."USER"
                        SET active = :active, user_name = :userName, email = :email, first_name = :firstName, 
                        last_name = :lastName, updated_at = CURRENT_TIMESTAMP
                        WHERE guid = :guid
                        """,
                    )
                        .bindBean(entity)
                        .execute()
                } else {
                    handle.createUpdate(
                        """
                        INSERT INTO public."USER" (guid, active, user_name, email, first_name, last_name, created_at, updated_at)
                        VALUES (:guid, :active, :userName, :email, :firstName, :lastName, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                    )
                        .bindBean(entity)
                        .execute()
                }
                // Return the entity with updated timestamps
                findByGuid(entity.guid) ?: entity
            }
        }

        /**
         * Permanently deletes a user from the database.
         *
         * @param guid The unique identifier of the user to delete
         * @return true if the user was successfully deleted, false if no user was found
         */
        override fun delete(guid: UUID): Boolean {
            logger.info { "Deleting user with guid: $guid" }
            return jdbi.withHandle<Boolean, Exception> { handle ->
                val rowsAffected =
                    handle.createUpdate("DELETE FROM public.\"USER\" WHERE guid = :guid")
                        .bind("guid", guid)
                        .execute()
                rowsAffected > 0
            }
        }

        /**
         * Finds a user by their email address.
         *
         * @param email The email address to search for
         * @return The User entity if found, null otherwise
         */
        fun findByEmail(email: String): User? {
            logger.info { "Finding user with email: $email" }
            return jdbi.withHandle<User?, Exception> { handle ->
                handle.createQuery(
                    """
                    SELECT guid, active, user_name as "userName", email, first_name as "firstName", 
                    last_name as "lastName", created_at as "createdAt", updated_at as "updatedAt"
                    FROM public."USER"
                    WHERE email = :email
                    """,
                )
                    .bind("email", email)
                    .mapTo(User::class.java)
                    .findFirst()
                    .orElse(null)
            }
        }

        /**
         * Finds a user by their username.
         *
         * @param userName The username to search for
         * @return The User entity if found, null otherwise
         */
        fun findByUsername(userName: String): User? {
            logger.info { "Finding user with userName: $userName" }
            return jdbi.withHandle<User?, Exception> { handle ->
                handle.createQuery(
                    """
                    SELECT guid, active, user_name as "userName", email, first_name as "firstName", 
                    last_name as "lastName", created_at as "createdAt", updated_at as "updatedAt" 
                    FROM public."USER"     
                    WHERE user_name = :userName
                    """,
                )
                    .bind("userName", userName)
                    .mapTo(User::class.java)
                    .findFirst()
                    .orElse(null)
            }
        }

        /**
         * Retrieves all users with pagination support.
         *
         * Users are ordered by creation date in descending order (newest first).
         *
         * @param limit Maximum number of users to return
         * @param offset Number of users to skip for pagination
         * @return List of User entities matching the pagination criteria
         */
        fun findAll(
            limit: Int,
            offset: Int,
        ): List<User> {
            logger.info { "Finding all users: limit: $limit, offset: $offset" }
            return jdbi.withHandle<List<User>, Exception> { handle ->
                handle.createQuery(
                    """
                    SELECT guid, active, user_name as "userName", email, first_name as "firstName", 
                    last_name as "lastName", created_at as "createdAt", updated_at as "updatedAt"
                    FROM public."USER"
                    ORDER BY created_at DESC LIMIT :limit OFFSET :offset
                    """,
                )
                    .bind("limit", limit)
                    .bind("offset", offset)
                    .mapTo(User::class.java)
                    .list()
            }
        }
    } 

