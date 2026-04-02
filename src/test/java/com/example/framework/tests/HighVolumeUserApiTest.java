package com.example.framework.tests;

import com.example.framework.base.BaseApiTest;
import com.example.framework.config.FrameworkConfig;
import com.example.framework.data.UserPayloadFactory;
import com.example.framework.model.CreateUserRequest;
import com.example.framework.model.UserRecord;
import com.example.framework.validation.UserResponseValidator;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Load-oriented test class that demonstrates how the framework can fan out many API calls.
 * It uses a parallel data provider so one test method can be invoked thousands of times concurrently.
 */
public class HighVolumeUserApiTest extends BaseApiTest {

    /**
     * Generates one payload per intended request.
     * Using a data provider keeps the load size configurable from the command line.
     */
    @DataProvider(name = "userPayloads", parallel = true)
    public Object[][] userPayloads() {
        int requestCount = FrameworkConfig.getRequestCount();
        Object[][] data = new Object[requestCount][1];

        for (int index = 0; index < requestCount; index++) {
            data[index][0] = UserPayloadFactory.build(index + 1000);
        }

        return data;
    }

    /** Verifies each high-volume create call succeeds and returns the expected payload values. */
    @Test(dataProvider = "userPayloads", groups = "load")
    public void shouldCreateUsersUnderConcurrentLoad(CreateUserRequest request) {
        UserRecord createdUser = UserResponseValidator.validateCreatedUser(USER_API_CLIENT.createUser(request), request);
        registerUserCleanup(createdUser.getId());
    }
}
