package com.devoxx.genie.service.mcp;

import com.devoxx.genie.service.agent.loop.JsonSchemaSupport;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checks tool-call arguments against a tool's declared input schema before the call is sent
 * to the MCP server, so obviously invalid calls fail fast with a precise message instead of
 * a network round trip and a server-specific error.
 *
 * <p>Deliberately lenient: only clear violations are reported — arguments that are not a
 * JSON object, missing required parameters, top-level values of the wrong JSON kind, enum
 * values outside the allowed set, and unknown parameters when the schema forbids them.
 * Numeric strings are accepted for numeric parameters because many servers coerce them.
 */
public final class McpArgumentValidator {

    private McpArgumentValidator() {
    }

    /**
     * @return a list of problems; empty when the arguments are acceptable or when there is no
     *         schema to check against
     */
    public static @NotNull List<String> validate(@Nullable String arguments, @Nullable JsonObjectSchema schema) {
        List<String> problems = new ArrayList<>();
        if (schema == null) {
            return problems;
        }

        JsonObject args;
        try {
            JsonElement parsed = (arguments == null || arguments.isBlank())
                    ? new JsonObject()
                    : JsonParser.parseString(arguments);
            if (!parsed.isJsonObject()) {
                problems.add("arguments must be a JSON object");
                return problems;
            }
            args = parsed.getAsJsonObject();
        } catch (Exception e) {
            problems.add("arguments are not valid JSON");
            return problems;
        }

        Map<String, JsonSchemaElement> properties = schema.properties() != null ? schema.properties() : Map.of();
        List<String> required = schema.required() != null ? schema.required() : List.of();

        for (String name : required) {
            JsonElement value = args.get(name);
            if (value == null || value.isJsonNull()) {
                problems.add("missing required parameter '" + name + "'");
            }
        }

        for (Map.Entry<String, JsonElement> entry : args.entrySet()) {
            String name = entry.getKey();
            JsonElement value = entry.getValue();
            JsonSchemaElement expected = properties.get(name);
            if (expected == null) {
                if (Boolean.FALSE.equals(schema.additionalProperties())) {
                    problems.add("unknown parameter '" + name + "'");
                }
                continue;
            }
            if (value == null || value.isJsonNull()) {
                continue;
            }
            String problem = typeProblem(value, expected);
            if (problem != null) {
                problems.add("parameter '" + name + "' " + problem);
            }
        }
        return problems;
    }

    /** Formats problems as a tool result the model can act on. */
    public static @NotNull String errorMessage(@NotNull String toolName, @NotNull List<String> problems,
                                               @Nullable JsonObjectSchema schema) {
        return "Error: invalid arguments for MCP tool '" + toolName + "': " + String.join("; ", problems)
                + ". Expected parameters " + JsonSchemaSupport.signature(schema)
                + " (parameters marked ? are optional). The call was not sent to the server; fix the arguments and retry.";
    }

    private static @Nullable String typeProblem(@NotNull JsonElement value, @NotNull JsonSchemaElement expected) {
        String type = JsonSchemaSupport.typeName(expected);
        if (expected instanceof JsonObjectSchema) {
            return value.isJsonObject() ? null : "must be an object";
        }
        if (expected instanceof JsonArraySchema) {
            return value.isJsonArray() ? null : "must be an array (" + type + ")";
        }
        if (value.isJsonObject() || value.isJsonArray()) {
            if (expected instanceof JsonStringSchema || expected instanceof JsonIntegerSchema
                    || expected instanceof JsonNumberSchema || expected instanceof JsonBooleanSchema
                    || expected instanceof JsonEnumSchema) {
                return "must be a single " + type + " value, not " + (value.isJsonObject() ? "an object" : "an array");
            }
            return null;
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (expected instanceof JsonBooleanSchema) {
            return primitive.isBoolean() ? null : "must be a boolean (true or false)";
        }
        if (expected instanceof JsonIntegerSchema) {
            BigDecimal number = asNumber(primitive);
            if (number == null) return "must be an integer";
            return isIntegral(number) ? null : "must be an integer, got " + primitive.getAsString();
        }
        if (expected instanceof JsonNumberSchema) {
            return asNumber(primitive) != null ? null : "must be a number";
        }
        if (expected instanceof JsonEnumSchema enumSchema) {
            List<String> allowed = enumSchema.enumValues();
            return allowed == null || allowed.contains(primitive.getAsString())
                    ? null
                    : "must be one of " + allowed + ", got '" + primitive.getAsString() + "'";
        }
        return null;
    }

    private static @Nullable BigDecimal asNumber(@NotNull JsonPrimitive primitive) {
        if (primitive.isBoolean()) return null;
        try {
            return new BigDecimal(primitive.getAsString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isIntegral(@NotNull BigDecimal number) {
        return number.signum() == 0 || number.scale() <= 0 || number.stripTrailingZeros().scale() <= 0;
    }
}
