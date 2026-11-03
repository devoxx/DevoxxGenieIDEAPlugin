package com.devoxx.genie.service.agent.codemode;

import com.devoxx.genie.service.agent.tool.BuiltInToolDescriptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.langchain4j.agent.tool.ToolSpecification;
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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class CodeModeToolDescription {

    private static final int MAX_SUMMARY_LENGTH = 160;

    private static final int MAX_OBJECT_NESTING = 3;

    private static final Pattern JAVASCRIPT_IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s");

    private static final String TOOL_LIST_HEADING = "\n\nCallable tools:\n";

    private CodeModeToolDescription() {
    }

    static @NotNull String render(@NotNull List<ToolSpecification> tools, long scriptTimeBudgetSeconds,
                                int maxScriptToolCalls) {
        var signatures = tools.stream()
                .map(CodeModeToolDescription::signatureOf)
                .collect(Collectors.joining("\n"));
        return introductionOf(scriptTimeBudgetSeconds, maxScriptToolCalls) + TOOL_LIST_HEADING + signatures;
    }

    /**
     * The editable part of the description: the user's override from Settings, or the shipped text,
     * with the limits filled in. Plain replacement rather than String.format, so a stray % in an
     * override can never break tool registration.
     */
    private static @NotNull String introductionOf(long scriptTimeBudgetSeconds, int maxScriptToolCalls) {
        var template = Objects.requireNonNull(BuiltInToolDescriptions.effective(CodeModeToolProvider.RUN_SCRIPT_TOOL_NAME));
        return template
                .replace(BuiltInToolDescriptions.MAX_SCRIPT_TOOL_CALLS_PLACEHOLDER, String.valueOf(maxScriptToolCalls))
                .replace(BuiltInToolDescriptions.SCRIPT_TIME_BUDGET_PLACEHOLDER, String.valueOf(scriptTimeBudgetSeconds))
                .strip();
    }

    static @NotNull String catalogJsonOf(@NotNull List<ToolSpecification> tools) {
        var catalog = new JsonArray();
        for (var tool : tools) {
            var entry = new JsonObject();
            entry.addProperty("name", tool.name());
            entry.addProperty("description", summaryOf(tool.description()));
            catalog.add(entry);
        }
        return catalog.toString();
    }

    static @NotNull String signatureOf(@NotNull ToolSpecification tool) {
        var signature = accessorOf(tool.name()) + "(args: " + argumentsTypeOf(tool.parameters()) + ")";
        var summary = summaryOf(tool.description());
        return summary.isEmpty() ? signature : signature + " - " + summary;
    }

    static @NotNull String typeScriptOf(@Nullable JsonSchemaElement element, int depth) {
        if (element instanceof JsonStringSchema) {
            return "string";
        }
        if (element instanceof JsonIntegerSchema || element instanceof JsonNumberSchema) {
            return "number";
        }
        if (element instanceof JsonBooleanSchema) {
            return "boolean";
        }
        if (element instanceof JsonNullSchema) {
            return "null";
        }
        if (element instanceof JsonEnumSchema enumeration) {
            return enumTypeOf(enumeration);
        }
        if (element instanceof JsonArraySchema array) {
            return arrayTypeOf(array, depth);
        }
        if (element instanceof JsonAnyOfSchema anyOf) {
            return unionTypeOf(anyOf, depth);
        }
        if (element instanceof JsonObjectSchema object) {
            return objectTypeOf(object, depth);
        }
        return "unknown";
    }

    private static String accessorOf(String toolName) {
        return isJavaScriptIdentifier(toolName) ? "tools." + toolName : "tools[" + quoted(toolName) + "]";
    }

    private static String argumentsTypeOf(@Nullable JsonObjectSchema parameters) {
        return parameters == null ? "{}" : objectTypeOf(parameters, 0);
    }

    private static String enumTypeOf(JsonEnumSchema enumeration) {
        var values = enumeration.enumValues();
        if (values == null || values.isEmpty()) {
            return "string";
        }
        return values.stream().map(CodeModeToolDescription::quoted).collect(Collectors.joining(" | "));
    }

    private static String arrayTypeOf(JsonArraySchema array, int depth) {
        var itemType = typeScriptOf(array.items(), depth);
        return itemType.contains(" | ") ? "(" + itemType + ")[]" : itemType + "[]";
    }

    private static String unionTypeOf(JsonAnyOfSchema anyOf, int depth) {
        var options = anyOf.anyOf();
        if (options == null || options.isEmpty()) {
            return "unknown";
        }
        return options.stream()
                .map(option -> typeScriptOf(option, depth))
                .distinct()
                .collect(Collectors.joining(" | "));
    }

    private static String objectTypeOf(JsonObjectSchema object, int depth) {
        if (depth >= MAX_OBJECT_NESTING) {
            return "object";
        }
        var properties = object.properties();
        if (properties == null || properties.isEmpty()) {
            return "{}";
        }
        var required = object.required() == null ? new HashSet<String>() : new HashSet<>(object.required());
        var members = properties.entrySet().stream()
                .map(property -> propertyNameOf(property.getKey())
                        + (required.contains(property.getKey()) ? ": " : "?: ")
                        + typeScriptOf(property.getValue(), depth + 1))
                .collect(Collectors.joining("; "));
        return "{ " + members + " }";
    }

    private static String propertyNameOf(String name) {
        return isJavaScriptIdentifier(name) ? name : quoted(name);
    }

    private static boolean isJavaScriptIdentifier(String name) {
        return JAVASCRIPT_IDENTIFIER.matcher(name).matches();
    }

    private static String quoted(String value) {
        return new JsonPrimitive(value).toString();
    }

    private static String summaryOf(@Nullable String description) {
        if (description == null || description.isBlank()) {
            return "";
        }
        var firstLine = description.strip().lines().findFirst().orElse("");
        var firstSentence = SENTENCE_END.split(firstLine, 2)[0].strip();
        return firstSentence.length() <= MAX_SUMMARY_LENGTH
                ? firstSentence
                : firstSentence.substring(0, MAX_SUMMARY_LENGTH - 3) + "...";
    }
}
