package com.devoxx.genie.service.cli;

import com.devoxx.genie.model.spec.CliToolConfig;
import com.devoxx.genie.service.cli.command.CliCommand;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CliTaskExecutorServiceTest {

    private static final String TEST_TASK_ID = "test-task-id";

    @Mock
    private Project project;

    @Mock
    private Application application;

    private CliTaskExecutorService service;
    private MockedStatic<ApplicationManager> mockedAppManager;

    @BeforeEach
    void setUp() {
        when(project.getBasePath()).thenReturn("/tmp/test-project");

        mockedAppManager = Mockito.mockStatic(ApplicationManager.class);
        mockedAppManager.when(ApplicationManager::getApplication).thenReturn(application);
        doNothing().when(application).invokeLater(any(Runnable.class));

        service = new CliTaskExecutorService(project);
    }

    @AfterEach
    void tearDown() {
        mockedAppManager.close();
    }

    @Test
    void isRunning_noProcess_returnsFalse() {
        assertThat(service.isRunning()).isFalse();
    }

    @Test
    void isRunning_withDeadProcess_returnsFalse() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.isAlive()).thenReturn(false);
        setActiveProcess(mockProcess);

        assertThat(service.isRunning()).isFalse();
    }

    @Test
    void isRunning_withAliveProcess_returnsTrue() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.isAlive()).thenReturn(true);
        setActiveProcess(mockProcess);

        assertThat(service.isRunning()).isTrue();
    }

    @Test
    void cancelCurrentProcess_noProcess_doesNotThrow() {
        service.cancelCurrentProcess();
    }

    @Test
    void cancelCurrentProcess_withDeadProcess_doesNotDestroy() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.isAlive()).thenReturn(false);
        setActiveProcess(mockProcess);

        service.cancelCurrentProcess();
        verify(mockProcess, never()).destroyForcibly();
    }

    @Test
    void cancelCurrentProcess_withAliveProcess_destroysProcess() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.isAlive()).thenReturn(true);
        when(mockProcess.pid()).thenReturn(12345L);
        setActiveProcess(mockProcess);

        service.cancelCurrentProcess();
        verify(mockProcess).destroyForcibly();
    }

    @Test
    void notifyTaskDone_noProcessOrCommand_doesNotThrow() {
        service.notifyTaskDone();
    }

    @Test
    void notifyTaskDone_processNotAlive_doesNotCallOnTaskCompleted() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.isAlive()).thenReturn(false);
        CliCommand mockCommand = mock(CliCommand.class);
        setActiveTask(mockProcess, mockCommand);

        service.notifyTaskDone();
        verify(mockCommand, never()).onTaskCompleted(any());
    }

    @Test
    void notifyTaskDone_processAlive_callsOnTaskCompleted() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.isAlive()).thenReturn(true);
        CliCommand mockCommand = mock(CliCommand.class);
        when(mockCommand.onTaskCompleted(mockProcess)).thenReturn(true);
        setActiveTask(mockProcess, mockCommand);

        service.notifyTaskDone();
        verify(mockCommand).onTaskCompleted(mockProcess);
    }

    @Test
    void notifyTaskDone_commandReturnsFalse_doesNotSetTaskCompletedKill() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.isAlive()).thenReturn(true);
        CliCommand mockCommand = mock(CliCommand.class);
        when(mockCommand.onTaskCompleted(mockProcess)).thenReturn(false);
        setActiveTask(mockProcess, mockCommand);

        service.notifyTaskDone();
        verify(mockCommand).onTaskCompleted(mockProcess);
        assertThat(getTaskCompletedKill()).isFalse();
    }

    @Test
    void notifyTaskDone_commandReturnsTrue_setsTaskCompletedKill() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.isAlive()).thenReturn(true);
        CliCommand mockCommand = mock(CliCommand.class);
        when(mockCommand.onTaskCompleted(mockProcess)).thenReturn(true);
        setActiveTask(mockProcess, mockCommand);

        service.notifyTaskDone();
        assertThat(getTaskCompletedKill()).isTrue();
    }

    @Test
    void dispose_withAliveProcess_cancelsProcess() throws Exception {
        Process mockProcess = mock(Process.class);
        when(mockProcess.isAlive()).thenReturn(true);
        when(mockProcess.pid()).thenReturn(12345L);
        setActiveProcess(mockProcess);

        service.dispose();
        verify(mockProcess).destroyForcibly();
    }

    @Test
    void dispose_noProcess_doesNotThrow() {
        service.dispose();
    }

    // Tests for resolveCliType()

    @Test
    void resolveCliType_withExplicitNonCustomType_returnsIt() {
        CliToolConfig tool = CliToolConfig.builder()
                .type(CliToolConfig.CliType.CLAUDE)
                .name("anything")
                .build();
        assertThat(service.resolveCliType(tool)).isEqualTo(CliToolConfig.CliType.CLAUDE);
    }

    @Test
    void resolveCliType_withCustomTypeAndMatchingName_autoDetects() {
        CliToolConfig tool = CliToolConfig.builder()
                .type(CliToolConfig.CliType.CUSTOM)
                .name("Claude")
                .build();
        assertThat(service.resolveCliType(tool)).isEqualTo(CliToolConfig.CliType.CLAUDE);
    }

    @Test
    void resolveCliType_withCustomTypeAndMatchingNameCaseInsensitive_autoDetects() {
        CliToolConfig tool = CliToolConfig.builder()
                .type(CliToolConfig.CliType.CUSTOM)
                .name("CODEX")
                .build();
        assertThat(service.resolveCliType(tool)).isEqualTo(CliToolConfig.CliType.CODEX);
    }

    @Test
    void resolveCliType_withCustomTypeAndUnknownName_remainsCustom() {
        CliToolConfig tool = CliToolConfig.builder()
                .type(CliToolConfig.CliType.CUSTOM)
                .name("MyCustomTool")
                .build();
        assertThat(service.resolveCliType(tool)).isEqualTo(CliToolConfig.CliType.CUSTOM);
    }

    @Test
    void resolveCliType_withNullType_defaultsToCustomAndMatchesByName() {
        CliToolConfig tool = CliToolConfig.builder()
                .name("Gemini")
                .build();
        // Builder default is CUSTOM, so name-based detection applies
        tool.setType(null);
        assertThat(service.resolveCliType(tool)).isEqualTo(CliToolConfig.CliType.GEMINI);
    }

    @Test
    void generateMcpConfig_producesValidJsonFile() throws Exception {
        Method method = CliTaskExecutorService.class.getDeclaredMethod("generateMcpConfig", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, "mcpServers");
        assertThat(result).isNotNull();
        assertThat(result).endsWith(".json");

        java.io.File file = new java.io.File(result);
        assertThat(file).exists();
        String content = java.nio.file.Files.readString(file.toPath());
        assertThat(content).contains("\"mcpServers\"");
        assertThat(content).contains("\"backlog\"");
        assertThat(content).contains("\"command\": \"backlog\"");

        file.delete();
    }

    @Test
    void generateMcpConfig_differentKey_usesProvidedKey() throws Exception {
        Method method = CliTaskExecutorService.class.getDeclaredMethod("generateMcpConfig", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(service, "servers");
        assertThat(result).isNotNull();

        java.io.File file = new java.io.File(result);
        assertThat(file).exists();
        String content = java.nio.file.Files.readString(file.toPath());
        assertThat(content).contains("\"servers\"");
        assertThat(content).contains("\"backlog\"");

        file.delete();
    }

    // Tests for processStreamLine behaviour (extracted from createStreamReader refactor)

    @Test
    void processStreamLine_withLineCollector_addsLine() throws Exception {
        List<String> collector = new ArrayList<>();
        invokeProcessStreamLine("hello", 1, true, collector, false);
        assertThat(collector).containsExactly("hello");
    }

    @Test
    void processStreamLine_withNullCollector_doesNotThrow() throws Exception {
        // Should complete without NPE and dispatch via invokeLater
        invokeProcessStreamLine("hello", 1, true, null, false);
        verify(application, atLeastOnce()).invokeLater(any(Runnable.class));
    }

    @Test
    void processStreamLine_stdout_dispatchesToPrintOutput() throws Exception {
        CliConsoleManager mockConsole = mock(CliConsoleManager.class);
        // Capture the Runnable passed to invokeLater and execute it synchronously
        doAnswer(inv -> { inv.getArgument(0, Runnable.class).run(); return null; })
                .when(application).invokeLater(any(Runnable.class));

        invokeProcessStreamLineWithConsole("output-line", 1, true, null, false, mockConsole);
        verify(mockConsole).printOutput("output-line");
        verify(mockConsole, never()).printError(any());
    }

    @Test
    void processStreamLine_stderr_dispatchesToPrintError() throws Exception {
        CliConsoleManager mockConsole = mock(CliConsoleManager.class);
        doAnswer(inv -> { inv.getArgument(0, Runnable.class).run(); return null; })
                .when(application).invokeLater(any(Runnable.class));

        invokeProcessStreamLineWithConsole("error-line", 1, false, null, false, mockConsole);
        verify(mockConsole).printError("error-line");
        verify(mockConsole, never()).printOutput(any());
    }

    /** Calls processStreamLine via reflection using the service's own CliConsoleManager mock. */
    private void invokeProcessStreamLine(String line, int lineCount, boolean isStdout,
                                          List<String> collector, boolean parseJson) throws Exception {
        invokeProcessStreamLineWithConsole(line, lineCount, isStdout, collector, parseJson,
                mock(CliConsoleManager.class));
    }

    private void invokeProcessStreamLineWithConsole(String line, int lineCount, boolean isStdout,
                                                      List<String> collector, boolean parseJson,
                                                      CliConsoleManager console) throws Exception {
        Method method = CliTaskExecutorService.class.getDeclaredMethod(
                "processStreamLine",
                String.class, int.class, boolean.class, String.class, String.class,
                List.class, boolean.class, CliConsoleManager.class);
        method.setAccessible(true);
        method.invoke(service, line, lineCount, isStdout, isStdout ? "stdout" : "stderr",
                TEST_TASK_ID, collector, parseJson, console);
    }

    // Helper methods to manipulate private state via reflection

    /**
     * Injects an ActiveCliTask (process + command) into the activeTasks map under TEST_TASK_ID.
     * Uses reflection to access the private static inner class ActiveCliTask.
     */
    private void setActiveTask(Process process, CliCommand command) throws Exception {
        Class<?> activeCliTaskClass = Arrays.stream(CliTaskExecutorService.class.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals("ActiveCliTask"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("ActiveCliTask inner class not found"));

        var constructor = activeCliTaskClass.getDeclaredConstructor(Process.class, CliCommand.class);
        constructor.setAccessible(true);
        Object activeTask = constructor.newInstance(process, command);

        Field activeTasksField = CliTaskExecutorService.class.getDeclaredField("activeTasks");
        activeTasksField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Object> map =
                (ConcurrentHashMap<String, Object>) activeTasksField.get(service);
        map.put(TEST_TASK_ID, activeTask);
    }

    /**
     * Injects a process-only active task (with a stub command) under TEST_TASK_ID.
     */
    private void setActiveProcess(Process process) throws Exception {
        setActiveTask(process, mock(CliCommand.class));
    }

    /**
     * Reads taskCompletedKill from the ActiveCliTask stored under TEST_TASK_ID.
     */
    private boolean getTaskCompletedKill() throws Exception {
        Field activeTasksField = CliTaskExecutorService.class.getDeclaredField("activeTasks");
        activeTasksField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, ?> map =
                (ConcurrentHashMap<String, ?>) activeTasksField.get(service);
        Object task = map.get(TEST_TASK_ID);
        if (task == null) return false;
        Field field = task.getClass().getDeclaredField("taskCompletedKill");
        field.setAccessible(true);
        return (boolean) field.get(task);
    }
}
