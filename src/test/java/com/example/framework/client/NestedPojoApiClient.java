package com.example.framework.client;

import com.example.framework.config.EndpointConfig;
import com.example.framework.model.CreateOrderRequest;
import com.example.framework.model.CreateProfileRequest;
import io.restassured.response.Response;

/**
 * API client for the simple and complex nested POJO demo endpoints.
 */
public class NestedPojoApiClient extends BaseApiClient {

    public NestedPojoApiClient(AuthTokenManager authTokenManager) {
        super(authTokenManager);
    }

    public Response createProfile(CreateProfileRequest request) {
        return post("createProfile", EndpointConfig.profiles(), request, true, false);
    }

    public Response getProfile(long profileId) {
        return get("getProfile", EndpointConfig.profileById(), true, true, profileId);
    }

    public Response updateProfile(long profileId, CreateProfileRequest request) {
        return put("updateProfile", EndpointConfig.profileById(), request, true, false, profileId);
    }

    public Response patchProfile(long profileId, Object request) {
        return patch("patchProfile", EndpointConfig.profileById(), request, true, false, profileId);
    }

    public Response deleteProfile(long profileId) {
        return delete("deleteProfile", EndpointConfig.profileById(), true, false, profileId);
    }

    public Response createOrder(CreateOrderRequest request) {
        return post("createOrder", EndpointConfig.orders(), request, true, false);
    }

    public Response getOrder(long orderId) {
        return get("getOrder", EndpointConfig.orderById(), true, true, orderId);
    }

    public Response updateOrder(long orderId, CreateOrderRequest request) {
        return put("updateOrder", EndpointConfig.orderById(), request, true, false, orderId);
    }

    public Response patchOrder(long orderId, Object request) {
        return patch("patchOrder", EndpointConfig.orderById(), request, true, false, orderId);
    }

    public Response deleteOrder(long orderId) {
        return delete("deleteOrder", EndpointConfig.orderById(), true, false, orderId);
    }
}
