package com.example.framework.server;

import com.example.framework.model.CreateUserRequest;
import com.example.framework.model.UserRecord;
import com.fasterxml.jackson.databind.JsonNode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL-backed user store that persists data across server restarts.
 * The implementation intentionally uses plain JDBC so the framework stays lightweight and easy to learn.
 */
public class JdbcUserStore implements UserStore {
    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS users (
                id BIGSERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL,
                status VARCHAR(64) NOT NULL,
                created_at VARCHAR(64) NOT NULL
            )
            """;

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public JdbcUserStore(String jdbcUrl, String username, String password, boolean initializeSchema) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;

        if (initializeSchema) {
            initializeSchema();
        }
    }

    @Override
    public UserRecord create(CreateUserRequest request) {
        String createdAt = Instant.now().toString();
        String sql = "INSERT INTO users (name, email, status, created_at) VALUES (?, ?, ?, ?) RETURNING id";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, request.getName());
            statement.setString(2, request.getEmail());
            statement.setString(3, request.getStatus());
            statement.setString(4, createdAt);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("User insert did not return a generated id");
                }
                return new UserRecord(resultSet.getLong("id"), request.getName(), request.getEmail(), request.getStatus(), createdAt);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to create user in PostgreSQL", exception);
        }
    }

    @Override
    public Optional<UserRecord> get(long id) {
        String sql = "SELECT id, name, email, status, created_at FROM users WHERE id = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapUser(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to fetch user from PostgreSQL", exception);
        }
    }

    @Override
    public Optional<UserRecord> update(long id, CreateUserRequest request) {
        Optional<UserRecord> existingUser = get(id);
        if (existingUser.isEmpty()) {
            return Optional.empty();
        }

        String sql = "UPDATE users SET name = ?, email = ?, status = ? WHERE id = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, request.getName());
            statement.setString(2, request.getEmail());
            statement.setString(3, request.getStatus());
            statement.setLong(4, id);
            statement.executeUpdate();

            UserRecord storedUser = existingUser.get();
            return Optional.of(new UserRecord(storedUser.getId(), request.getName(), request.getEmail(), request.getStatus(), storedUser.getCreatedAt()));
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update user in PostgreSQL", exception);
        }
    }

    @Override
    public Optional<UserRecord> patch(long id, JsonNode patch) {
        Optional<UserRecord> existingUser = get(id);
        if (existingUser.isEmpty()) {
            return Optional.empty();
        }

        UserRecord storedUser = existingUser.get();
        CreateUserRequest mergedRequest = JsonUtil.merge(
                new CreateUserRequest(storedUser.getName(), storedUser.getEmail(), storedUser.getStatus()),
                patch,
                CreateUserRequest.class
        );

        return update(id, mergedRequest);
    }

    @Override
    public List<UserRecord> getAll() {
        String sql = "SELECT id, name, email, status, created_at FROM users ORDER BY id";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<UserRecord> users = new ArrayList<>();
            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
            return users;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to list users from PostgreSQL", exception);
        }
    }

    @Override
    public boolean delete(long id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to delete user from PostgreSQL", exception);
        }
    }

    @Override
    public List<UserRecord> seed(int count) {
        List<UserRecord> seededUsers = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            seededUsers.add(create(new CreateUserRequest(
                    "Seed User " + index,
                    "seed-user-" + index + "@example.com",
                    "SEEDED"
            )));
        }
        return seededUsers;
    }

    /** Creates the database table required for persisted user storage if it does not already exist. */
    private void initializeSchema() {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE_SQL);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to initialize PostgreSQL user schema", exception);
        }
    }

    /** Opens a fresh JDBC connection for the current database operation. */
    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /** Maps the current JDBC result-set row into a user record model. */
    private UserRecord mapUser(ResultSet resultSet) throws SQLException {
        return new UserRecord(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                resultSet.getString("status"),
                resultSet.getString("created_at")
        );
    }
}
