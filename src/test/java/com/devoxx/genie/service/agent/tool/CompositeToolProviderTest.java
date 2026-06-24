package com.devoxx.genie.service.agent.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CompositeToolProviderTest {

    @Mock
    private ToolProviderRequest request;

    @Test
    void provideTools_mergesFromTwoProviders() {
        ToolSpecification spec1 = ToolSpecification.builder()
                .name("tool_a")
                .description("Tool A")
                .parameters(JsonObjectSchema.builder().build())
                .build();
        ToolSpecification spec2 = ToolSpecification.builder()
                .name("tool_b")
                .description("Tool B")
                .parameters(JsonObjectSchema.builder().build())
                .build();

        ToolExecutor exec1 = (req, id) -> "result_a";
        ToolExecutor exec2 = (req, id) -> "result_b";

        ToolProvider provider1 = req -> ToolProviderResult.builder().add(spec1, exec1).build();
        ToolProvider provider2 = req -> ToolProviderResult.builder().add(spec2, exec2).build();

        CompositeToolProvider composite = new CompositeToolProvider(List.of(provider1, provider2));
        ToolProviderResult result = composite.provideTools(request);

        assertThat(result.tools()).hasSize(2);
    }

    @Test
    void provideTools_duplicateToolName_doesNotThrow_andLaterProviderWins() throws Exception {
        // Both providers define a tool named "read_file": the built-in one and an MCP one.
        // Without deduplication, langchain4j's ToolProviderResult.Builder throws
        // IllegalConfigurationException("Duplicated definition for tool: read_file").
        ToolSpecification builtInReadFile = ToolSpecification.builder()
                .name("read_file")
                .description("Built-in read_file")
                .parameters(JsonObjectSchema.builder().build())
                .build();
        ToolSpecification mcpReadFile = ToolSpecification.builder()
                .name("read_file")
                .description("JetBrains MCP read_file")
                .parameters(JsonObjectSchema.builder().build())
                .build();

        ToolExecutor builtInExec = (req, id) -> "built-in";
        ToolExecutor mcpExec = (req, id) -> "mcp";

        // Provider order mirrors AgentToolProviderFactory: built-in first, MCP second.
        ToolProvider builtInProvider = req -> ToolProviderResult.builder().add(builtInReadFile, builtInExec).build();
        ToolProvider mcpProvider = req -> ToolProviderResult.builder().add(mcpReadFile, mcpExec).build();

        CompositeToolProvider composite = new CompositeToolProvider(List.of(builtInProvider, mcpProvider));
        ToolProviderResult result = composite.provideTools(request);

        // Only one read_file survives, and the MCP one (later provider) wins.
        assertThat(result.aiServiceTools()).hasSize(1);
        AiServiceTool surviving = result.aiServiceTools().get(0);
        assertThat(surviving.name()).isEqualTo("read_file");
        assertThat(surviving.toolSpecification().description()).isEqualTo("JetBrains MCP read_file");
        assertThat(surviving.toolExecutor().execute(null, null)).isEqualTo("mcp");
    }

    @Test
    void provideTools_emptyProviderList_returnsEmpty() {
        CompositeToolProvider composite = new CompositeToolProvider(Collections.emptyList());
        ToolProviderResult result = composite.provideTools(request);

        assertThat(result.tools()).isEmpty();
    }

    @Test
    void provideTools_providerWithNoTools_returnsEmpty() {
        ToolProvider emptyProvider = req -> ToolProviderResult.builder().build();

        CompositeToolProvider composite = new CompositeToolProvider(List.of(emptyProvider));
        ToolProviderResult result = composite.provideTools(request);

        assertThat(result.tools()).isEmpty();
    }
}
