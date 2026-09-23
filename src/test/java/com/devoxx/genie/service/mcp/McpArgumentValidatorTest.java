package com.devoxx.genie.service.mcp;

import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class McpArgumentValidatorTest {

    private static final JsonObjectSchema SCHEMA = JsonObjectSchema.builder()
            .addStringProperty("repo", "owner/name")
            .addIntegerProperty("limit", "max results")
            .addNumberProperty("ratio", "a ratio")
            .addBooleanProperty("draft", "draft flag")
            .addProperty("state", JsonEnumSchema.builder().enumValues("open", "closed").build())
            .addProperty("labels", JsonArraySchema.builder().items(new JsonStringSchema()).build())
            .addProperty("filter", JsonObjectSchema.builder().build())
            .required("repo")
            .build();

    @Test
    void acceptsValidArguments() {
        assertThat(McpArgumentValidator.validate(
                "{\"repo\":\"a/b\",\"limit\":5,\"ratio\":0.5,\"draft\":false,\"state\":\"open\",\"labels\":[\"x\"],\"filter\":{}}",
                SCHEMA)).isEmpty();
    }

    @Test
    void acceptsNumericStringsAndIntegralDecimals() {
        assertThat(McpArgumentValidator.validate("{\"repo\":\"a/b\",\"limit\":\"5\",\"ratio\":\"1.5\"}", SCHEMA)).isEmpty();
        assertThat(McpArgumentValidator.validate("{\"repo\":\"a/b\",\"limit\":5.0}", SCHEMA)).isEmpty();
    }

    @Test
    void reportsMissingRequiredParameter() {
        assertThat(McpArgumentValidator.validate("{\"limit\":5}", SCHEMA))
                .containsExactly("missing required parameter 'repo'");
        assertThat(McpArgumentValidator.validate("{\"repo\":null}", SCHEMA))
                .containsExactly("missing required parameter 'repo'");
    }

    @Test
    void reportsClearTypeMismatches() {
        List<String> problems = McpArgumentValidator.validate(
                "{\"repo\":[\"a\"],\"limit\":2.5,\"ratio\":\"abc\",\"draft\":\"yes\",\"state\":\"merged\",\"labels\":\"x\",\"filter\":1}",
                SCHEMA);

        assertThat(problems).hasSize(7);
        assertThat(String.join("\n", problems))
                .contains("'repo' must be a single string value, not an array")
                .contains("'limit' must be an integer, got 2.5")
                .contains("'ratio' must be a number")
                .contains("'draft' must be a boolean")
                .contains("'state' must be one of [open, closed], got 'merged'")
                .contains("'labels' must be an array")
                .contains("'filter' must be an object");
    }

    @Test
    void rejectsNonObjectAndInvalidJson() {
        assertThat(McpArgumentValidator.validate("[1,2]", SCHEMA)).containsExactly("arguments must be a JSON object");
        assertThat(McpArgumentValidator.validate("{oops", SCHEMA)).containsExactly("arguments are not valid JSON");
    }

    @Test
    void unknownParameters_onlyRejectedWhenSchemaForbidsThem() {
        JsonObjectSchema closed = JsonObjectSchema.builder()
                .addStringProperty("a", "a")
                .additionalProperties(false)
                .build();

        assertThat(McpArgumentValidator.validate("{\"repo\":\"x\",\"extra\":1}", SCHEMA)).isEmpty();
        assertThat(McpArgumentValidator.validate("{\"extra\":1}", closed)).containsExactly("unknown parameter 'extra'");
    }

    @Test
    void noSchema_meansNothingToCheck() {
        assertThat(McpArgumentValidator.validate("not even json", null)).isEmpty();
    }

    @Test
    void errorMessage_includesSignature() {
        String message = McpArgumentValidator.errorMessage("create_issue", List.of("missing required parameter 'repo'"),
                JsonObjectSchema.builder().addStringProperty("repo", "r").addIntegerProperty("limit", "l").required("repo").build());

        assertThat(message).startsWith("Error: invalid arguments for MCP tool 'create_issue'")
                .contains("(repo: string, limit?: integer)")
                .contains("not sent to the server");
    }
}
