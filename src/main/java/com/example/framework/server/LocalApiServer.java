package com.example.framework.server;

import com.example.framework.model.ApiError;
import com.example.framework.model.CreateOrderRequest;
import com.example.framework.model.CreateProfileRequest;
import com.example.framework.model.AuthTokenRequest;
import com.example.framework.model.AuthTokenResponse;
import com.example.framework.model.CreateUserRequest;
import com.example.framework.model.OrderRecord;
import com.example.framework.model.ProfileRecord;
import com.example.framework.model.RefreshTokenRequest;
import com.example.framework.model.UserRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Small embeddable HTTP server that simulates a real API for local and container-based testing.
 * The class keeps the framework self-contained, so users can run API tests without depending on
 * an external application or database.
 */
@Slf4j
public class LocalApiServer {
    /** Canonical JSON media type used across request and response validation. */
    private static final String APPLICATION_JSON = "application/json";
    /** Default port used when no explicit server.port property is supplied. */
    private static final int DEFAULT_PORT = 9876;
    /** Backlog controls how many incoming connections can wait to be accepted under burst load. */
    private static final int DEFAULT_BACKLOG = 1024;
    /** Startup wait avoids race conditions where tests hit the server before it is reachable. */
    private static final int DEFAULT_READY_TIMEOUT_MS = 10_000;

    /** Port on which the HTTP server should listen. */
    private final int port;
    /** Data store backing all CRUD-style user operations. */
    private final UserStore userStore;
    /** Authentication service backing access-token validation and refresh for protected endpoints. */
    private final InMemoryAuthService authService;
    /** Store backing the simple and complex nested POJO demo endpoints. */
    private final InMemoryNestedPojoStore nestedPojoStore;
    /** Socket backlog chosen to better tolerate high-volume test bursts. */
    private final int backlog;
    /** Worker pool used by the built-in server to process requests concurrently. */
    private final ExecutorService executorService;
    /** Underlying JDK HTTP server instance. */
    private HttpServer server;

    /** Creates a server using property-driven concurrency defaults. */
    public LocalApiServer(int port) {
        this(port, resolveBacklog(), buildExecutorService());
    }

    /** Full constructor makes the server configurable for future tuning or extension. */
    public LocalApiServer(int port, int backlog, ExecutorService executorService) {
        this.port = port;
        this.userStore = buildUserStore();
        this.authService = new InMemoryAuthService();
        this.nestedPojoStore = new InMemoryNestedPojoStore();
        this.backlog = backlog;
        this.executorService = executorService;
    }

    /**
     * Starts the local API server and registers all supported endpoints.
     * The method is synchronized so repeated suite setup cannot accidentally double-start the server.
     */
    public synchronized void start() {
        if (server != null) {
            return;
        }

        try {
            server = HttpServer.create(new InetSocketAddress(port), backlog);
            server.createContext("/health", new HealthHandler());
            server.createContext("/auth/token", new TokenHandler(authService));
            server.createContext("/auth/refresh", new RefreshHandler(authService));
            server.createContext("/users", new UsersHandler(userStore, authService));
            server.createContext("/seed", new SeedHandler(userStore, authService));
            server.createContext("/profiles", new ProfilesHandler(nestedPojoStore, authService));
            server.createContext("/orders", new OrdersHandler(nestedPojoStore, authService));
            server.setExecutor(executorService);
            server.start();
            awaitReadiness();
            log.info("Local API server started on port {} with backlog {}", port, backlog);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to start local API server on port " + port, exception);
        }
    }

    /**
     * Stops the server and shuts down worker threads to release local resources cleanly.
     */
    public synchronized void stop() {
        if (server == null) {
            return;
        }

        server.stop(0);
        server = null;
        executorService.shutdownNow();
        userStore.close();
        log.info("Local API server stopped");
    }

    /** Standalone entry point used by the shaded jar and Docker server image. */
    public static void main(String[] args) {
        int port = Integer.parseInt(System.getProperty("server.port", String.valueOf(DEFAULT_PORT)));
        LocalApiServer localApiServer = new LocalApiServer(port);
        localApiServer.start();
        Runtime.getRuntime().addShutdownHook(new Thread(localApiServer::stop));
    }

    /** Handles health checks so Docker and Kubernetes can verify the server is alive. */
    private static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!acceptsJson(exchange)) {
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, new ApiError("Method not allowed"));
                return;
            }

            send(exchange, 200, new ApiHealth("UP"));
        }
    }

    /** Handles collection-level user endpoints such as create and list. */
    private static class UsersHandler implements HttpHandler {
        /** Shared user store used across all requests handled by the local server. */
        private final UserStore userStore;
        /** Auth service validates bearer tokens before protected actions are processed. */
        private final InMemoryAuthService authService;

        private UsersHandler(UserStore userStore, InMemoryAuthService authService) {
            this.userStore = userStore;
            this.authService = authService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!acceptsJson(exchange)) {
                return;
            }

            if (!authorize(exchange, authService)) {
                return;
            }

            String requestMethod = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("POST".equalsIgnoreCase(requestMethod) && "/users".equals(path)) {
                handleCreate(exchange);
                return;
            }

            if ("PUT".equalsIgnoreCase(requestMethod) && path.startsWith("/users/")) {
                handleUpdate(exchange, path);
                return;
            }

            if ("PATCH".equalsIgnoreCase(requestMethod) && path.startsWith("/users/")) {
                handlePatch(exchange, path);
                return;
            }

            if ("GET".equalsIgnoreCase(requestMethod) && "/users".equals(path)) {
                send(exchange, 200, userStore.getAll());
                return;
            }

            if (path.startsWith("/users/")) {
                handleUserById(exchange, requestMethod, path);
                return;
            }

            send(exchange, 404, new ApiError("Endpoint not found"));
        }

        /** Creates a user from the incoming JSON payload and returns the stored record. */
        private void handleCreate(HttpExchange exchange) throws IOException {
            try {
                if (!hasJsonContentType(exchange)) {
                    return;
                }
                CreateUserRequest request = readBody(exchange.getRequestBody(), CreateUserRequest.class);
                validateUserRequest(request);
                UserRecord createdUser = userStore.create(request);
                send(exchange, 201, createdUser);
            } catch (IllegalArgumentException exception) {
                send(exchange, 400, new ApiError(exception.getMessage()));
            }
        }

        /** Updates an existing user with the supplied request body. */
        private void handleUpdate(HttpExchange exchange, String path) throws IOException {
            try {
                if (!hasJsonContentType(exchange)) {
                    return;
                }
                long userId = Long.parseLong(path.substring("/users/".length()));
                CreateUserRequest request = readBody(exchange.getRequestBody(), CreateUserRequest.class);
                validateUserRequest(request);
                Optional<UserRecord> updatedUser = userStore.update(userId, request);
                if (updatedUser.isPresent()) {
                    send(exchange, 200, updatedUser.get());
                } else {
                    send(exchange, 404, new ApiError("User not found"));
                }
            } catch (NumberFormatException exception) {
                send(exchange, 400, new ApiError("Invalid user id"));
            } catch (IllegalArgumentException exception) {
                send(exchange, 400, new ApiError(exception.getMessage()));
            }
        }

        /** Partially updates an existing user using merge-patch semantics. */
        private void handlePatch(HttpExchange exchange, String path) throws IOException {
            try {
                if (!hasJsonContentType(exchange)) {
                    return;
                }
                long userId = Long.parseLong(path.substring("/users/".length()));
                JsonNode patch = readBody(exchange.getRequestBody(), JsonNode.class);

                Optional<UserRecord> existingUser = userStore.get(userId);
                if (existingUser.isEmpty()) {
                    send(exchange, 404, new ApiError("User not found"));
                    return;
                }

                CreateUserRequest mergedRequest = JsonUtil.merge(
                        new CreateUserRequest(
                                existingUser.get().getName(),
                                existingUser.get().getEmail(),
                                existingUser.get().getStatus()
                        ),
                        patch,
                        CreateUserRequest.class
                );
                validateUserRequest(mergedRequest);

                send(exchange, 200, userStore.patch(userId, patch).orElseThrow());
            } catch (NumberFormatException exception) {
                send(exchange, 400, new ApiError("Invalid user id"));
            } catch (IllegalArgumentException exception) {
                send(exchange, 400, new ApiError(exception.getMessage()));
            }
        }

        /** Routes id-based operations so GET and DELETE can share the same path parsing logic. */
        private void handleUserById(HttpExchange exchange, String requestMethod, String path) throws IOException {
            String identifier = path.substring("/users/".length());

            try {
                long userId = Long.parseLong(identifier);
                if ("GET".equalsIgnoreCase(requestMethod)) {
                    Optional<UserRecord> user = userStore.get(userId);
                    if (user.isPresent()) {
                        send(exchange, 200, user.get());
                    } else {
                        send(exchange, 404, new ApiError("User not found"));
                    }
                    return;
                }

                if ("DELETE".equalsIgnoreCase(requestMethod)) {
                    boolean deleted = userStore.delete(userId);
                    if (deleted) {
                        send(exchange, 200, new ApiMessage("Deleted"));
                    } else {
                        send(exchange, 404, new ApiError("User not found"));
                    }
                    return;
                }

                send(exchange, 405, new ApiError("Method not allowed"));
            } catch (NumberFormatException exception) {
                send(exchange, 400, new ApiError("Invalid user id"));
            }
        }
    }

    /** Handles the seed endpoint used to create many users quickly through one call. */
    private static class SeedHandler implements HttpHandler {
        /** Shared store so seeded records become visible to other endpoints immediately. */
        private final UserStore userStore;
        /** Auth service validates bearer tokens before seed operations are processed. */
        private final InMemoryAuthService authService;

        private SeedHandler(UserStore userStore, InMemoryAuthService authService) {
            this.userStore = userStore;
            this.authService = authService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!acceptsJson(exchange)) {
                return;
            }

            if (!authorize(exchange, authService)) {
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, new ApiError("Method not allowed"));
                return;
            }

            try {
                int count = extractSeedCount(exchange.getRequestURI());
                List<UserRecord> seededUsers = userStore.seed(count);
                send(exchange, 201, seededUsers);
            } catch (NumberFormatException exception) {
                send(exchange, 400, new ApiError("Invalid seed count"));
            }
        }

        /** Extracts the optional count query parameter and falls back to a sensible default. */
        private int extractSeedCount(URI uri) {
            String query = uri.getQuery();
            if (query == null || query.isBlank()) {
                return 10;
            }

            for (String token : query.split("&")) {
                String[] keyValue = token.split("=", 2);
                if (keyValue.length == 2 && "count".equalsIgnoreCase(keyValue[0])) {
                    return Integer.parseInt(keyValue[1]);
                }
            }
            return 10;
        }
    }

    /** Handles the simple nested POJO example for profile creation and retrieval. */
    private static class ProfilesHandler implements HttpHandler {
        private final InMemoryNestedPojoStore nestedPojoStore;
        private final InMemoryAuthService authService;

        private ProfilesHandler(InMemoryNestedPojoStore nestedPojoStore, InMemoryAuthService authService) {
            this.nestedPojoStore = nestedPojoStore;
            this.authService = authService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!acceptsJson(exchange)) {
                return;
            }

            if (!authorize(exchange, authService)) {
                return;
            }

            String requestMethod = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("POST".equalsIgnoreCase(requestMethod) && "/profiles".equals(path)) {
                handleCreate(exchange);
                return;
            }

            if ("PUT".equalsIgnoreCase(requestMethod) && path.startsWith("/profiles/")) {
                handleUpdate(exchange, path);
                return;
            }

            if ("PATCH".equalsIgnoreCase(requestMethod) && path.startsWith("/profiles/")) {
                handlePatch(exchange, path);
                return;
            }

            if (path.startsWith("/profiles/")) {
                handleGetById(exchange, requestMethod, path);
                return;
            }

            send(exchange, 404, new ApiError("Endpoint not found"));
        }

        private void handleCreate(HttpExchange exchange) throws IOException {
            try {
                if (!hasJsonContentType(exchange)) {
                    return;
                }
                CreateProfileRequest request = readBody(exchange.getRequestBody(), CreateProfileRequest.class);
                validateProfileRequest(request);
                ProfileRecord createdProfile = nestedPojoStore.createProfile(request);
                send(exchange, 201, createdProfile);
            } catch (IllegalArgumentException exception) {
                send(exchange, 400, new ApiError(exception.getMessage()));
            }
        }

        private void handleUpdate(HttpExchange exchange, String path) throws IOException {
            try {
                if (!hasJsonContentType(exchange)) {
                    return;
                }
                long profileId = Long.parseLong(path.substring("/profiles/".length()));
                CreateProfileRequest request = readBody(exchange.getRequestBody(), CreateProfileRequest.class);
                validateProfileRequest(request);
                Optional<ProfileRecord> updatedProfile = nestedPojoStore.updateProfile(profileId, request);
                if (updatedProfile.isPresent()) {
                    send(exchange, 200, updatedProfile.get());
                } else {
                    send(exchange, 404, new ApiError("Profile not found"));
                }
            } catch (NumberFormatException exception) {
                send(exchange, 400, new ApiError("Invalid profile id"));
            } catch (IllegalArgumentException exception) {
                send(exchange, 400, new ApiError(exception.getMessage()));
            }
        }

        /** Partially updates an existing profile using deep-merge semantics for nested objects like address. */
        private void handlePatch(HttpExchange exchange, String path) throws IOException {
            try {
                if (!hasJsonContentType(exchange)) {
                    return;
                }
                long profileId = Long.parseLong(path.substring("/profiles/".length()));
                JsonNode patch = readBody(exchange.getRequestBody(), JsonNode.class);

                Optional<ProfileRecord> existingProfile = nestedPojoStore.getProfile(profileId);
                if (existingProfile.isEmpty()) {
                    send(exchange, 404, new ApiError("Profile not found"));
                    return;
                }

                CreateProfileRequest mergedRequest = JsonUtil.merge(
                        new CreateProfileRequest(
                                existingProfile.get().getFullName(),
                                existingProfile.get().getEmail(),
                                existingProfile.get().getAddress()
                        ),
                        patch,
                        CreateProfileRequest.class
                );
                validateProfileRequest(mergedRequest);

                send(exchange, 200, nestedPojoStore.patchProfile(profileId, patch).orElseThrow());
            } catch (NumberFormatException exception) {
                send(exchange, 400, new ApiError("Invalid profile id"));
            } catch (IllegalArgumentException exception) {
                send(exchange, 400, new ApiError(exception.getMessage()));
            }
        }

        private void handleGetById(HttpExchange exchange, String requestMethod, String path) throws IOException {
            String identifier = path.substring("/profiles/".length());
            try {
                long profileId = Long.parseLong(identifier);
                if ("GET".equalsIgnoreCase(requestMethod)) {
                    Optional<ProfileRecord> profile = nestedPojoStore.getProfile(profileId);
                    if (profile.isPresent()) {
                        send(exchange, 200, profile.get());
                    } else {
                        send(exchange, 404, new ApiError("Profile not found"));
                    }
                    return;
                }

                if ("DELETE".equalsIgnoreCase(requestMethod)) {
                    boolean deleted = nestedPojoStore.deleteProfile(profileId);
                    if (deleted) {
                        send(exchange, 200, new ApiMessage("Deleted"));
                    } else {
                        send(exchange, 404, new ApiError("Profile not found"));
                    }
                    return;
                }

                send(exchange, 405, new ApiError("Method not allowed"));
            } catch (NumberFormatException exception) {
                send(exchange, 400, new ApiError("Invalid profile id"));
            }
        }
    }

    /** Handles the complex nested POJO example for order creation and retrieval. */
    private static class OrdersHandler implements HttpHandler {
        private final InMemoryNestedPojoStore nestedPojoStore;
        private final InMemoryAuthService authService;

        private OrdersHandler(InMemoryNestedPojoStore nestedPojoStore, InMemoryAuthService authService) {
            this.nestedPojoStore = nestedPojoStore;
            this.authService = authService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!acceptsJson(exchange)) {
                return;
            }

            if (!authorize(exchange, authService)) {
                return;
            }

            String requestMethod = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("POST".equalsIgnoreCase(requestMethod) && "/orders".equals(path)) {
                handleCreate(exchange);
                return;
            }

            if ("PUT".equalsIgnoreCase(requestMethod) && path.startsWith("/orders/")) {
                handleUpdate(exchange, path);
                return;
            }

            if ("PATCH".equalsIgnoreCase(requestMethod) && path.startsWith("/orders/")) {
                handlePatch(exchange, path);
                return;
            }

            if (path.startsWith("/orders/")) {
                handleGetById(exchange, requestMethod, path);
                return;
            }

            send(exchange, 404, new ApiError("Endpoint not found"));
        }

        private void handleCreate(HttpExchange exchange) throws IOException {
            try {
                if (!hasJsonContentType(exchange)) {
                    return;
                }
                CreateOrderRequest request = readBody(exchange.getRequestBody(), CreateOrderRequest.class);
                validateOrderRequest(request);
                OrderRecord createdOrder = nestedPojoStore.createOrder(request);
                send(exchange, 201, createdOrder);
            } catch (IllegalArgumentException exception) {
                send(exchange, 400, new ApiError(exception.getMessage()));
            }
        }

        private void handleUpdate(HttpExchange exchange, String path) throws IOException {
            try {
                if (!hasJsonContentType(exchange)) {
                    return;
                }
                long orderId = Long.parseLong(path.substring("/orders/".length()));
                CreateOrderRequest request = readBody(exchange.getRequestBody(), CreateOrderRequest.class);
                validateOrderRequest(request);
                Optional<OrderRecord> updatedOrder = nestedPojoStore.updateOrder(orderId, request);
                if (updatedOrder.isPresent()) {
                    send(exchange, 200, updatedOrder.get());
                } else {
                    send(exchange, 404, new ApiError("Order not found"));
                }
            } catch (NumberFormatException exception) {
                send(exchange, 400, new ApiError("Invalid order id"));
            } catch (IllegalArgumentException exception) {
                send(exchange, 400, new ApiError(exception.getMessage()));
            }
        }

        /** Partially updates an existing order using merge-patch semantics for nested structures. */
        private void handlePatch(HttpExchange exchange, String path) throws IOException {
            try {
                if (!hasJsonContentType(exchange)) {
                    return;
                }
                long orderId = Long.parseLong(path.substring("/orders/".length()));
                JsonNode patch = readBody(exchange.getRequestBody(), JsonNode.class);

                Optional<OrderRecord> existingOrder = nestedPojoStore.getOrder(orderId);
                if (existingOrder.isEmpty()) {
                    send(exchange, 404, new ApiError("Order not found"));
                    return;
                }

                CreateOrderRequest mergedRequest = JsonUtil.merge(
                        new CreateOrderRequest(
                                existingOrder.get().getCustomer(),
                                existingOrder.get().getItems(),
                                existingOrder.get().getShippingAddress(),
                                existingOrder.get().getPaymentDetails(),
                                existingOrder.get().getTags()
                        ),
                        patch,
                        CreateOrderRequest.class
                );
                validateOrderRequest(mergedRequest);

                send(exchange, 200, nestedPojoStore.patchOrder(orderId, patch).orElseThrow());
            } catch (NumberFormatException exception) {
                send(exchange, 400, new ApiError("Invalid order id"));
            } catch (IllegalArgumentException exception) {
                send(exchange, 400, new ApiError(exception.getMessage()));
            }
        }

        private void handleGetById(HttpExchange exchange, String requestMethod, String path) throws IOException {
            String identifier = path.substring("/orders/".length());
            try {
                long orderId = Long.parseLong(identifier);
                if ("GET".equalsIgnoreCase(requestMethod)) {
                    Optional<OrderRecord> order = nestedPojoStore.getOrder(orderId);
                    if (order.isPresent()) {
                        send(exchange, 200, order.get());
                    } else {
                        send(exchange, 404, new ApiError("Order not found"));
                    }
                    return;
                }

                if ("DELETE".equalsIgnoreCase(requestMethod)) {
                    boolean deleted = nestedPojoStore.deleteOrder(orderId);
                    if (deleted) {
                        send(exchange, 200, new ApiMessage("Deleted"));
                    } else {
                        send(exchange, 404, new ApiError("Order not found"));
                    }
                    return;
                }

                send(exchange, 405, new ApiError("Method not allowed"));
            } catch (NumberFormatException exception) {
                send(exchange, 400, new ApiError("Invalid order id"));
            }
        }
    }

    /** Handles credential-based token creation for the embedded authentication flow. */
    private static class TokenHandler implements HttpHandler {
        private final InMemoryAuthService authService;

        private TokenHandler(InMemoryAuthService authService) {
            this.authService = authService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!acceptsJson(exchange)) {
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, new ApiError("Method not allowed"));
                return;
            }

            try {
                if (!hasJsonContentType(exchange)) {
                    return;
                }
                AuthTokenRequest request = readBody(exchange.getRequestBody(), AuthTokenRequest.class);
                Optional<AuthTokenResponse> response = authService.authenticate(request.getUsername(), request.getPassword());
                if (response.isPresent()) {
                    send(exchange, 200, response.get());
                } else {
                    sendUnauthorized(exchange, "Invalid username or password");
                }
            } catch (IllegalArgumentException exception) {
                send(exchange, 400, new ApiError(exception.getMessage()));
            }
        }
    }

    /** Handles refresh-token exchange for long-running test sessions. */
    private static class RefreshHandler implements HttpHandler {
        private final InMemoryAuthService authService;

        private RefreshHandler(InMemoryAuthService authService) {
            this.authService = authService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!acceptsJson(exchange)) {
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, new ApiError("Method not allowed"));
                return;
            }

            try {
                if (!hasJsonContentType(exchange)) {
                    return;
                }
                RefreshTokenRequest request = readBody(exchange.getRequestBody(), RefreshTokenRequest.class);
                Optional<AuthTokenResponse> response = authService.refresh(request.getRefreshToken());
                if (response.isPresent()) {
                    send(exchange, 200, response.get());
                } else {
                    sendUnauthorized(exchange, "Refresh token is invalid or expired");
                }
            } catch (IllegalArgumentException exception) {
                send(exchange, 400, new ApiError(exception.getMessage()));
            }
        }
    }

    /** Validates that a full user payload contains all fields required by create, put, and patch flows. */
    private static void validateUserRequest(CreateUserRequest request) {
        if (request.getName() == null || request.getEmail() == null || request.getStatus() == null) {
            throw new IllegalArgumentException("name, email, and status are required");
        }
    }

    /** Validates that a full profile payload contains all required top-level and nested fields. */
    private static void validateProfileRequest(CreateProfileRequest request) {
        if (request.getFullName() == null || request.getEmail() == null || request.getAddress() == null) {
            throw new IllegalArgumentException("fullName, email, and address are required");
        }

        if (request.getAddress().getLine1() == null || request.getAddress().getCity() == null) {
            throw new IllegalArgumentException("address.line1 and address.city are required");
        }
    }

    /** Validates that a full order payload still has the required nested blocks after merging updates. */
    private static void validateOrderRequest(CreateOrderRequest request) {
        if (request.getCustomer() == null || request.getItems() == null || request.getItems().isEmpty()
                || request.getShippingAddress() == null || request.getPaymentDetails() == null) {
            throw new IllegalArgumentException("customer, items, shippingAddress, and paymentDetails are required");
        }
    }

    /** Validates that the caller can accept JSON responses, rejecting incompatible Accept headers. */
    private static boolean acceptsJson(HttpExchange exchange) throws IOException {
        String acceptHeader = exchange.getRequestHeaders().getFirst("Accept");
        if (acceptHeader == null || acceptHeader.isBlank()) {
            return true;
        }

        String normalized = acceptHeader.toLowerCase(Locale.ROOT);
        if (normalized.contains(APPLICATION_JSON) || normalized.contains("*/*") || normalized.contains("application/*")) {
            return true;
        }

        send(exchange, 406, new ApiError("Accept header must allow application/json"));
        return false;
    }

    /** Validates that requests carrying a body declare a JSON content type. */
    private static boolean hasJsonContentType(HttpExchange exchange) throws IOException {
        String contentTypeHeader = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentTypeHeader == null || contentTypeHeader.isBlank()) {
            send(exchange, 415, new ApiError("Content-Type must be application/json"));
            return false;
        }

        if (!contentTypeHeader.toLowerCase(Locale.ROOT).startsWith(APPLICATION_JSON)) {
            send(exchange, 415, new ApiError("Content-Type must be application/json"));
            return false;
        }

        return true;
    }

    /** Builds the configured user-store implementation so the server can switch between memory and PostgreSQL. */
    private static UserStore buildUserStore() {
        String userStoreType = ServerStorageConfig.getUserStoreType().toLowerCase(Locale.ROOT);
        return switch (userStoreType) {
            case "memory", "in-memory", "inmemory" -> new InMemoryUserStore();
            case "jdbc", "postgres", "postgresql" -> new JdbcUserStore(
                    ServerStorageConfig.getDbUrl(),
                    ServerStorageConfig.getDbUsername(),
                    ServerStorageConfig.getDbPassword(),
                    ServerStorageConfig.isDbSchemaInitEnabled()
            );
            default -> throw new IllegalArgumentException("Unsupported server.user.store.type: " + userStoreType);
        };
    }

    /** Deserializes the raw HTTP request body into a typed Java object for handler use. */
    private static <T> T readBody(InputStream inputStream, Class<T> type) throws IOException {
        byte[] body = inputStream.readAllBytes();
        if (body.length == 0) {
            throw new IllegalArgumentException("Request body cannot be empty");
        }
        return JsonUtil.objectMapper().readValue(body, type);
    }

    /** Validates the bearer token supplied with a protected request and returns whether it is usable. */
    private static boolean authorize(HttpExchange exchange, InMemoryAuthService authService) throws IOException {
        String authorizationHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            sendUnauthorized(exchange, "Missing bearer token");
            return false;
        }

        String accessToken = authorizationHeader.substring("Bearer ".length()).trim();
        if (accessToken.isEmpty() || !authService.isAccessTokenValid(accessToken)) {
            sendUnauthorized(exchange, "Access token is invalid or expired");
            return false;
        }

        return true;
    }

    /** Sends a 401 response with a WWW-Authenticate header so auth failures are explicit to clients. */
    private static void sendUnauthorized(HttpExchange exchange, String message) throws IOException {
        exchange.getResponseHeaders().set("WWW-Authenticate", "Bearer");
        send(exchange, 401, new ApiError(message));
    }

    /** Serializes a response object to JSON and writes it back with the desired status code. */
    private static void send(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        byte[] response = JsonUtil.toJson(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", APPLICATION_JSON);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    /** Reads backlog tuning from JVM properties so load runs can be adjusted without code changes. */
    private static int resolveBacklog() {
        return Integer.parseInt(System.getProperty("server.backlog", String.valueOf(DEFAULT_BACKLOG)));
    }

    /** Builds the worker pool used by the JDK server for request processing under concurrency. */
    private static ExecutorService buildExecutorService() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int threadCount = Integer.parseInt(System.getProperty(
                "server.worker.threads",
                String.valueOf(Math.max(64, availableProcessors * 8))
        ));
        return Executors.newFixedThreadPool(threadCount);
    }

    /** Waits until the server socket accepts connections before tests begin making API calls. */
    private void awaitReadiness() {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(DEFAULT_READY_TIMEOUT_MS);

        while (System.nanoTime() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 250);
                return;
            } catch (IOException ignored) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for local API server readiness", exception);
                }
            }
        }

        throw new IllegalStateException("Local API server did not become ready within " + DEFAULT_READY_TIMEOUT_MS + " ms");
    }

    /** Tiny success payload used by health checks. */
    private record ApiHealth(String status) {
    }

    /** Tiny success payload used by delete operations. */
    private record ApiMessage(String message) {
    }
}

