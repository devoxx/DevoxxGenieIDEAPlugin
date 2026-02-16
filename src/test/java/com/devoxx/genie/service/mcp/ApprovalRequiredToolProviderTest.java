package com.devoxx.genie.service.mcp;

import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApprovalRequiredToolProviderTest {

    @Mock
    private ToolProvider delegate;

    @Mock
    private Project project;

    @Mock
    private ToolProviderRequest request;

    @Mock
    private ToolExecutor originalExecutor1;

    @Mock
    private ToolExecutor originalExecutor2;

    private MockedStatic<MCPService> mockedMCPService;

    @BeforeEach
    void setUp() {
        mockedMCPService = Mockito.mockStatic(MCPService.class);
        mockedMCPService.when(() -> MCPService.logDebug(any(String.class))).thenAnswer(inv -> null);
    }

    @AfterEach
    void tearDown() {
        mockedMCPService.close();
    }

    @Test
    void provideTools_wrapsAllToolsFromDelegate() {
        ToolProviderResult delegateResult = buildResult(
                Map.of(toolSpec("tool_a"), originalExecutor1, toolSpec("tool_b"), originalExecutor2)
        );
        when(delegate.provideTools(request)).thenReturn(delegateResult);

        // Always approve
        ApprovalRequiredToolProvider provider = new ApprovalRequiredToolProvider(
                delegate, project, (p, name, args) -> true
        );

        ToolProviderResult result = provider.provideTools(request);

        // Same number of tools, same specs
        assertThat(result.tools()).hasSize(2);
        assertThat(toolNames(result)).containsExactlyInAnyOrder("tool_a", "tool_b");

        // But executors should be different (wrapped)
        for (ToolExecutor executor : result.tools().values()) {
            assertThat(executor).isNotSameAs(originalExecutor1);
            assertThat(executor).isNotSameAs(originalExecutor2);
        }
    }

    @Test
    void provideTools_approvalGranted_callsOriginalExecutor() {
        ToolSpecification spec = toolSpec("read_file");
        ToolProviderResult delegateResult = buildResult(Map.of(spec, originalExecutor1));
        when(delegate.provideTools(request)).thenReturn(delegateResult);
        when(originalExecutor1.execute(any(), any())).thenReturn("file contents");

        ApprovalRequiredToolProvider provider = new ApprovalRequiredToolProvider(
                delegate, project, (p, name, args) -> true
        );

        ToolProviderResult result = provider.provideTools(request);
        ToolExecutor wrappedExecutor = result.tools().values().iterator().next();

        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
                .name("read_file")
                .arguments("{\"path\": \"/tmp/test.txt\"}")
                .build();

        String executionResult = wrappedExecutor.execute(toolRequest, "memory-1");

        assertThat(executionResult).isEqualTo("file contents");
        verify(originalExecutor1).execute(toolRequest, "memory-1");
    }

    @Test
    void provideTools_approvalDenied_returnsDeniedMessage() {
        ToolSpecification spec = toolSpec("delete_file");
        ToolProviderResult delegateResult = buildResult(Map.of(spec, originalExecutor1));
        when(delegate.provideTools(request)).thenReturn(delegateResult);

        ApprovalRequiredToolProvider provider = new ApprovalRequiredToolProvider(
                delegate, project, (p, name, args) -> false
        );

        ToolProviderResult result = provider.provideTools(request);
        ToolExecutor wrappedExecutor = result.tools().values().iterator().next();

        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
                .name("delete_file")
                .arguments("{\"path\": \"/tmp/test.txt\"}")
                .build();

        String executionResult = wrappedExecutor.execute(toolRequest, "memory-1");

        assertThat(executionResult).isEqualTo("Tool execution was denied by the user.");
        verify(originalExecutor1, never()).execute(any(), any());
    }

    @Test
    void provideTools_approvalCheckerReceivesCorrectArguments() {
        ToolSpecification spec = toolSpec("write_file");
        ToolProviderResult delegateResult = buildResult(Map.of(spec, originalExecutor1));
        when(delegate.provideTools(request)).thenReturn(delegateResult);
        when(originalExecutor1.execute(any(), any())).thenReturn("ok");

        ApprovalRequiredToolProvider.ApprovalChecker checker = mock(ApprovalRequiredToolProvider.ApprovalChecker.class);
        when(checker.requestApproval(any(), any(), any())).thenReturn(true);

        ApprovalRequiredToolProvider provider = new ApprovalRequiredToolProvider(delegate, project, checker);

        ToolProviderResult result = provider.provideTools(request);
        ToolExecutor wrappedExecutor = result.tools().values().iterator().next();

        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
                .name("write_file")
                .arguments("{\"content\": \"hello\"}")
                .build();

        wrappedExecutor.execute(toolRequest, "memory-1");

        verify(checker).requestApproval(project, "write_file", "{\"content\": \"hello\"}");
    }

    @Test
    void provideTools_multipleTools_eachWrappedIndependently() {
        ToolSpecification specA = toolSpec("tool_a");
        ToolSpecification specB = toolSpec("tool_b");
        ToolProviderResult delegateResult = buildResult(Map.of(specA, originalExecutor1, specB, originalExecutor2));
        when(delegate.provideTools(request)).thenReturn(delegateResult);
        when(originalExecutor1.execute(any(), any())).thenReturn("result_a");
        when(originalExecutor2.execute(any(), any())).thenReturn("result_b");

        // Approve tool_a, deny tool_b
        ApprovalRequiredToolProvider provider = new ApprovalRequiredToolProvider(
                delegate, project, (p, name, args) -> "tool_a".equals(name)
        );

        ToolProviderResult result = provider.provideTools(request);

        // Execute tool_a
        ToolExecutor executorA = findExecutor(result, "tool_a");
        ToolExecutionRequest requestA = ToolExecutionRequest.builder()
                .name("tool_a").arguments("{}").build();
        assertThat(executorA.execute(requestA, "m")).isEqualTo("result_a");

        // Execute tool_b
        ToolExecutor executorB = findExecutor(result, "tool_b");
        ToolExecutionRequest requestB = ToolExecutionRequest.builder()
                .name("tool_b").arguments("{}").build();
        assertThat(executorB.execute(requestB, "m")).isEqualTo("Tool execution was denied by the user.");
    }

    @Test
    void provideTools_preservesToolSpecifications() {
        ToolSpecification spec = ToolSpecification.builder()
                .name("complex_tool")
                .description("A complex tool")
                .build();
        ToolProviderResult delegateResult = buildResult(Map.of(spec, originalExecutor1));
        when(delegate.provideTools(request)).thenReturn(delegateResult);

        ApprovalRequiredToolProvider provider = new ApprovalRequiredToolProvider(
                delegate, project, (p, name, args) -> true
        );

        ToolProviderResult result = provider.provideTools(request);

        ToolSpecification resultSpec = result.tools().keySet().iterator().next();
        assertThat(resultSpec.name()).isEqualTo("complex_tool");
        assertThat(resultSpec.description()).isEqualTo("A complex tool");
    }

    @Test
    void provideTools_emptyDelegateResult_returnsEmptyResult() {
        ToolProviderResult delegateResult = buildResult(Map.of());
        when(delegate.provideTools(request)).thenReturn(delegateResult);

        ApprovalRequiredToolProvider provider = new ApprovalRequiredToolProvider(
                delegate, project, (p, name, args) -> true
        );

        ToolProviderResult result = provider.provideTools(request);

        assertThat(result.tools()).isEmpty();
    }

    @Test
    void provideTools_nullProject_passedToChecker() {
        ToolSpecification spec = toolSpec("tool_a");
        ToolProviderResult delegateResult = buildResult(Map.of(spec, originalExecutor1));
        when(delegate.provideTools(request)).thenReturn(delegateResult);
        when(originalExecutor1.execute(any(), any())).thenReturn("ok");

        ApprovalRequiredToolProvider.ApprovalChecker checker = mock(ApprovalRequiredToolProvider.ApprovalChecker.class);
        when(checker.requestApproval(any(), any(), any())).thenReturn(true);

        ApprovalRequiredToolProvider provider = new ApprovalRequiredToolProvider(delegate, null, checker);

        ToolProviderResult result = provider.provideTools(request);
        ToolExecutor wrappedExecutor = result.tools().values().iterator().next();

        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
                .name("tool_a").arguments("{}").build();
        wrappedExecutor.execute(toolRequest, "m");

        verify(checker).requestApproval(null, "tool_a", "{}");
    }

    // -- Helpers --

    private static ToolSpecification toolSpec(String name) {
        return ToolSpecification.builder().name(name).build();
    }

    private static Set<String> toolNames(ToolProviderResult result) {
        Set<String> names = new HashSet<>();
        for (ToolSpecification spec : result.tools().keySet()) {
            names.add(spec.name());
        }
        return names;
    }

    private static ToolProviderResult buildResult(Map<ToolSpecification, ToolExecutor> tools) {
        ToolProviderResult.Builder builder = ToolProviderResult.builder();
        for (Map.Entry<ToolSpecification, ToolExecutor> entry : tools.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    private static ToolExecutor findExecutor(ToolProviderResult result, String toolName) {
        for (Map.Entry<ToolSpecification, ToolExecutor> entry : result.tools().entrySet()) {
            if (entry.getKey().name().equals(toolName)) {
                return entry.getValue();
            }
        }
        throw new IllegalArgumentException("Tool not found: " + toolName);
    }
}
