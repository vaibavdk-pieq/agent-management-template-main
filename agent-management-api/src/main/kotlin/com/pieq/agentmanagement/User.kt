package com.pieq.agentmanagement

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a user entity in the system.
 *
 * This data class contains all the information related to a user including their
 * personal details, authentication information, and system metadata.
 *
 * @param guid Unique identifier for the user
 * @param userName Unique username for the user (3-50 characters)
 * @param email User's email address (must be valid email format)
 * @param firstName User's first name (max 100 characters)
 * @param lastName User's last name (max 100 characters)
 * @param active Whether the user account is active or deactivated
 * @param createdAt Timestamp when the user was created
 * @param updatedAt Timestamp when the user was last updated
 */
data class User(
    @JsonProperty("guid")
    val guid: UUID,
    @JsonProperty("username")
    @get:NotBlank(message = "Username cannot be blank")
    @get:Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    val userName: String,
    @JsonProperty("email")
    @get:NotBlank(message = "Email cannot be blank")
    @get:Email(message = "Email must be valid")
    val email: String,
    @JsonProperty("firstName")
    @get:NotBlank(message = "First name cannot be blank")
    @get:Size(max = 100, message = "First name must not exceed 100 characters")
    val firstName: String,
    @JsonProperty("lastName")
    @get:NotBlank(message = "Last name cannot be blank")
    @get:Size(max = 100, message = "Last name must not exceed 100 characters")
    val lastName: String,
    @JsonProperty("active")
    val active: Boolean = true,
    @JsonProperty("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    val createdAt: LocalDateTime,
    @JsonProperty("updatedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    val updatedAt: LocalDateTime,
)

/**
 * Request payload for creating a new user.
 *
 * Contains all the required information to create a new user account.
 * The GUID and timestamps will be generated automatically by the system.
 *
 * @param userName Unique username for the new user (3-50 characters)
 * @param email User's email address (must be valid email format)
 * @param firstName User's first name (max 100 characters)
 * @param lastName User's last name (max 100 characters)
 */
data class CreateUserRequest(
    @JsonProperty("userName")
    @get:NotBlank(message = "Username cannot be blank")
    @get:Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    val userName: String,
    @JsonProperty("email")
    @get:NotBlank(message = "Email cannot be blank")
    @get:Email(message = "Email must be valid")
    val email: String,
    @JsonProperty("firstName")
    @get:NotBlank(message = "First name cannot be blank")
    @get:Size(max = 100, message = "First name must not exceed 100 characters")
    val firstName: String,
    @JsonProperty("lastName")
    @get:NotBlank(message = "Last name cannot be blank")
    @get:Size(max = 100, message = "Last name must not exceed 100 characters")
    val lastName: String,
)

/**
 * Request payload for updating an existing user.
 *
 * All fields are optional - only provided fields will be updated.
 * The username and GUID cannot be changed after user creation.
 *
 * @param email New email address (optional, must be valid email format if provided)
 * @param firstName New first name (optional, max 100 characters if provided)
 * @param lastName New last name (optional, max 100 characters if provided)
 * @param active New active status (optional, true to activate, false to deactivate)
 */
data class UpdateUserRequest(
    @JsonProperty("email")
    @get:Email(message = "Email must be valid")
    val email: String?,
    @JsonProperty("firstName")
    @get:Size(max = 100, message = "First name must not exceed 100 characters")
    val firstName: String?,
    @JsonProperty("lastName")
    @get:Size(max = 100, message = "Last name must not exceed 100 characters")
    val lastName: String?,
    @JsonProperty("active")
    val active: Boolean?,
) 
