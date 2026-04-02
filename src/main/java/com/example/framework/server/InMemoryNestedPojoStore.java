package com.example.framework.server;

import com.example.framework.model.CreateOrderRequest;
import com.example.framework.model.CreateProfileRequest;
import com.example.framework.model.OrderAudit;
import com.example.framework.model.OrderItemRequest;
import com.example.framework.model.OrderRecord;
import com.example.framework.model.OrderSummary;
import com.example.framework.model.ProfileRecord;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe store for nested POJO demo flows used by the framework.
 */
public class InMemoryNestedPojoStore {
    private final ConcurrentMap<Long, ProfileRecord> profiles = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, OrderRecord> orders = new ConcurrentHashMap<>();
    private final AtomicLong profileSequence = new AtomicLong(2000);
    private final AtomicLong orderSequence = new AtomicLong(5000);

    public ProfileRecord createProfile(CreateProfileRequest request) {
        long id = profileSequence.incrementAndGet();
        ProfileRecord record = new ProfileRecord(
                id,
                request.getFullName(),
                request.getEmail(),
                request.getAddress(),
                Instant.now().toString()
        );
        profiles.put(id, record);
        return record;
    }

    public Optional<ProfileRecord> getProfile(long id) {
        return Optional.ofNullable(profiles.get(id));
    }

    public Optional<ProfileRecord> updateProfile(long id, CreateProfileRequest request) {
        ProfileRecord updatedRecord = profiles.computeIfPresent(id, (key, existingRecord) -> new ProfileRecord(
                existingRecord.getId(),
                request.getFullName(),
                request.getEmail(),
                request.getAddress(),
                existingRecord.getCreatedAt()
        ));
        return Optional.ofNullable(updatedRecord);
    }

    /** Partially updates an existing profile by deep-merging only the supplied fields. */
    public Optional<ProfileRecord> patchProfile(long id, JsonNode patch) {
        ProfileRecord patchedRecord = profiles.computeIfPresent(id, (key, existingRecord) -> {
            CreateProfileRequest mergedRequest = JsonUtil.merge(
                    new CreateProfileRequest(existingRecord.getFullName(), existingRecord.getEmail(), existingRecord.getAddress()),
                    patch,
                    CreateProfileRequest.class
            );

            return new ProfileRecord(
                    existingRecord.getId(),
                    mergedRequest.getFullName(),
                    mergedRequest.getEmail(),
                    mergedRequest.getAddress(),
                    existingRecord.getCreatedAt()
            );
        });
        return Optional.ofNullable(patchedRecord);
    }

    /** Deletes a profile by id and reports whether anything was removed. */
    public boolean deleteProfile(long id) {
        return profiles.remove(id) != null;
    }

    public OrderRecord createOrder(CreateOrderRequest request) {
        long id = orderSequence.incrementAndGet();
        String currency = request.getPaymentDetails().getCurrency();
        BigDecimal totalAmount = request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalItems = request.getItems().stream()
                .mapToInt(OrderItemRequest::getQuantity)
                .sum();

        OrderRecord record = new OrderRecord(
                id,
                request.getCustomer(),
                new ArrayList<>(request.getItems()),
                request.getShippingAddress(),
                request.getPaymentDetails(),
                request.getTags() == null ? List.of() : new ArrayList<>(request.getTags()),
                new OrderSummary(totalItems, totalAmount, currency),
                new OrderAudit(Instant.now().toString(), "embedded-api", "local-framework")
        );
        orders.put(id, record);
        return record;
    }

    public Optional<OrderRecord> getOrder(long id) {
        return Optional.ofNullable(orders.get(id));
    }

    public Optional<OrderRecord> updateOrder(long id, CreateOrderRequest request) {
        OrderRecord updatedRecord = orders.computeIfPresent(id, (key, existingRecord) -> {
            String currency = request.getPaymentDetails().getCurrency();
            BigDecimal totalAmount = request.getItems().stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int totalItems = request.getItems().stream()
                    .mapToInt(OrderItemRequest::getQuantity)
                    .sum();

            return new OrderRecord(
                    existingRecord.getId(),
                    request.getCustomer(),
                    new ArrayList<>(request.getItems()),
                    request.getShippingAddress(),
                    request.getPaymentDetails(),
                    request.getTags() == null ? List.of() : new ArrayList<>(request.getTags()),
                    new OrderSummary(totalItems, totalAmount, currency),
                    new OrderAudit(existingRecord.getAudit().getCreatedAt(), "embedded-api", "local-framework")
            );
        });
        return Optional.ofNullable(updatedRecord);
    }

    /** Partially updates an existing order by deep-merging only the supplied fields. */
    public Optional<OrderRecord> patchOrder(long id, JsonNode patch) {
        OrderRecord patchedRecord = orders.computeIfPresent(id, (key, existingRecord) -> {
            CreateOrderRequest mergedRequest = JsonUtil.merge(
                    new CreateOrderRequest(
                            existingRecord.getCustomer(),
                            existingRecord.getItems(),
                            existingRecord.getShippingAddress(),
                            existingRecord.getPaymentDetails(),
                            existingRecord.getTags()
                    ),
                    patch,
                    CreateOrderRequest.class
            );

            String currency = mergedRequest.getPaymentDetails().getCurrency();
            BigDecimal totalAmount = mergedRequest.getItems().stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int totalItems = mergedRequest.getItems().stream()
                    .mapToInt(OrderItemRequest::getQuantity)
                    .sum();

            return new OrderRecord(
                    existingRecord.getId(),
                    mergedRequest.getCustomer(),
                    new ArrayList<>(mergedRequest.getItems()),
                    mergedRequest.getShippingAddress(),
                    mergedRequest.getPaymentDetails(),
                    mergedRequest.getTags() == null ? List.of() : new ArrayList<>(mergedRequest.getTags()),
                    new OrderSummary(totalItems, totalAmount, currency),
                    new OrderAudit(existingRecord.getAudit().getCreatedAt(), "embedded-api", "local-framework")
            );
        });
        return Optional.ofNullable(patchedRecord);
    }

    /** Deletes an order by id and reports whether anything was removed. */
    public boolean deleteOrder(long id) {
        return orders.remove(id) != null;
    }
}
