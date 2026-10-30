package com.devoxx.genie.service.agent.codemode;

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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class CodeModeToolDescription {

    private static final int MAX_SUMMARY_LENGTH = 160;

    private static final int MAX_OBJECT_NESTING = 3;

    private static final Pattern JAVASCRIPT_IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s");

    private static final String INTRODUCTION = """
            Run JavaScript that calls several tools in one step and returns only what you print. \
            Prefer it over separate tool calls when a task needs many calls whose raw results you do not need \
            to read: reading many files, checking every reference of a symbol, filtering large search results.
            - The code runs as the body of an async function in a sandboxed QuickJS runtime: \
            no Node.js, no require or import, no timers, no file system, no network.
            - Call a tool with `await tools.<name>({ ...arguments })`, passing the same arguments the tool \
            takes when called directly.
            - Tool calls run one at a time, so Promise.all does not make them faster.
            - A tool returns a string, or a parsed object when its result is JSON. A failed call returns a \
            string starting with "Error:" instead of throwing.
            - Every call still goes through the user's approval rules and counts toward the tool call limit.
            - Only values passed to `text(value)` or `console.log(...)`, or returned from the script, are shown to you. \
            Print the filtered result, not raw tool output.
            - `ALL_TOOLS` lists every callable tool as { name, description }.
            - A script is stopped after %d seconds of JavaScript run time; time spent inside tool calls \
            does not count.

            Callable tools:
            """;

    private CodeModeToolDescription() {
    }

    static @NotNull String render(@NotNull List<ToolSpecification> tools, long scriptTimeBudgetSeconds) {
        var signatures = tools.stream()
                .map(CodeModeToolDescription::signatureOf)
                .collect(Collectors.joining("\n"));
        return INTRODUCTION.formatted(scriptTimeBudgetSeconds) + signatures;
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
