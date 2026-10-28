package com.devoxx.genie.service.agent.codemode;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CodeModeToolDescriptionTest {

    @Test
    void signatureOfRendersRequiredAndOptionalArgumentsAsTypeScript() {
        var readFile = ToolSpecification.builder()
                .name("read_file")
                .description("Read a file from the project.")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("path")
                        .addIntegerProperty("start_line")
                        .required("path")
                        .build())
                .build();

        assertThat(CodeModeToolDescription.signatureOf(readFile))
                .isEqualTo("tools.read_file(args: { path: string; start_line?: number }) - Read a file from the project.");
    }

    @Test
    void signatureOfQuotesToolNamesThatAreNotJavaScriptIdentifiers() {
        var tool = ToolSpecification.builder().name("jira-search").description("Search Jira.").build();

        assertThat(CodeModeToolDescription.signatureOf(tool))
                .isEqualTo("tools[\"jira-search\"](args: {}) - Search Jira.");
    }

    @Test
    void signatureOfKeepsOnlyTheFirstSentenceOfTheDescription() {
        var tool = ToolSpecification.builder()
                .name("search_files")
                .description("Search files for a regex. Returns matching lines.\nUse it before reading files.")
                .build();

        assertThat(CodeModeToolDescription.signatureOf(tool)).endsWith(" - Search files for a regex.");
    }

    @Test
    void typeScriptOfRendersAnArrayOfEnumValuesAsAParenthesisedUnion() {
        var severities = JsonArraySchema.builder()
                .items(JsonEnumSchema.builder().enumValues("HIGH", "LOW").build())
                .build();

        assertThat(CodeModeToolDescription.typeScriptOf(severities, 0)).isEqualTo("(\"HIGH\" | \"LOW\")[]");
    }

    @Test
    void catalogJsonOfListsEveryToolWithItsSummary() {
        var tools = List.of(
                ToolSpecification.builder().name("read_file").description("Read a file. Long details.").build(),
                ToolSpecification.builder().name("list_files").build());

        assertThat(CodeModeToolDescription.catalogJsonOf(tools)).isEqualTo(
                "[{\"name\":\"read_file\",\"description\":\"Read a file.\"},{\"name\":\"list_files\",\"description\":\"\"}]");
    }

    @Test
    void renderStatesTheScriptTimeBudgetAndListsTheCallableTools() {
        var tools = List.of(ToolSpecification.builder().name("list_files").description("List files.").build());

        assertThat(CodeModeToolDescription.render(tools, 30))
                .contains("stopped after 30 seconds")
                .endsWith("Callable tools:\ntools.list_files(args: {}) - List files.");
    }
}
