package com.example.framework.tests;

import com.example.framework.base.BaseApiTest;
import com.example.framework.data.UserPayloadFactory;
import com.example.framework.model.CreateUserRequest;
import com.example.framework.model.UserRecord;
import com.example.framework.validation.UserResponseValidator;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * Functional smoke tests that prove the core API flow works end to end.
 * These tests are intentionally small and readable so they can act as examples for future tests.
 */
public class UserApiFlowTest extends BaseApiTest {

    /** Confirms the server health endpoint is available before deeper functional assertions are made. */
    @Test(groups = "smoke")
    public void healthEndpointShouldReturnUpStatus() {
        UserResponseValidator.validateHealth(USER_API_CLIENT.health());
    }

    /** Verifies the basic user lifecycle: create, fetch, list, and delete. */
    @Test(groups = "smoke")
    public void shouldCreateReadListAndDeleteUser() {
        CreateUserRequest createUserRequest = UserPayloadFactory.build(1);
        UserRecord createdUser = UserResponseValidator.validateCreatedUser(
                USER_API_CLIENT.createUser(createUserRequest),
                createUserRequest
        );
        registerUserCleanup(createdUser.getId());

        CreateUserRequest updateUserRequest = UserPayloadFactory.buildUpdated(1);
        UserRecord updatedUser = UserResponseValidator.validateUpdatedUser(
                USER_API_CLIENT.updateUser(createdUser.getId(), updateUserRequest),
                createdUser.getId(),
                updateUserRequest
        );

        UserResponseValidator.validateFetchedUser(
                USER_API_CLIENT.getUser(updatedUser.getId()),
                updatedUser
        );

        Map<String, Object> patchRequest = UserPayloadFactory.buildPatch(1);
        UserRecord patchedUser = UserResponseValidator.validatePatchedUser(
                USER_API_CLIENT.patchUser(updatedUser.getId(), patchRequest),
                new UserRecord(
                        updatedUser.getId(),
                        patchRequest.get("name").toString(),
                        updatedUser.getEmail(),
                        patchRequest.get("status").toString(),
                        updatedUser.getCreatedAt()
                )
        );

        UserResponseValidator.validateFetchedUser(
                USER_API_CLIENT.getUser(patchedUser.getId()),
                patchedUser
        );

        UserResponseValidator.validateUserListContains(
                USER_API_CLIENT.listUsers(),
                patchedUser.getId()
        );

        UserResponseValidator.validateDeleteSuccess(
                USER_API_CLIENT.deleteUser(patchedUser.getId())
        );
    }

    /** Verifies the seed endpoint can bulk-create records for larger test setups. */
    @Test(groups = "smoke")
    public void shouldSeedUsersFromLocalServerEndpoint() {
        List<UserRecord> seededUsers = UserResponseValidator.validateSeededUsers(USER_API_CLIENT.seedUsers(5), 5);
        seededUsers.forEach(user -> registerUserCleanup(user.getId()));
    }
}
