package com.example.framework.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Iterator;
import java.util.Map;

/**
 * Shared JSON helper so the server uses one Jackson {@link ObjectMapper} instance everywhere.
 * Centralizing JSON handling avoids repeated mapper creation and keeps serialization consistent.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonUtil {
    /** Reused mapper instance for converting request and response bodies. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Exposes the shared mapper when direct Jackson operations are needed. */
    public static ObjectMapper objectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * Deep-merges a JSON patch into an existing Java object and returns a new instance of the target type.
     * Object fields merge recursively, while scalar values and arrays replace the previous value.
     */
    public static <T> T merge(Object baseValue, JsonNode patch, Class<T> targetType) {
        ObjectNode baseNode = OBJECT_MAPPER.valueToTree(baseValue);
        mergeInto(baseNode, patch);
        return OBJECT_MAPPER.convertValue(baseNode, targetType);
    }

    /** Applies merge-patch semantics recursively to the supplied JSON object node. */
    private static void mergeInto(ObjectNode target, JsonNode patch) {
        if (patch == null || patch.isNull() || !patch.isObject()) {
            return;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = patch.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode patchValue = field.getValue();
            JsonNode existingValue = target.get(field.getKey());

            if (patchValue != null && patchValue.isObject() && existingValue != null && existingValue.isObject()) {
                mergeInto((ObjectNode) existingValue, patchValue);
            } else {
                target.set(field.getKey(), patchValue);
            }
        }
    }

    /**
     * Converts any supported object into JSON for HTTP responses.
     * Wrapping the checked Jackson exception keeps call sites cleaner.
     */
    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize JSON", exception);
        }
    }
}
