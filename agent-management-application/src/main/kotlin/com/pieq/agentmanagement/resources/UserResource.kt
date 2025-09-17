package com.pieq.agentmanagement.resources

import com.pieq.agentmanagement.service.UserService
import com.pieq.core.annotation.PieqAuthorization
import com.pieq.core.resource.PieqResource
import com.pieq.agentmanagement.CreateUserRequest
import com.pieq.agentmanagement.UpdateUserRequest
import com.pieq.agentmanagement.User
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import java.util.UUID

/**
 * REST API resource for user management operations.
 *
 * This resource provides endpoints for creating, retrieving, updating, and deleting users.
 * All endpoints require appropriate authorization scopes for access control.
 *
 * Base path: /api/users
 * Default authorization: user:read scope required
 *
 * @param userService Service layer for user business logic
 */
@Path("/api/users")
@PieqAuthorization(scopes = ["user:read"])
class UserResource
    @Inject
    constructor(
        private val userService: UserService,
    ) : PieqResource<User>() {
        /**
         * Retrieves all users with optional pagination.
         *
         * GET /api/users/all?limit={limit}
         *
         * @param limit Maximum number of users to return (default: 20, max: 100)
         * @return HTTP 200 with list of users in response body
         */
        @GET
        @Path("/all")
        fun getAllUsers(
            @QueryParam("limit") @DefaultValue("20") limit: Int,
        ): Response {
            val users = userService.getAllUsers(limit)
            return Response.ok(users).build()
        }

        /**
         * Retrieves a specific user by their unique identifier.
         *
         * GET /api/users/{guid}
         *
         * @param guid String representation of the user's UUID
         * @return HTTP 200 with user data, HTTP 404 if not found, HTTP 400 for invalid UUID
         * @throws BadRequestException if the provided GUID is not a valid UUID format
         */
        @GET
        @Path("/{guid}")
        fun getUserByGuid(
            @PathParam("guid") guid: String,
        ): Response {
            val uuid =
                try {
                    UUID.fromString(guid)
                } catch (e: IllegalArgumentException) {
                    throw BadRequestException("Invalid UUID format: $guid")
                }

            val user = userService.getUserByGuid(uuid)
            return Response.ok(user).build()
        }

        /**
         * Retrieves a specific user by their username.
         *
         * GET /api/users/userName/{userName}
         *
         * @param userName The username to search for
         * @return HTTP 200 with user data, HTTP 404 if not found
         */
        @GET
        @Path("/userName/{userName}")
        fun getUserByUsername(
            @PathParam("userName") userName: String,
        ): Response {
            val user = userService.getUserByUsername(userName)
            return Response.ok(user).build()
        }

        /**
         * Creates a new user in the system.
         *
         * POST /api/users
         * Requires user:write authorization scope.
         *
         * @param request Validated user creation request containing user details
         * @return HTTP 201 with created user data, HTTP 400 for validation errors or duplicate data
         */
        @POST
        @PieqAuthorization(scopes = ["user:write"])
        fun createUser(
            @Valid request: CreateUserRequest,
        ): Response {
            val user = userService.createUser(request)
            return Response.status(Response.Status.CREATED).entity(user).build()
        }

        /**
         * Updates an existing user's information.
         *
         * PUT /api/users/{guid}
         * Requires user:write authorization scope.
         *
         * @param guid String representation of the user's UUID to update
         * @param request Validated user update request containing fields to modify
         * @return HTTP 200 with updated user data, HTTP 404 if not found, HTTP 400 for invalid data
         * @throws BadRequestException if the provided GUID is not a valid UUID format
         */
        @PUT
        @Path("/{guid}")
        @PieqAuthorization(scopes = ["user:write"])
        fun updateUser(
            @PathParam("guid") guid: String,
            @Valid request: UpdateUserRequest,
        ): Response {
            val uuid =
                try {
                    UUID.fromString(guid)
                } catch (e: IllegalArgumentException) {
                    throw BadRequestException("Invalid UUID format: $guid")
                }

            val user = userService.updateUser(uuid, request)
            return Response.ok(user).build()
        }

        /**
         * Deactivates a user account.
         *
         * POST /api/users/{id}/deactivate
         * Requires user:write authorization scope.
         *
         * Note: This endpoint appears to be a duplicate of the update functionality.
         * Consider using PUT /api/users/{id} with active=false instead.
         *
         * @param id String representation of the user's UUID to deactivate
         * @param updateUserRequest User update request (typically with active=false)
         * @return HTTP 200 with updated user data, HTTP 404 if not found, HTTP 400 for invalid data
         * @throws BadRequestException if the provided ID is not a valid UUID format
         */
        @POST
        @Path("/{id}/deactivate")
        @PieqAuthorization(scopes = ["user:write"])
        fun deactivateUser(
            @PathParam("id") id: String,
            @Valid updateUserRequest: UpdateUserRequest,
        ): Response {
            val uuid =
                try {
                    UUID.fromString(id)
                } catch (e: IllegalArgumentException) {
                    throw BadRequestException("Invalid UUID format: $id")
                }

            val user = userService.updateUser(uuid, updateUserRequest)
            return Response.ok(user).build()
        }

        /**
         * Permanently deletes a user from the system.
         *
         * DELETE /api/users/{id}
         * Requires user:write authorization scope.
         *
         * Warning: This operation cannot be undone. Consider deactivating users instead.
         *
         * @param id String representation of the user's UUID to delete
         * @return HTTP 204 (No Content) on successful deletion, HTTP 404 if not found, HTTP 400 for invalid UUID
         * @throws BadRequestException if the provided ID is not a valid UUID format
         */
        @DELETE
        @Path("/{id}")
        @PieqAuthorization(scopes = ["user:write"])
        fun deleteUser(
            @PathParam("id") id: String,
        ): Response {
            val uuid =
                try {
                    UUID.fromString(id)
                } catch (e: IllegalArgumentException) {
                    throw BadRequestException("Invalid UUID format: $id")
                }

            userService.deleteUser(uuid)
            return Response.noContent().build()
        }
    } 

