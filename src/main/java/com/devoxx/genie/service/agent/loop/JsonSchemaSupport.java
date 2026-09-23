package com.devoxx.genie.service.agent.loop;

import dev.langchain4j.model.chat.request.json.JsonAnyOfSchema;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNullSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Small helpers over langchain4j's {@link JsonSchemaElement} tree: a readable type name and a
 * compact one-line signature of a tool's parameters.
 */
public final class JsonSchemaSupport {

    private JsonSchemaSupport() {
    }

    /** Short type name, e.g. {@code string}, {@code integer}, {@code array<string>}, {@code enum[a|b]}. */
    public static @NotNull String typeName(@Nullable JsonSchemaElement element) {
        if (element == null) return "any";
        if (element instanceof JsonStringSchema) return "string";
        if (element instanceof JsonIntegerSchema) return "integer";
        if (element instanceof JsonNumberSchema) return "number";
        if (element instanceof JsonBooleanSchema) return "boolean";
        if (element instanceof JsonNullSchema) return "null";
        if (element instanceof JsonObjectSchema) return "object";
        if (element instanceof JsonArraySchema array) return "array<" + typeName(array.items()) + ">";
        if (element instanceof JsonEnumSchema e) {
            List<String> values = e.enumValues();
            return "enum[" + String.join("|", values) + "]";
        }
        if (element instanceof JsonAnyOfSchema anyOf) {
            return anyOf.anyOf().stream().map(JsonSchemaSupport::typeName).collect(Collectors.joining(" or "));
        }
        return "any";
    }

    /**
     * One-line parameter signature, e.g. {@code (query: string, limit?: integer)}; optional
     * parameters are marked with {@code ?}.
     */
    public static @NotNull String signature(@Nullable JsonObjectSchema parameters) {
        if (parameters == null || parameters.properties() == null || parameters.properties().isEmpty()) {
            return "()";
        }
        List<String> required = parameters.required() != null ? parameters.required() : List.of();
        StringBuilder sb = new StringBuilder("(");
        boolean first = true;
        for (Map.Entry<String, JsonSchemaElement> e : parameters.properties().entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(e.getKey());
            if (!required.contains(e.getKey())) sb.append('?');
            sb.append(": ").append(typeName(e.getValue()));
        }
        return sb.append(')').toString();
    }
}
