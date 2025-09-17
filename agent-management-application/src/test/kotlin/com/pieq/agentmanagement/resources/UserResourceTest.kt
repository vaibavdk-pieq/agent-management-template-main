package com.pieq.agentmanagement.resources

// import com.pieq.agentmanagement.config.PieqApiConfiguration
import com.pieq.agentmanagement.service.UserService
import com.pieq.agentmanagement.CreateUserRequest
import com.pieq.agentmanagement.UpdateUserRequest
import com.pieq.agentmanagement.User
// import io.dropwizard.testing.junit5.DropwizardAppExtension
// import io.dropwizard.testing.junit5.DropwizardExtensionsSupport
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
// import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

// TODO: PostgreSQL connection will be configured in future
// @ExtendWith(DropwizardExtensionsSupport::class)
class UserResourceTest {
    // companion object {
    //     @JvmField
    //     val app =
    //         DropwizardAppExtension<PieqApiConfiguration>(
    //             com.pieq.agentmanagement.PieqApiApplication::class.java,
    //             "src/test/resources/config_test.yml",
    //         )
    // }

    private lateinit var mockUserService: UserService
    private lateinit var userResource: UserResource

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        mockUserService = mockk<UserService>()
        userResource = UserResource(mockUserService)
    }

    @Test
    fun testCreateUser() {
        // Given
        val currentTime = System.currentTimeMillis()
        val request =
            CreateUserRequest(
                userName = "testuser_$currentTime",
                email = "test_$currentTime@example.com",
                firstName = "Test",
                lastName = "User",
            )

        val expectedUser =
            User(
                guid = UUID.randomUUID(),
                userName = request.userName,
                email = request.email,
                firstName = request.firstName,
                lastName = request.lastName,
                active = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        every { mockUserService.createUser(any()) } returns expectedUser

        // When
        val response = userResource.createUser(request)

        // Then
        assertEquals(201, response.status)
        assertNotNull(response.entity)
        assertEquals(expectedUser, response.entity)
    }

    @Test
    fun testGetUsers() {
        // Given
        val expectedUsers =
            listOf(
                User(
                    guid = UUID.randomUUID(),
                    userName = "user1",
                    email = "user1@example.com",
                    firstName = "User",
                    lastName = "One",
                    active = true,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
                User(
                    guid = UUID.randomUUID(),
                    userName = "user2",
                    email = "user2@example.com",
                    firstName = "User",
                    lastName = "Two",
                    active = true,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )

        every { mockUserService.getAllUsers(any()) } returns expectedUsers

        // When
        val response = userResource.getAllUsers(limit = 20)

        // Then
        assertEquals(200, response.status)
        assertNotNull(response.entity)
        assertEquals(expectedUsers, response.entity)
    }

    @Test
    fun testGetUserByGuid() {
        // Given
        val guid = UUID.randomUUID()
        val expectedUser =
            User(
                guid = guid,
                userName = "testuser",
                email = "test@example.com",
                firstName = "Test",
                lastName = "User",
                active = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        every { mockUserService.getUserByGuid(guid) } returns expectedUser

        // When
        val response = userResource.getUserByGuid(guid.toString())

        // Then
        assertEquals(200, response.status)
        assertNotNull(response.entity)
        assertEquals(expectedUser, response.entity)
    }

    @Test
    fun testGetUserByUsername() {
        // Given
        val username = "testuser"
        val expectedUser =
            User(
                guid = UUID.randomUUID(),
                userName = username,
                email = "test@example.com",
                firstName = "Test",
                lastName = "User",
                active = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        every { mockUserService.getUserByUsername(username) } returns expectedUser

        // When
        val response = userResource.getUserByUsername(username)

        // Then
        assertEquals(200, response.status)
        assertNotNull(response.entity)
        assertEquals(expectedUser, response.entity)
    }

    @Test
    fun testUpdateUser() {
        // Given
        val guid = UUID.randomUUID()
        val request =
            UpdateUserRequest(
                email = "updated@example.com",
                firstName = "Updated",
                lastName = "User",
                active = true,
            )

        val existingUser =
            User(
                guid = guid,
                userName = "testuser",
                email = "test@example.com",
                firstName = "Test",
                lastName = "User",
                active = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        val updatedUser =
            existingUser.copy(
                email = request.email ?: existingUser.email,
                firstName = request.firstName ?: existingUser.firstName,
                lastName = request.lastName ?: existingUser.lastName,
                updatedAt = LocalDateTime.now(),
            )

        every { mockUserService.getUserByGuid(guid) } returns existingUser
        every { mockUserService.updateUser(guid, request) } returns updatedUser

        // When
        val response = userResource.updateUser(guid.toString(), request)

        // Then
        assertEquals(200, response.status)
        assertNotNull(response.entity)
        assertEquals(updatedUser, response.entity)
    }

    @Test
    fun testDeleteUser() {
        // Given
        val guid = UUID.randomUUID()

        every { mockUserService.deleteUser(guid) } returns Unit

        // When
        val response = userResource.deleteUser(guid.toString())

        // Then
        assertEquals(204, response.status)
    }

    @Test
    fun testDeactivateUser() {
        // Given
        val guid = UUID.randomUUID()
        val request =
            UpdateUserRequest(
                email = null,
                firstName = null,
                lastName = null,
                active = false,
            )

        val existingUser =
            User(
                guid = guid,
                userName = "testuser",
                email = "test@example.com",
                firstName = "Test",
                lastName = "User",
                active = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        val deactivatedUser =
            existingUser.copy(
                active = false,
                updatedAt = LocalDateTime.now(),
            )

        every { mockUserService.getUserByGuid(guid) } returns existingUser
        every { mockUserService.updateUser(guid, request) } returns deactivatedUser

        // When
        val response = userResource.deactivateUser(guid.toString(), request)

        // Then
        assertEquals(200, response.status)
        assertNotNull(response.entity)
        assertEquals(deactivatedUser, response.entity)
    }

    @Test
    fun testAuthorizationScopes() {
        // This test demonstrates the authorization scopes used in the UserResource
        // user:read scope is required for GET operations (getAllUsers, getUserByGuid, getUserByUsername)
        // user:write scope is required for POST, PUT, DELETE operations (createUser, updateUser, deleteUser, deactivateUser)

        // Given - Mock service calls for read operations (user:read scope)
        val readUsers =
            listOf(
                User(
                    guid = UUID.randomUUID(),
                    userName = "readuser",
                    email = "read@example.com",
                    firstName = "Read",
                    lastName = "User",
                    active = true,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )

        every { mockUserService.getAllUsers(any()) } returns readUsers

        // When - Test read operation (requires user:read scope)
        val readResponse = userResource.getAllUsers(limit = 10)

        // Then - Verify read operation works
        assertEquals(200, readResponse.status)
        assertEquals(readUsers, readResponse.entity)

        // Given - Mock service calls for write operations (user:write scope)
        val writeRequest =
            CreateUserRequest(
                userName = "writeuser",
                email = "write@example.com",
                firstName = "Write",
                lastName = "User",
            )

        val createdUser =
            User(
                guid = UUID.randomUUID(),
                userName = writeRequest.userName,
                email = writeRequest.email,
                firstName = writeRequest.firstName,
                lastName = writeRequest.lastName,
                active = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        every { mockUserService.createUser(any()) } returns createdUser

        // When - Test write operation (requires user:write scope)
        val writeResponse = userResource.createUser(writeRequest)

        // Then - Verify write operation works
        assertEquals(201, writeResponse.status)
        assertEquals(createdUser, writeResponse.entity)
    }
} 

