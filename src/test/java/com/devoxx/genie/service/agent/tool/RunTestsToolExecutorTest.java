package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RunTestsToolExecutorTest {

    @Mock
    private Project project;
    @Mock
    private DevoxxGenieStateService stateService;

    private MockedStatic<DevoxxGenieStateService> stateServiceMock;
    private RunTestsToolExecutor executor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        stateServiceMock = mockStatic(DevoxxGenieStateService.class);
        stateServiceMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
        when(stateService.getTestExecutionCustomCommand()).thenReturn("");
        when(stateService.getTestExecutionTimeoutSeconds()).thenReturn(30);
        when(stateService.getTestExecutionEnabled()).thenReturn(true);

        when(project.getBasePath()).thenReturn(tempDir.toString());
        executor = new RunTestsToolExecutor(project);
    }

    @AfterEach
    void tearDown() {
        stateServiceMock.close();
    }

    @Test
    void execute_noBuildSystem_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_tests")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("Could not determine test command");
    }

    @Test
    void execute_withCustomCommand_usesCustomCommand() throws IOException {
        when(stateService.getTestExecutionCustomCommand()).thenReturn("echo custom_test_output");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_tests")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).containsAnyOf("PASSED", "custom_test_output");
    }

    @Test
    void execute_withCustomCommandAndTarget_replacesPlaceholder() {
        // Use a command that writes {target} to stdout — if placeholder is replaced,
        // the output will contain "MyTest" instead of "{target}"
        when(stateService.getTestExecutionCustomCommand()).thenReturn("printf '%s' '{target}'");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_tests")
                .arguments("{\"test_target\": \"MyTest\"}")
                .build();

        String result = executor.execute(request, null);
        // The placeholder should have been replaced with "MyTest"
        // Exit code 0 → PASSED status, raw output contains "MyTest"
        assertThat(result).contains("PASSED");
        assertThat(result).doesNotContain("{target}");
    }

    @Test
    void execute_gradleProject_detectsBuildSystem() throws IOException {
        // Create a build.gradle to trigger Gradle detection
        new File(tempDir.toFile(), "build.gradle").createNewFile();

        // Use a custom command since actual gradlew won't exist
        when(stateService.getTestExecutionCustomCommand()).thenReturn("echo '10 tests completed, 0 failed'");

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_tests")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("PASSED");
    }

    @Test
    void execute_returnsStructuredOutput() {
        when(stateService.getTestExecutionCustomCommand())
                .thenReturn("echo '10 tests completed, 2 failed'");

        // Create build.gradle for Gradle detection
        try {
            new File(tempDir.toFile(), "build.gradle").createNewFile();
        } catch (IOException ignored) {
        }

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("run_tests")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Test Result:");
    }
}
