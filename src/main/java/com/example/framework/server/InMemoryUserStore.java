package com.example.framework.server;

import com.example.framework.model.CreateUserRequest;
import com.example.framework.model.UserRecord;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe in-memory repository used by the local API server.
 * It exists so the framework can simulate a real API data layer without requiring a database.
 */
public class InMemoryUserStore implements UserStore {
    /** Concurrent map allows multiple requests to create/read/delete users safely in parallel. */
    private final ConcurrentMap<Long, UserRecord> users = new ConcurrentHashMap<>();
    /** Sequence generates unique identifiers without collisions under concurrent traffic. */
    private final AtomicLong sequence = new AtomicLong(1000);

    /**
     * Creates and stores a new user record from the incoming request payload.
     * The method enriches the request with server-owned fields such as id and timestamp.
     */
    public UserRecord create(CreateUserRequest request) {
        long id = sequence.incrementAndGet();
        UserRecord record = new UserRecord(
                id,
                request.getName(),
                request.getEmail(),
                request.getStatus(),
                Instant.now().toString()
        );
        users.put(id, record);
        return record;
    }

    /** Looks up a single user by id so endpoint handlers can return 200 or 404 cleanly. */
    public Optional<UserRecord> get(long id) {
        return Optional.ofNullable(users.get(id));
    }

    /** Updates an existing user if present and preserves its original creation timestamp. */
    public Optional<UserRecord> update(long id, CreateUserRequest request) {
        UserRecord updatedRecord = users.computeIfPresent(id, (key, existingRecord) -> new UserRecord(
                existingRecord.getId(),
                request.getName(),
                request.getEmail(),
                request.getStatus(),
                existingRecord.getCreatedAt()
        ));
        return Optional.ofNullable(updatedRecord);
    }

    /** Partially updates an existing user by merging only the provided fields into the stored record. */
    public Optional<UserRecord> patch(long id, JsonNode patch) {
        UserRecord patchedRecord = users.computeIfPresent(id, (key, existingRecord) -> {
            CreateUserRequest mergedRequest = JsonUtil.merge(
                    new CreateUserRequest(existingRecord.getName(), existingRecord.getEmail(), existingRecord.getStatus()),
                    patch,
                    CreateUserRequest.class
            );

            return new UserRecord(
                    existingRecord.getId(),
                    mergedRequest.getName(),
                    mergedRequest.getEmail(),
                    mergedRequest.getStatus(),
                    existingRecord.getCreatedAt()
            );
        });
        return Optional.ofNullable(patchedRecord);
    }

    /** Returns a snapshot list of all users currently stored in memory. */
    public List<UserRecord> getAll() {
        return new ArrayList<>(users.values());
    }

    /** Deletes a user and returns whether a record actually existed. */
    public boolean delete(long id) {
        return users.remove(id) != null;
    }

    /** Generates seed data so tests can quickly create many records through one endpoint call. */
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
}
